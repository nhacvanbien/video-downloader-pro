package com.smarttool.videodownloader.feature.downloads.presentation

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat.setAudioMuted
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.browser.applyDetectionDefaults
import com.smarttool.videodownloader.core.di.ScopedViewModelStore
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosContract
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoSniffer
import com.smarttool.videodownloader.feature.browser.presentation.WebTabPipelineContract
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * The Processing tab: paste a link, probe it with an off-screen WebView, then list and
 * control the running downloads.
 *
 * This used to be `ProcessingFragment`. The detection WebView has to outlive tab
 * switches — the tab bodies are now composables that get disposed when you switch away
 * — so it lives here in an Activity-owned object instead of in the composition.
 *
 * The WebView is configured, sniffed and presented exactly as the browser's is; the
 * shared halves are [applyDetectionDefaults], [VideoSniffer] and
 * [DetectedVideosPresenter]. What is left here is what makes this tab itself: the probe
 * is invisible and muted, and its URL comes from the clipboard rather than from browsing.
 */
class ProcessingController(
    private val activity: AppCompatActivity,
    private val permissionSheet: StoragePermissionSheet,
    private val permissionChecker: MediaPermissionChecker,
) : KoinComponent {

    /**
     * Rebuilt on every [start]. Clearing a store cancels its ViewModels'
     * `viewModelScope` for good, and the detection pipeline runs on that scope, so a
     * restart has to hand out fresh instances rather than cleared ones.
     */
    private var viewModels = ScopedViewModelStore()

    lateinit var processingViewModel: ProcessingViewModel
        private set

    private lateinit var tabViewModel: WebTabViewModel
    private lateinit var detector: DetectedVideosTabViewModel
    private lateinit var sniffer: VideoSniffer

    /**
     * Owns the detected-videos sheet's state; the route renders it. Null outside a
     * session, and Compose state rather than a plain field so appearing recomposes the
     * route.
     */
    var detected by mutableStateOf<DetectedVideosPresenter?>(null)
        private set

    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val proxyController: CustomProxyController by inject()
    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()

    /** Set by the route composable so the controller can drive navigation. */
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    var uiState by mutableStateOf(ProcessingUiState())
        private set

    private var webView: WebView? = null

    private var currentUrl = ""

    private var started = false

    /** The off-screen probe WebView the screen keeps attached at 1dp. */
    val detectionWebView: WebView get() = requireNotNull(webView)

    // ------------------------------------------------------------------ lifecycle

    fun start() {
        if (started) return
        started = true

        viewModels = ScopedViewModelStore()
        processingViewModel = viewModels.get()
        tabViewModel = viewModels.get()
        detector = viewModels.get()

        sniffer = VideoSniffer(detector, tabViewModel, okHttpProxyClient)
        detected = DetectedVideosPresenter(
            activity = activity,
            detector = detector,
            processingViewModel = processingViewModel,
            mapper = detectedVideoUiMapper,
            sanitizeFileName = sanitizeFileName,
            announceDownloadStart = true,
            onPreviewMedia = { url, title, headers -> onPreviewMedia(url, title, headers) },
        )

        webView = WebView(activity).also(::configureWebView)

        detector.attach(tabViewModel)

        observeDetectionState()
        observeLoadPageEvent()

        tabViewModel.onEvent(WebTabPipelineContract.Event.LoadPage(currentUrl))
    }

    fun release() {
        if (!started) return
        started = false

        // Probes are stopped before the WebView goes away; the rest of the detector's
        // teardown happens in its `onCleared`, when `viewModels.clear()` runs below.
        detector.onEvent(DetectedVideosContract.Event.CancelAllChecks)
        sniffer.cancelPendingProbes()
        detected = null

        webView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.destroy()
        }
        webView = null

        viewModels.clear()
    }

    fun onActivityPause() {
        webView?.onPause()
    }

    fun onActivityResume() {
        webView?.onResume()
    }

    // ------------------------------------------------------------------ observers

    private fun observeDetectionState() {
        activity.lifecycleScope.launch {
            detector.uiState.collect { state ->
                uiState = uiState.copy(
                    downloadButtonState = when (state.downloadButtonState) {
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

                    DetectedVideosContract.Effect.VideoPushed -> toast(R.string.string_video_found)

                    // Never surfaced today — carried over unchanged from the pre-Contract
                    // `loginRequiredEvent`, which also had no observer anywhere.
                    is DetectedVideosContract.Effect.LoginRequired -> Unit
                }
            }
        }
    }

    /**
     * The event only carries the URL to load. It must not replace the WebView the
     * screen is showing — `WebTabViewModel.loadPage` builds a tab with no WebView at
     * all, and this controller owns the only one there is.
     */
    private fun observeLoadPageEvent() {
        activity.lifecycleScope.launch {
            tabViewModel.effect.collect { effect ->
                if (effect is WebTabPipelineContract.Effect.LoadPage &&
                    effect.tab.getUrl().startsWith("http")
                ) {
                    webView?.stopLoading()
                    webView?.loadUrl(effect.tab.getUrl())
                }
            }
        }
    }

    // ------------------------------------------------------------------ screen actions

    fun onUrlChange(url: String) {
        uiState = uiState.copy(url = url)
        currentUrl = url

        // Anything that is not an http(s) link cannot be probed, so reset the button.
        if (url.isBlank() || !url.startsWith("http")) {
            detector.onEvent(DetectedVideosContract.Event.MarkCanNotDownload)
            return
        }

        tabViewModel.onEvent(WebTabPipelineContract.Event.LoadPage(url))
    }

    fun pasteFromClipboard() {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val hasText = clipboard.hasPrimaryClip() &&
            clipboard.primaryClipDescription
                ?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

        if (!hasText) {
            toast(R.string.string_no_text_in_clipboard)
            return
        }

        val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()

        if (copied.isEmpty()) {
            toast(R.string.string_clipboard_is_empty)
            return
        }

        onUrlChange(copied)
    }

    fun requestDetectedVideos() {
        detector.onEvent(DetectedVideosContract.Event.ShowVideoInfo)
    }

    fun onPauseResume(info: ProgressInfo) {
        if (info.downloadStatus == VideoTaskState.PAUSE) {
            processingViewModel.onEvent(ProcessingContract.Event.Resume(info))
        } else {
            processingViewModel.onEvent(ProcessingContract.Event.Pause(info))
        }
    }

    fun cancel(info: ProgressInfo) {
        processingViewModel.onEvent(ProcessingContract.Event.Cancel(info, removeFile = true))
    }

    // ------------------------------------------------------------------ web view

    private fun configureWebView(webView: WebView) {
        webView.webChromeClient = webChromeClient
        webView.webViewClient = webViewClient
        webView.applyDetectionDefaults(allowAutoplay = false)
        setAudioMuted(webView, true)

        Timber.d("Processing detection WebView ready")
    }

    private val webViewClient = object : WebViewClient() {

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val title = tabViewModel.uiState.value.currentTitle
            val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

            if (url != null) {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    detector.onEvent(DetectedVideosContract.Event.StartPage(url, userAgent))
                    tabViewModel.onEvent(
                        WebTabPipelineContract.Event.UpdateVisitedHistory(url, title, userAgent),
                    )
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
            tabViewModel.onEvent(WebTabPipelineContract.Event.StartPage(url, view.title))
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val url = request.url.toString()
            val isAd = tabViewModel.isAd(url)

            return if (url.startsWith("http") && request.isForMainFrame && !isAd) {
                if (!tabViewModel.uiState.value.isTabInputFocused) {
                    tabViewModel.onEvent(WebTabPipelineContract.Event.SetTabTextInput(url))
                }
                false
            } else {
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            tabViewModel.onEvent(WebTabPipelineContract.Event.FinishPage(url))
        }

        override fun onRenderProcessGone(
            view: WebView?,
            detail: RenderProcessGoneDetail?,
        ): Boolean {
            val crashedOurs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                view == webView && detail?.didCrash() == true
            } else {
                view == webView
            }

            if (crashedOurs) {
                view?.destroy()
                return true
            }

            return super.onRenderProcessGone(view, detail)
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
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
