package com.smarttool.videodownloader.feature.browser.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
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
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.android.databinding.LayoutBannerContainerBinding
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.browser.applyDetectionDefaults
import com.smarttool.videodownloader.core.di.ScopedViewModelStore
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.core.ui.dialogs.DialogInformationImage
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.feature.browser.domain.FaviconUtils
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosPresenter
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.usecase.SaveHistoryEntryUseCase
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.GetSelectedTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.OpenTabUseCase
import com.vimalcvs.materialrating.DialogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * The browser tab: WebView, ad-blocking and video-detection wiring, download sheet.
 *
 * This used to be `WebTabActivity`. Under a single-Activity graph the destination's
 * composable is thrown away whenever something is pushed on top of it (a video
 * preview, for instance), so the WebView cannot live in the composition — it lives
 * here, in an object the Activity owns for as long as the browser is on the back
 * stack. [release] is called explicitly when the user leaves the browser rather than
 * on composable disposal, which is what keeps page state across a preview round trip.
 *
 * The WebView is configured, sniffed and presented exactly as the Processing tab's probe
 * is; the shared halves are [applyDetectionDefaults], [VideoSniffer] and
 * [DetectedVideosPresenter]. What is left here is what makes this a browser: chrome,
 * history, tabs, fullscreen video, image downloads and the ad banner.
 */
class WebTabController(
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

    private lateinit var tabViewModel: WebTabViewModel
    private lateinit var settingsViewModel: BrowserSettingsViewModel
    private lateinit var detector: DetectedVideosTabViewModel
    private lateinit var processingViewModel: ProcessingViewModel
    private lateinit var sniffer: VideoSniffer

    /**
     * Owns the detected-videos sheet's state; the route renders it. Null outside a
     * session — the route composes before [start] runs and again after [release] — and
     * Compose state rather than a plain field so appearing recomposes the route.
     */
    var detected by mutableStateOf<DetectedVideosPresenter?>(null)
        private set

    private val saveHistoryEntry: SaveHistoryEntryUseCase by inject()
    private val appUtil: SystemUiController by inject()
    private val proxyController: CustomProxyController by inject()
    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()
    private val videoTaskItemRepository: VideoTaskItemRepository by inject()
    private val observeTabs: ObserveTabsUseCase by inject()
    private val createTab: CreateTabUseCase by inject()
    private val getSelectedTab: GetSelectedTabUseCase by inject()
    private val openTab: OpenTabUseCase by inject()

    /** Set by the route composable so the controller can drive navigation. */
    var onOpenTabs: () -> Unit = {}
    var onCloseBrowser: () -> Unit = {}
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    var uiState by mutableStateOf(WebTabUiState())
        private set

    /** Hosts the WebView so the existing show/hide logic keeps working under Compose. */
    val webViewContainer: FrameLayout by lazy { FrameLayout(activity) }

    /** Receives the page's fullscreen video via `WebChromeClient.onShowCustomView`. */
    val fullscreenContainer: FrameLayout by lazy { FrameLayout(activity) }

    private val bannerBinding by lazy {
        LayoutBannerContainerBinding.inflate(activity.layoutInflater)
    }

    val bannerView: View get() = bannerBinding.root

    private lateinit var webTab: WebTab

    private var historyItemCurrent: HistoryItem? = null

    private var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""

    private var isReload = false
    private var canGoCounter = 0
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
            sanitizeFileName = sanitizeFileName,
            announceDownloadStart = false,
            onPreviewMedia = { mediaUrl, title, headers ->
                onPreviewMedia(mediaUrl, title, headers)
            },
        )

        webTab = WebTabFactory.createWebTabFromInput(url)
        uiState = WebTabUiState(url = webTab.getUrl())

        loadAd()
        registerServiceWorkerClient()

        detector.attach(tabViewModel)

        ensureSelectedTabModel()
        observeDownloadOutcomes()
        observeDetectionState()
        observeTabState()

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

    // ------------------------------------------------------------------ observers

    private fun ensureSelectedTabModel() {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (getSelectedTab() == null) {
                    createTab(TabModel(url = webTab.getUrl(), isSelected = true))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to insert initial tab model")
            }
        }
    }

    private fun observeDetectionState() {
        activity.lifecycleScope.launch {
            detector.uiState.collect { state ->
                uiState = uiState.copy(
                    downloadButtonState =
                        when (state.downloadButtonState) {
                            is DownloadButtonStateCanDownload -> DownloadButtonUiState.Enabled
                            is DownloadButtonStateLoading -> DownloadButtonUiState.Loading
                            else -> DownloadButtonUiState.Disabled
                        },
                )
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

                    // Never surfaced today — carried over unchanged from the pre-Contract
                    // `loginRequiredEvent`, which also had no observer anywhere.
                    is DetectedVideosContract.Effect.LoginRequired -> Unit
                }
            }
        }
    }

    private fun observeTabState() {
        activity.lifecycleScope.launch {
            observeTabs().collect { uiState = uiState.copy(tabCount = it.size) }
        }

        activity.lifecycleScope.launch {
            // The effect only carries the URL to load. It must not replace [webTab]:
            // `WebTabViewModel.loadPage` builds a tab with no WebView at all, and this
            // controller owns the only one there is.
            tabViewModel.effect.collect { effect ->
                if (effect is WebTabPipelineContract.Effect.LoadPage &&
                    effect.tab.getUrl().startsWith("http")
                ) {
                    webTab.setUrl(effect.tab.getUrl())
                    webTab.getWebView()?.stopLoading()
                    webTab.getWebView()?.loadUrl(effect.tab.getUrl())
                }
            }
        }

        activity.lifecycleScope.launch {
            tabViewModel.uiState.collect { state ->
                uiState = uiState.copy(
                    url = state.tabUrl,
                    progress = state.progress,
                    showProgress = state.progress != 100,
                    isLoadingPage = state.isShowProgress,
                )
                isReload = !state.isShowProgress
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

    fun submitUrl() {
        val url = uiState.url
        if (url.isEmpty()) return

        detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
        tabViewModel.onEvent(WebTabPipelineContract.Event.OpenPage(url))
    }

    fun onUrlChange(url: String) {
        uiState = uiState.copy(url = url)
    }

    fun closeTab() {
        detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
        onCloseBrowser()
    }

    fun openTabs() {
        onOpenTabs()
    }

    fun requestDetectedVideos() {
        detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo)
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, webTab.getTitle())
            putExtra(Intent.EXTRA_TEXT, webTab.getUrl())
        }
        activity.startActivity(
            Intent.createChooser(
                intent,
                activity.getString(R.string.string_share)
            )
        )
    }

    /** Mirrors the WebView's history state into the Compose chrome. */
    private fun refreshNavState() {
        val webView = webTab.getWebView() ?: return
        uiState = uiState.copy(
            canGoBack = webView.canGoBack(),
            canGoForward = webView.canGoForward(),
        )
    }

    fun navigateBack() {
        val webView = webTab.getWebView() ?: return

        if (webView.canGoBack()) {
            webView.goBack()
            tabViewModel.onEvent(WebTabPipelineContract.Event.GoBack(webView))
            detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
            refreshNavState()
        } else {
            canGoCounter = if (canGoCounter >= 1) 0 else canGoCounter + 1
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
     * icon, matching the View implementation's [isReload] flag.
     */
    fun reloadPage() {
        if (!isReload) {
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

    fun saveUrlToHistoryBookmark() {
        val item = historyItemCurrent ?: return

        item.isBookmark = true
        activity.lifecycleScope.launch(Dispatchers.IO) {
            saveHistoryEntry(item.toEntry())

            withContext(Dispatchers.Main) {
                toast(R.string.string_save_to_bookmarks_successfully)
            }
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
            val viewTitle = view?.title
            val title = tabViewModel.uiState.value.currentTitle
            val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

            if (url != null && lastSavedHistoryUrl != url) {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val icon = try {
                            FaviconUtils.getEncodedFaviconFromUrl(
                                okHttpProxyClient.getProxyOkHttpClient(),
                                url,
                            )
                        } catch (e: Throwable) {
                            null
                        }

                        val outputFavicon = FaviconUtils.bitmapToBytes(icon)

                        saveUrlToHistory(url, icon, viewTitle ?: title)

                        detector.onEvent(DetectedVideosContract.Event.StartPage(url, userAgent))
                        tabViewModel.onEvent(
                            WebTabPipelineContract.Event.UpdateVisitedHistory(url, title, userAgent),
                        )

                        val tabModel = getSelectedTab()
                        if (tabModel == null) {
                            createTab(TabModel(url = url, isSelected = true, favicon = outputFavicon))
                        } else {
                            tabModel.url = url
                            tabModel.favicon = outputFavicon
                            openTab(tabModel)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to update tab favicon")
                    }
                }
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

            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)

            activity.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val selected = getSelectedTab()
                    if (selected == null) {
                        createTab(TabModel(url = url, isSelected = true, favicon = outputFavicon))
                    } else {
                        selected.url = url
                        selected.favicon = outputFavicon
                        openTab(selected)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to update selected tab favicon")
                }
            }

            tabViewModel.onEvent(WebTabPipelineContract.Event.StartPage(url, view.title))
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
            uiState = uiState.copy(isFullscreen = true)
        }

        override fun onHideCustomView() {
            super.onHideCustomView()

            fullscreenContainer.removeAllViews()
            webViewContainer.visibility = View.VISIBLE
            fullscreenContainer.visibility = View.GONE
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            uiState = uiState.copy(isFullscreen = false)
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
                        downloadImage(imageUrl)
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

    // ------------------------------------------------------------------ image download

    private suspend fun downloadImage(imageUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            if (imageUrl.startsWith("data:image")) {
                return@withContext saveBase64Image(imageUrl)
            }

            val client = OkHttpClient()
            val response = client.newCall(Request.Builder().url(imageUrl).build()).execute()

            if (!response.isSuccessful) {
                Timber.e("Image download failed: ${response.code}")
                return@withContext null
            }

            val fileName = generateFileName(URL(imageUrl).toString())
            val outputFile: File
            val fileSize: Long

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = insertPendingImage(fileName) ?: return@withContext null
                val descriptor = activity.contentResolver.openFileDescriptor(uri, "w")
                    ?: return@withContext null

                fileSize = response.body?.contentLength() ?: 0

                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    response.body?.byteStream()?.copyTo(output)
                }
                descriptor.close()

                outputFile = File(realPathFromUri(uri) ?: return@withContext null)
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                outputFile = File(downloadsDir, fileName)

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(outputFile).use { output -> input.copyTo(output) }
                }

                fileSize = outputFile.length()
            }

            saveDownloadedImage(outputFile, fileName, fileSize, imageUrl)
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Image download failed: $imageUrl")
            null
        }
    }

    private suspend fun saveBase64Image(base64String: String): File? = withContext(Dispatchers.IO) {
        try {
            val base64Data = base64String.substringAfter("base64,", "")
            if (base64Data.isBlank()) {
                Timber.e("Invalid base64 data")
                return@withContext null
            }

            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            val fileName = "downloaded_base64_image_${System.currentTimeMillis()}.jpg"

            val outputFile: File
            val fileSize: Long

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = insertPendingImage(fileName) ?: return@withContext null
                val descriptor = activity.contentResolver.openFileDescriptor(uri, "w")
                    ?: return@withContext null

                FileOutputStream(descriptor.fileDescriptor).use { it.write(decodedBytes) }
                descriptor.close()

                outputFile = File(realPathFromUri(uri) ?: return@withContext null)
                fileSize = decodedBytes.size.toLong()
            } else {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                outputFile = File(picturesDir, fileName)

                FileOutputStream(outputFile).use { it.write(decodedBytes) }
                fileSize = outputFile.length()
            }

            saveDownloadedImage(outputFile, fileName, fileSize, base64String)
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Saving base64 image failed")
            null
        }
    }

    private suspend fun saveDownloadedImage(
        outputFile: File,
        fileName: String,
        fileSize: Long,
        sourceUrl: String,
    ) {
        videoTaskItemRepository.insertVideoTaskItem(
            VideoTaskItem(
                mId = outputFile.absolutePath.hashCode().toString(),
                fileName = fileName,
                filePath = outputFile.absolutePath,
                fileSize = fileSize,
                fileDate = System.currentTimeMillis(),
                url = sourceUrl,
                mimeType = "image",
            ),
        )

        withContext(Dispatchers.Main) {
            Toast.makeText(
                activity,
                "File saved: ${outputFile.absolutePath}",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun insertPendingImage(fileName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        return activity.contentResolver
            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun realPathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        activity.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) return cursor.getString(columnIndex)
        }

        return null
    }

    private fun generateFileName(url: String): String {
        val extension = url.substringAfterLast(".", "jpg")
        return "downloaded_image_${System.currentTimeMillis()}.$extension"
    }

    // ------------------------------------------------------------------ history

    private suspend fun saveUrlToHistory(url: String, favicon: Bitmap?, title: String?) {
        val isTitleEmpty = title?.trim()?.isEmpty() == true

        val shouldSave = !isTitleEmpty &&
                lastSavedTitleHistory != title &&
                lastSavedHistoryUrl != url &&
                url.isNotEmpty() &&
                !url.contains("about:blank")

        if (!shouldSave) return

        lastSavedHistoryUrl = url
        lastSavedTitleHistory = title.orEmpty()

        yield()

        val item = HistoryItem(
            url = url,
            favicon = FaviconUtils.bitmapToBytes(favicon),
            title = title,
        )

        historyItemCurrent = item
        saveHistoryEntry(item.toEntry())
    }

    private fun HistoryItem.toEntry() = HistoryEntry(
        id = id,
        title = title.orEmpty(),
        url = url,
        datetime = datetime,
        isBookmark = isBookmark,
        favicon = favicon,
    )

    // ------------------------------------------------------------------ ads

    private val bannerAdHelper by lazy {
        BannerAdHelper(
            activity = activity,
            lifecycleOwner = activity,
            config = BannerAdConfig(
                idAds = BuildConfig.BANNER_ALL,
                canShowAds = AdsConstant.showBannerAll,
                canReloadAds = true,
                adPlacement = "banner_web_tab",
            ),
        )
    }

    private fun loadAd() {
        if (!AdsConstant.showBannerAll) return

        bannerAdHelper.setBannerContentView(bannerBinding.frAdsBanner)
        bannerAdHelper.requestAds(BannerAdParam.Request)
        bannerAdHelper.registerAdListener(object : BannerAdCallBack {
            override fun onAdImpression() = Unit
            override fun onAdClicked() = Unit
            override fun onAdFailedToLoad(loadAdError: LoadAdError) = Unit
            override fun onAdFailedToShow(adError: AdError) = Unit
            override fun onAdLoaded(data: ContentAd) = Unit
        })
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
