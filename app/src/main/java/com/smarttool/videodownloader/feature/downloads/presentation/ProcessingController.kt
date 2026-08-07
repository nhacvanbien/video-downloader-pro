package com.smarttool.videodownloader.feature.downloads.presentation

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewCompat.setAudioMuted
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.AppLogger
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.di.ScopedViewModelStore
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.browser.domain.AdBlockerHelper
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.feature.browser.domain.VideoUtils
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanNotDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoDetectionAlgVModel
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import com.smarttool.videodownloader.feature.downloads.domain.model.DownloadTabListener
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The Processing tab: paste a link, probe it with an off-screen WebView, then list and
 * control the running downloads.
 *
 * This used to be `ProcessingFragment`. The detection WebView has to outlive tab
 * switches — the tab bodies are now composables that get disposed when you switch away
 * — so it lives here in an Activity-owned object instead of in the composition.
 */
class ProcessingController(
    private val activity: AppCompatActivity,
    private val permissionSheet: StoragePermissionSheet,
    private val permissionChecker: MediaPermissionChecker,
) : DownloadTabListener, KoinComponent {

    /**
     * Rebuilt on every [start]. Clearing a store cancels its ViewModels'
     * `viewModelScope` for good, and the detection pipeline runs on that scope, so a
     * restart has to hand out fresh instances rather than cleared ones.
     */
    private var viewModels = ScopedViewModelStore()

    lateinit var processingViewModel: ProcessingViewModel
        private set

    private lateinit var tabViewModel: WebTabViewModel
    private lateinit var videoDetectionTabViewModel: DetectedVideosTabViewModel
    private lateinit var videoDetectionModel: VideoDetectionAlgVModel

    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val proxyController: CustomProxyController by inject()
    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()

    /** Set by the route composable so the controller can drive navigation. */
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    var uiState by mutableStateOf(ProcessingUiState())
        private set

    var detectedVideos by mutableStateOf<List<DetectedVideoUi>>(emptyList())
        private set

    var showDetectedSheet by mutableStateOf(false)

    private var webView: WebView? = null

    private var currentUrl = ""
    private var lastSavedHistoryUrl = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

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
        videoDetectionTabViewModel = viewModels.get()
        videoDetectionModel = viewModels.get()

        webView = WebView(activity).also(::configureWebView)

        videoDetectionTabViewModel.webTabModel = tabViewModel
        videoDetectionTabViewModel.start()
        videoDetectionModel.start()

        observeDetectionState()
        observeLoadPageEvent()

        tabViewModel.loadPage(currentUrl)
    }

    fun release() {
        if (!started) return
        started = false

        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()

        regularJobsStorage.values.flatten().forEach { it.dispose() }
        regularJobsStorage.clear()

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
        videoDetectionTabViewModel.downloadButtonState.addOnPropertyChangedCallback(
            object : OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    uiState = uiState.copy(
                        downloadButtonState =
                        when (videoDetectionTabViewModel.downloadButtonState.get()) {
                            is DownloadButtonStateCanDownload -> DownloadButtonUiState.Enabled
                            is DownloadButtonStateLoading -> DownloadButtonUiState.Loading
                            else -> DownloadButtonUiState.Disabled
                        },
                    )
                }
            },
        )

        videoDetectionTabViewModel.showDetectedVideosEvent.observe(activity) {
            val firstItem = videoDetectionTabViewModel.detectedVideosList.get()?.firstOrNull()
            if (firstItem == null) return@observe

            if (permissionChecker.hasAll()) openDetectedSheet() else permissionSheet.show()
        }

        videoDetectionTabViewModel.videoPushedEvent.observe(activity) {
            toast(R.string.string_video_found)
        }
    }

    /**
     * The event only carries the URL to load. It must not replace the WebView the
     * screen is showing — `WebTabViewModel.loadPage` builds a tab with no WebView at
     * all, and this controller owns the only one there is.
     */
    private fun observeLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(activity) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webView?.stopLoading()
                webView?.loadUrl(tab.getUrl())
            }
        }
    }

    // ------------------------------------------------------------------ screen actions

    fun onUrlChange(url: String) {
        uiState = uiState.copy(url = url)
        currentUrl = url

        // Anything that is not an http(s) link cannot be probed, so reset the button.
        if (url.isBlank() || !url.startsWith("http")) {
            videoDetectionTabViewModel.downloadButtonState.set(DownloadButtonStateCanNotDownload())
            return
        }

        tabViewModel.loadPage(url)
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
        videoDetectionTabViewModel.showVideoInfo()
    }

    fun onPauseResume(info: ProgressInfo) {
        if (info.downloadStatus == VideoTaskState.PAUSE) {
            processingViewModel.resume(info)
        } else {
            processingViewModel.pause(info)
        }
    }

    fun cancel(info: ProgressInfo) {
        processingViewModel.cancel(info, removeFile = true)
    }

    // ------------------------------------------------------------------ detected videos

    /** Rebuilds the sheet model from the detection ViewModel's current edits. */
    private fun refreshDetectedVideos() {
        val titles = videoDetectionTabViewModel.formatsTitles.get().orEmpty()
        val formats = videoDetectionTabViewModel.selectedFormats.get().orEmpty()

        detectedVideos = videoDetectionTabViewModel.detectedVideosList.get()
            .orEmpty()
            .reversed()
            .map { detectedVideoUiMapper(it, titles, formats) }
    }

    private fun openDetectedSheet() {
        refreshDetectedVideos()
        showDetectedSheet = true
    }

    private fun findVideoInfo(id: String): VideoInfo? =
        videoDetectionTabViewModel.detectedVideosList.get()?.firstOrNull { it.id == id }

    fun onSelectFormatById(id: String, format: String) {
        val info = findVideoInfo(id) ?: return
        onSelectFormat(info, format)
        refreshDetectedVideos()
    }

    fun renameDetectedVideo(video: DetectedVideoUi) {
        DialogRename(activity, video.title) { newName ->
            val titles = videoDetectionTabViewModel.formatsTitles.get()?.toMutableMap()
                ?: mutableMapOf()
            titles[video.id] = newName
            videoDetectionTabViewModel.formatsTitles.set(titles)
            refreshDetectedVideos()
        }.show()
    }

    fun previewDetectedVideo(video: DetectedVideoUi) {
        val info = findVideoInfo(video.id) ?: return
        val format = video.selectedFormat ?: return
        onPreviewVideo(info, format, false)
    }

    fun downloadDetectedVideo(video: DetectedVideoUi) {
        val info = findVideoInfo(video.id) ?: return
        val format = video.selectedFormat

        if (format == null) {
            toast(R.string.string_invalid_data)
            return
        }

        onDownloadVideo(info, format, sanitizeFileName(video.title))
        showDetectedSheet = false
        toast(R.string.string_downloading)
    }

    override fun onCancel() {
        showDetectedSheet = false
    }

    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        val title = videoDetectionTabViewModel.formatsTitles.get()?.get(videoInfo.id).orEmpty()
        val currFormat = videoInfo.formats.formats.filter {
            it.format?.contains(format) ?: false
        }

        val first = currFormat.firstOrNull() ?: return

        val headers = first.httpHeaders
            ?.let { JSONObject(it as Map<*, *>).toString() }
            ?: "{}"

        onPreviewMedia(first.url.orEmpty(), title, if (isForce) "{}" else headers)
    }

    override fun onDownloadVideo(videoInfo: VideoInfo, format: String, videoTitle: String) {
        val info = videoInfo.copy(
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(
                videoInfo.formats.formats.filter { it.format?.contains(format) ?: false },
            ),
        )

        processingViewModel.start(info)
        showDetectedSheet = false
        toast(R.string.download_started)
    }

    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
        val formats =
            videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        formats[videoInfo.id] = format
        videoDetectionTabViewModel.selectedFormats.set(formats)
    }

    // ------------------------------------------------------------------ web view

    private fun configureWebView(webView: WebView) {
        webView.webChromeClient = webChromeClient
        webView.webViewClient = webViewClient
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isScrollbarFadingEnabled = true
        setAudioMuted(webView, true)

        webView.settings.apply {
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            setGeolocationEnabled(false)
            allowContentAccess = true
            allowFileAccess = true
            offscreenPreRaster = false
            displayZoomControls = false
            builtInZoomControls = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            useWideViewPort = true
            domStorageEnabled = true
            javaScriptEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptCanOpenWindowsAutomatically = true
            userAgentString = BrowserUserAgent.MOBILE
            mediaPlaybackRequiresUserGesture = true // Chặn autoplay
        }

        AppLogger.d("Processing detection WebView ready")
    }

    private val webViewClient = object : WebViewClient() {

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val title = tabViewModel.currentTitle.get()
            val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

            if (url != null && lastSavedHistoryUrl != url) {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    videoDetectionTabViewModel.onStartPage(url, userAgent)
                    tabViewModel.onUpdateVisitedHistory(url, title, userAgent)
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
        ): WebResourceResponse? {
            val url = request?.url.toString()

            if (tabViewModel.isAd(url)) return AdBlockerHelper.createEmptyResource()

            val requestWithCookies = request?.let { resourceRequest ->
                try {
                    CookieUtils.webRequestToHttpWithCookies(resourceRequest)
                } catch (e: Throwable) {
                    null
                }
            }

            val contentType = VideoUtils.getContentTypeByUrl(
                url,
                requestWithCookies?.headers,
                okHttpProxyClient,
            )

            val isManifest = contentType == ContentType.M3U8 ||
                contentType == ContentType.MPD ||
                url.contains(".m3u8") ||
                url.contains(".mpd") ||
                (url.contains(".txt") && url.contains("hentaihaven"))

            if (isManifest) {
                if (requestWithCookies != null) {
                    videoDetectionTabViewModel.verifyLinkStatus(
                        requestWithCookies,
                        tabViewModel.currentTitle.get(),
                        true,
                    )
                }
            } else {
                trackRegularMp4(requestWithCookies)
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val url = request.url.toString()
            val isAd = tabViewModel.isAd(url)

            return if (url.startsWith("http") && request.isForMainFrame && !isAd) {
                if (!tabViewModel.isTabInputFocused.get()) {
                    tabViewModel.setTabTextInput(url)
                }
                false
            } else {
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            tabViewModel.finishPage(url)
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

    /**
     * Long-lived MP4 probes are keyed by the page that started them, so navigating
     * away disposes the previous page's checks instead of leaking them.
     */
    private fun trackRegularMp4(requestWithCookies: okhttp3.Request?) {
        val disposable = videoDetectionTabViewModel.checkRegularMp4(requestWithCookies)
        val pageUrl = tabViewModel.getTabTextInput().get() ?: ""

        if (pageUrl != lastRegularCheckUrl) {
            regularJobsStorage[lastRegularCheckUrl]?.forEach { it.dispose() }
            regularJobsStorage.remove(lastRegularCheckUrl)
            lastRegularCheckUrl = pageUrl
        }

        if (disposable != null) {
            regularJobsStorage[pageUrl] = (regularJobsStorage[pageUrl].orEmpty()) + disposable
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

            AppLogger.d(
                "ON_CREATE_WINDOW::************* $url ${view.url} isAd:: $isAd $isUserGesture",
            )

            if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) return false

            val transport = resultMsg!!.obj as WebView.WebViewTransport
            transport.webView = WebView(view.context)

            tabViewModel.openPageEvent.value = WebTab(
                webview = transport.webView,
                resultMsg = resultMsg,
                url = "url",
                title = view.title,
                iconBytes = null,
            )
            return true
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) = Unit

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)

            tabViewModel.setProgress(newProgress)
            tabViewModel.isShowProgress.set(newProgress != 100)
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(activity, activity.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
