package com.smarttool.videodownloader.feature.downloads.presentation

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
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosContract
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoSniffer
import com.smarttool.videodownloader.feature.browser.presentation.WebTabPipelineContract
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Owns the Processing tab's off-screen probe [WebView] and nothing else: every callback
 * below reads state from [tabViewModel]/[detector] or dispatches an event to them — none
 * of it decides anything itself. [ProcessingViewModel], [WebTabViewModel] and
 * [DetectedVideosTabViewModel] hold all the state and business rules; this class exists
 * purely because the probe (and the detected-videos sheet built from it) have to outlive
 * tab switches — the Processing tab body is a composable that gets disposed when you
 * switch away — so they cannot live in the composition, and cannot live in a ViewModel
 * either (a `View` outliving the screen that created it is exactly what ViewModels are
 * meant to avoid).
 */
class ProcessingWebViewHost(
    private val activity: AppCompatActivity,
    val permissionSheet: StoragePermissionSheet,
    val permissionChecker: MediaPermissionChecker,
) : KoinComponent {

    /**
     * Rebuilt on every [start]. Clearing a store cancels its ViewModels'
     * `viewModelScope` for good, and the detection pipeline runs on that scope, so a
     * restart has to hand out fresh instances rather than cleared ones.
     */
    private var viewModels = ScopedViewModelStore()

    lateinit var processingViewModel: ProcessingViewModel
        private set

    lateinit var tabViewModel: WebTabViewModel
        private set

    lateinit var detector: DetectedVideosTabViewModel
        private set

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

    /** Set by the route composable so the sheet can drive navigation. */
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    private var webView: WebView? = null
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
            announceDownloadStart = true,
            onPreviewMedia = { url, title, headers -> onPreviewMedia(url, title, headers) },
        )

        webView = WebView(activity).also(::configureWebView)

        detector.attach(tabViewModel)

        observeEffects()
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

    // ------------------------------------------------------------------ view-model wiring

    private fun observeEffects() {
        // ProcessingViewModel decided whether the pasted text is probeable; forward that
        // decision to whichever pipeline ViewModel acts on it.
        activity.lifecycleScope.launch {
            processingViewModel.effect.collect { effect ->
                when (effect) {
                    is ProcessingContract.Effect.LoadUrl ->
                        tabViewModel.onEvent(WebTabPipelineContract.Event.LoadPage(effect.url))

                    ProcessingContract.Effect.ResetDetection ->
                        detector.onEvent(DetectedVideosContract.Event.MarkCanNotDownload)
                }
            }
        }

        // The effect only carries the URL to load. It must not replace the WebView the
        // screen is showing — `WebTabViewModel`'s `LoadPage` builds a tab with no WebView
        // at all, and this host owns the only one there is.
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
            val crashedOurs =
                view == webView && detail?.didCrash() == true

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
