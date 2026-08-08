package com.smarttool.videodownloader.feature.browser.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.view.View
import android.view.WindowManager
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.browser.applyDetectionDefaults
import com.smarttool.videodownloader.core.di.ScopedViewModelStore
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.core.ui.dialogs.DialogInformationImage
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosPresenter
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.vimalcvs.materialrating.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Owns the browser tab's raw [WebView] (and the fullscreen container View around it)
 * and nothing else: every callback below reads state from [tabViewModel]/
 * [detector]/[settingsViewModel] or dispatches an event to them — none of it decides
 * anything itself. [WebTabViewModel], [DetectedVideosTabViewModel] and
 * [BrowserSettingsViewModel] hold all the state and business rules; this class exists
 * purely because the WebView has to outlive the browser destination's composable being
 * disposed (a video preview pushed on top throws it away) — the WebView cannot live in
 * the composition and cannot live in a ViewModel either (a `View` outliving the screen
 * that created it is exactly what ViewModels are meant to avoid).
 */
class WebTabViewHost(
    private val activity: AppCompatActivity,
    private val permissionSheet: StoragePermissionSheet,
    private val permissionChecker: MediaPermissionChecker,
) : DownloadImageHandler, KoinComponent {

    /**
     * Rebuilt for every browser session. Clearing a store cancels its ViewModels'
     * `viewModelScope` for good, and the detection pipeline runs on that scope — reusing
     * the same instances after a close would leave a browser that silently detects
     * nothing. Each session gets fresh ones, exactly as a new Activity used to.
     */
    private var viewModels = ScopedViewModelStore()

    lateinit var tabViewModel: WebTabViewModel
        private set

    lateinit var settingsViewModel: BrowserSettingsViewModel
        private set

    lateinit var detector: DetectedVideosTabViewModel
        private set

    private lateinit var processingViewModel: ProcessingViewModel
    private lateinit var sniffer: VideoSniffer

    /**
     * Owns the detected-videos sheet's state; the route renders it. Null outside a
     * session — the route composes before [start] runs and again after [release] — and
     * Compose state rather than a plain field so appearing recomposes the route.
     */
    var detected by mutableStateOf<DetectedVideosPresenter?>(null)
        private set

    private val appUtil: SystemUiController by inject()
    private val proxyController: CustomProxyController by inject()
    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()

    /** Set by the route composable so the host can drive navigation. */
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    /** Hosts the WebView so the existing show/hide logic keeps working under Compose. */
    val webViewContainer: FrameLayout by lazy { FrameLayout(activity) }

    /** Receives the page's fullscreen video via `WebChromeClient.onShowCustomView`. */
    val fullscreenContainer: FrameLayout by lazy { FrameLayout(activity) }

    private lateinit var webTab: WebTab

    private var videoAlert: MaterialAlertDialogBuilder? = null

    private var started = false

    // ------------------------------------------------------------------ lifecycle

    /**
     * Opens [url] in the browser. Safe to call again for a second tab: the previous
     * WebView is torn down first so only one is ever alive.
     */
    fun start(url: String) {
        if (started) release()
        started = true

        viewModels = ScopedViewModelStore()
        tabViewModel = viewModels.get()
        settingsViewModel = viewModels.get()
        detector = viewModels.get()
        processingViewModel = viewModels.get()

        sniffer = VideoSniffer(detector, tabViewModel, okHttpProxyClient)
        detected = DetectedVideosPresenter(
            activity = activity,
            detector = detector,
            processingViewModel = processingViewModel,
            mapper = detectedVideoUiMapper,
            announceDownloadStart = false,
            onPreviewMedia = { mediaUrl, title, headers ->
                onPreviewMedia(mediaUrl, title, headers)
            },
        )

        webTab = WebTabFactory.createWebTabFromInput(url)

        registerServiceWorkerClient()

        detector.attach(tabViewModel)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            tabViewModel.ensureSelectedTab(webTab.getUrl())
        }
        observeDownloadOutcomes()
        observeEffects()

        recreateWebView()
        configureWebView()
        activity.registerForContextMenu(webTab.getWebView())

        tabViewModel.onEvent(WebTabPipelineContract.Event.LoadPage(webTab.getUrl()))
    }

    fun release() {
        if (!started) return
        started = false

        // Probes are stopped before the WebView goes away; the rest of the detector's
        // teardown happens in its `onCleared`, when `viewModels.clear()` runs below.
        detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
        sniffer.cancelPendingProbes()

        webTab.getWebView()?.let { webView ->
            activity.unregisterForContextMenu(webView)
            webViewContainer.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        webTab.setWebView(null)

        webViewContainer.removeAllViews()
        fullscreenContainer.removeAllViews()
        detected = null
        viewModels.clear()
    }

    fun onActivityPause() {
        if (started) webTab.getWebView()?.onPause()
    }

    fun onActivityResume() {
        if (started) webTab.getWebView()?.onResume()
    }

    // ------------------------------------------------------------------ view-model wiring

    private fun observeEffects() {
        activity.lifecycleScope.launch {
            tabViewModel.effect.collect { effect ->
                when (effect) {
                    // The effect only carries the URL to load. It must not replace
                    // [webTab]: `LoadPage` builds a tab with no WebView at all, and
                    // this host owns the only one there is.
                    is WebTabPipelineContract.Effect.LoadPage -> {
                        if (effect.tab.getUrl().startsWith("http")) {
                            webTab.setUrl(effect.tab.getUrl())
                            webTab.getWebView()?.stopLoading()
                            webTab.getWebView()?.loadUrl(effect.tab.getUrl())
                        }
                    }

                    is WebTabPipelineContract.Effect.NotifyPageStarted ->
                        detector.onEvent(
                            DetectedVideosContract.Event.StartPage(effect.url, effect.userAgent),
                        )

                    WebTabPipelineContract.Effect.BookmarkSaved ->
                        toast(R.string.string_save_to_bookmarks_successfully)

                    is WebTabPipelineContract.Effect.ImageDownloaded ->
                        Toast.makeText(
                            activity,
                            "File saved: ${effect.path}",
                            Toast.LENGTH_SHORT,
                        ).show()

                    // Silent today too — `onClickDownloadImage` never checked the old
                    // `downloadImage()` return value either.
                    WebTabPipelineContract.Effect.ImageDownloadFailed -> Unit

                    // Never surfaced today — `openPageEvent`/`changeTabFocusEvent` had
                    // no observer before the Contract migration either.
                    is WebTabPipelineContract.Effect.ChangeTabFocus,
                    is WebTabPipelineContract.Effect.OpenPage,
                    -> Unit
                }
            }
        }

        activity.lifecycleScope.launch {
            detector.effect.collect { effect ->
                when (effect) {
                    DetectedVideosContract.Effect.ShowDetectedVideos -> {
                        if (detector.uiState.value.detectedVideos.isEmpty()) return@collect

                        if (permissionChecker.hasAll()) detected?.show() else permissionSheet.show()
                    }

                    DetectedVideosContract.Effect.VideoPushed -> onVideoPushed()

                    is DetectedVideosContract.Effect.LoginRequired ->
                        Toast.makeText(
                            activity,
                            activity.getString(
                                R.string.string_login_required_to_download,
                                effect.host,
                            ),
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }
    }

    /**
     * Surfaces one toast per finished download. The id sets guard against the polled
     * flow re-emitting the same terminal state on every tick.
     */
    private fun observeDownloadOutcomes() {
        val notifiedSuccess = mutableSetOf<String>()
        val notifiedError = mutableSetOf<String>()

        activity.lifecycleScope.launch {
            processingViewModel.downloads.collect { downloads ->
                for (info in downloads) {
                    when (info.downloadStatus) {
                        VideoTaskState.SUCCESS -> if (notifiedSuccess.add(info.id)) {
                            toast(R.string.string_download_successful)
                            DialogManager.showRatingAfterSuccessfulDownload(
                                activity,
                                AppConstant.FEEDBACK_EMAIL,
                            )
                        }

                        VideoTaskState.ERROR, VideoTaskState.ENOSPC ->
                            if (notifiedError.add(info.id)) toast(R.string.string_download_failed)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ chrome actions

    /** Mirrors the WebView's history state into the ViewModel. */
    private fun refreshNavState() {
        val webView = webTab.getWebView() ?: return
        tabViewModel.onEvent(
            WebTabPipelineContract.Event.RefreshNavState(webView.canGoBack(), webView.canGoForward()),
        )
    }

    fun navigateBack() {
        val webView = webTab.getWebView() ?: return

        if (webView.canGoBack()) {
            webView.goBack()
            tabViewModel.onEvent(WebTabPipelineContract.Event.GoBack(webView))
            detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
            refreshNavState()
        }
    }

    fun navigateForward() {
        val webView = webTab.getWebView() ?: return

        if (webView.canGoForward()) {
            webView.goForward()
            tabViewModel.onEvent(WebTabPipelineContract.Event.GoForward(webView))
            detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
            refreshNavState()
        }
    }

    /**
     * Doubles as stop-loading: while a page is in flight the control shows a close
     * icon, matching [WebTabPipelineContract.State.isShowProgress].
     */
    fun reloadPage() {
        if (tabViewModel.uiState.value.isShowProgress) {
            tabViewModel.onEvent(WebTabPipelineContract.Event.PageStop(webTab.getWebView()))
            return
        }

        var url = webTab.getWebView()?.url
        var urlWasChange = false

        if (url?.contains("m.facebook") == true) {
            url = url.replace("m.facebook", "www.facebook")
            urlWasChange = true
        }

        val userAgent = webTab.getWebView()?.settings?.userAgentString
            ?: tabViewModel.uiState.value.userAgent.ifEmpty { null }
            ?: BrowserUserAgent.MOBILE

        if (url == null) return

        detector.viewModelScope.launch(
            detector.executorReload,
        ) {
            detector.onEvent(DetectedVideosContract.Event.StartPage(url, userAgent))
        }

        if (url.contains("www.facebook") && urlWasChange) {
            tabViewModel.onEvent(WebTabPipelineContract.Event.OpenPage(url))
            tabViewModel.onEvent(WebTabPipelineContract.Event.CloseTab(webTab))
        } else {
            tabViewModel.onEvent(WebTabPipelineContract.Event.PageReload(webTab.getWebView()))
        }
    }

    // ------------------------------------------------------------------ detected videos

    private fun onVideoPushed() {
        toast(R.string.string_video_found)

        val isDownloadsVisible = true
        val isCond = !tabViewModel.uiState.value.isDownloadDialogShown && !isDownloadsVisible

        if (settingsViewModel.uiState.value.showVideoAlert && isCond) {
            showAlertVideoFound()
        }
    }

    private fun showAlertVideoFound() {
        if (tabViewModel.uiState.value.isDownloadDialogShown) return

        tabViewModel.onEvent(WebTabPipelineContract.Event.SetDownloadDialogShown(true))

        videoAlert = MaterialAlertDialogBuilder(activity).setTitle(R.string.string_video_found)

        videoAlert?.setOnDismissListener { videoAlert = null }
        videoAlert?.setMessage(R.string.whatshould)
            ?.setPositiveButton(R.string.view) { dialog, _ ->
                tabViewModel.onEvent(WebTabPipelineContract.Event.SetDownloadDialogShown(false))
                dialog.dismiss()
            }
            ?.setNeutralButton(R.string.dontshow) { dialog, _ ->
                settingsViewModel.onEvent(BrowserSettingsContract.Event.DismissVideoAlert)
                tabViewModel.onEvent(WebTabPipelineContract.Event.SetDownloadDialogShown(false))
                dialog.dismiss()
            }
            ?.setNegativeButton(R.string.string_cancel) { dialog, _ ->
                tabViewModel.onEvent(WebTabPipelineContract.Event.SetDownloadDialogShown(false))
                dialog.dismiss()
            }
            ?.show()
    }

    // ------------------------------------------------------------------ web view

    private fun recreateWebView() {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(activity))
        }
    }

    private fun configureWebView() {
        val webView = webTab.getWebView() ?: return

        webView.webChromeClient = webChromeClient
        webView.webViewClient = webViewClient
        webView.applyDetectionDefaults(allowAutoplay = true)

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        Timber.d("Web tab state: $webTab")

        webViewContainer.addView(webView, LinearLayout.LayoutParams(-1, -1))
    }

    private fun registerServiceWorkerClient() {
        val swController = ServiceWorkerController.getInstance()
        swController.setServiceWorkerClient(serviceWorkerClient)
        swController.serviceWorkerWebSettings.allowContentAccess = true
    }

    private val webViewClient = object : WebViewClient() {

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if (url != null) {
                val viewTitle = view?.title
                val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

                tabViewModel.onEvent(
                    WebTabPipelineContract.Event.PageVisited(url, viewTitle, userAgent),
                )
            }

            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView?,
            handler: HttpAuthHandler?,
            host: String?,
            realm: String?,
        ) {
            val creds = proxyController.getProxyCredentials()
            handler?.proceed(creds.first, creds.second)
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?,
        ): WebResourceResponse? =
            sniffer.onPageRequest(request) ?: super.shouldInterceptRequest(view, request)

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            videoAlert = null

            tabViewModel.onEvent(
                WebTabPipelineContract.Event.OnPageStarted(url, view.title, favicon),
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            val isAd = tabViewModel.isAd(url)

            return if (url.startsWith("http") && request.isForMainFrame && !isAd) {
                if (!tabViewModel.uiState.value.isTabInputFocused) {
                    tabViewModel.onEvent(WebTabPipelineContract.Event.SetTabTextInput(url))
                }
                // Trả về false: Tiếp tục tải trang ngay trong WebView hiện tại
                false
            } else {
                // Nếu là quảng cáo (isAd) hoặc chạy trong iframe (isForMainFrame == false):
                // Chúng ta KHÔNG gọi activity.startActivity() nữa để chặn mở app ngoài.

                // Trả về true: Nói với Android rằng ứng dụng đã xử lý liên kết này,
                // WebView sẽ hủy bỏ lệnh tải và không làm gì cả.
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            refreshNavState()
            super.onPageFinished(view, url)
            tabViewModel.onEvent(WebTabPipelineContract.Event.FinishPage(url))

            injectDownloadButton()
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
        ): Boolean {
            view?.destroy()
            return true
        }
    }

    private val webChromeClient = object : WebChromeClient() {

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?,
        ): Boolean {
            if (view == null || view.handler == null) return false

            val href = view.handler.obtainMessage()
            view.requestFocusNodeHref(href)
            val url = href.data.getString("url") ?: ""

            val isAd = tabViewModel.isAd(url)

            if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) return false

            val transport = resultMsg!!.obj as WebView.WebViewTransport
            transport.webView = WebView(view.context)

            tabViewModel.onEvent(
                WebTabPipelineContract.Event.NotifyPageOpened(
                    WebTab(
                        webview = transport.webView,
                        resultMsg = resultMsg,
                        url = "url",
                        title = view.title,
                        iconBytes = null,
                    ),
                ),
            )
            return true
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) = Unit

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if (!title.isNullOrBlank()) {
                tabViewModel.onEvent(WebTabPipelineContract.Event.TitleReceived(title))
            }
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)

            tabViewModel.onEvent(WebTabPipelineContract.Event.SetProgress(newProgress))
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            webViewContainer.visibility = View.GONE

            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            fullscreenContainer.addView(view)
            appUtil.hideSystemUI(activity.window, fullscreenContainer)
            fullscreenContainer.visibility = View.VISIBLE
            tabViewModel.onEvent(WebTabPipelineContract.Event.SetFullscreen(true))
        }

        override fun onHideCustomView() {
            super.onHideCustomView()

            fullscreenContainer.removeAllViews()
            webViewContainer.visibility = View.VISIBLE
            fullscreenContainer.visibility = View.GONE
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            tabViewModel.onEvent(WebTabPipelineContract.Event.SetFullscreen(false))
            activity.requestedOrientation = if (settingsViewModel.uiState.value.lockPortrait) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            appUtil.showSystemUI(activity.window, fullscreenContainer)
        }
    }

    /**
     * Streams delivered by a page's service worker never reach [webViewClient], so they
     * are sniffed here as well — through the same [sniffer], which is what makes them
     * land in the same detected-videos list.
     */
    private val serviceWorkerClient = object : ServiceWorkerClient() {
        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
            sniffer.onServiceWorkerRequest(request)
            return super.shouldInterceptRequest(request)
        }
    }

    /**
     * Overlays a download affordance on every reasonably sized image on the page and
     * keeps doing so as more load in, then routes taps back through [WebAppInterface].
     */
    private fun injectDownloadButton() {
        webTab.getWebView()?.evaluateJavascript(DOWNLOAD_BUTTON_SCRIPT, null)
    }

    // ------------------------------------------------------------------ context menu

    /** Delegated from the host Activity, which owns `onCreateContextMenu`. */
    fun onCreateContextMenu(view: View) {
        if (!started) return

        val webView = view as? WebView ?: return
        val result = webView.hitTestResult
        val imageUrl = result.extra ?: return

        val isImage = result.type == WebView.HitTestResult.IMAGE_TYPE ||
            result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE

        showImageDialog(imageUrl, isImage = isImage, showOpenInNewTab = true)
    }

    override fun onDownloadImageRequested(imageUrl: String) {
        showImageDialog(imageUrl, isImage = true, showOpenInNewTab = false)
    }

    private fun showImageDialog(imageUrl: String, isImage: Boolean, showOpenInNewTab: Boolean) {
        DialogInformationImage(
            activity,
            imageUrl,
            isImage,
            showOpenInNewTab,
            onClickOpenNewTab = {
                if (imageUrl.startsWith("http")) {
                    webTab.getWebView()?.stopLoading()
                    webTab.getWebView()?.loadUrl(imageUrl)
                }
            },
            onClickShare = { shareUrl(imageUrl) },
            onClickCopyLink = { copyToClipboard(imageUrl) },
            onClickDownloadImage = {
                activity.lifecycleScope.launch(Dispatchers.Main) {
                    if (permissionChecker.hasAll()) {
                        tabViewModel.onEvent(WebTabPipelineContract.Event.DownloadImage(imageUrl))
                    } else {
                        permissionSheet.show()
                    }
                }
            },
        ).show()
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        activity.startActivity(
            Intent.createChooser(intent, activity.getString(R.string.string_share)),
        )
    }

    private fun copyToClipboard(text: String) {
        val clipboard =
            activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("copied_text", text))
        toast(R.string.string_copied_to_clipboard)
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
