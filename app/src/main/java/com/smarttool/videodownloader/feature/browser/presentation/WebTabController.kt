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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
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
import com.smarttool.videodownloader.core.AppLogger
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.di.ScopedViewModelStore
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.permission.StoragePermissionSheet
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.dialog.DialogInformationImage
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.browser.domain.AdBlockerHelper
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.feature.browser.domain.FaviconUtils
import com.smarttool.videodownloader.feature.browser.domain.VideoUtils
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.downloads.domain.model.DownloadTabListener
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUi
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.usecase.SaveHistoryEntryUseCase
import com.smarttool.videodownloader.feature.library.presentation.PrivateVideoViewModel
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.presentation.TabModelViewModel
import com.vimalcvs.materialrating.DialogManager
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
 */
class WebTabController(
    private val activity: AppCompatActivity,
    private val permissionSheet: StoragePermissionSheet,
    private val permissionChecker: MediaPermissionChecker,
) : DownloadTabListener, DownloadImageHandler, KoinComponent {

    /**
     * Rebuilt for every browser session. Clearing a store cancels its ViewModels'
     * `viewModelScope` for good, and the detection pipeline runs on that scope — reusing
     * the same instances after a close would leave a browser that silently detects
     * nothing. Each session gets fresh ones, exactly as a new Activity used to.
     */
    private var viewModels = ScopedViewModelStore()

    private lateinit var tabViewModel: WebTabViewModel
    private lateinit var privateVideoViewModel: PrivateVideoViewModel
    private lateinit var settingsViewModel: BrowserSettingsViewModel
    private lateinit var videoDetectionTabViewModel: DetectedVideosTabViewModel
    private lateinit var videoDetectionModel: VideoDetectionAlgVModel
    private lateinit var tabModelViewModel: TabModelViewModel
    private lateinit var processingViewModel: ProcessingViewModel

    private val saveHistoryEntry: SaveHistoryEntryUseCase by inject()
    private val appUtil: SystemUiController by inject()
    private val proxyController: CustomProxyController by inject()
    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()

    /** Set by the route composable so the controller can drive navigation. */
    var onOpenTabs: () -> Unit = {}
    var onCloseBrowser: () -> Unit = {}
    var onPreviewMedia: (url: String, title: String, headers: String) -> Unit = { _, _, _ -> }

    var uiState by mutableStateOf(WebTabUiState())
        private set

    var detectedVideos by mutableStateOf<List<DetectedVideoUi>>(emptyList())
        private set

    var showDetectedSheet by mutableStateOf(false)

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
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

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
        privateVideoViewModel = viewModels.get()
        settingsViewModel = viewModels.get()
        videoDetectionTabViewModel = viewModels.get()
        videoDetectionModel = viewModels.get()
        tabModelViewModel = viewModels.get()
        processingViewModel = viewModels.get()

        webTab = WebTabFactory.createWebTabFromInput(url)
        uiState = WebTabUiState(url = webTab.getUrl())

        loadAd()
        registerServiceWorkerClient()

        videoDetectionTabViewModel.webTabModel = tabViewModel
        videoDetectionTabViewModel.start()
        videoDetectionModel.start()
        tabViewModel.start()

        ensureSelectedTabModel()
        observeDownloadOutcomes()
        observeDetectionState()
        observeTabState()

        recreateWebView()
        configureWebView()
        activity.registerForContextMenu(webTab.getWebView())

        tabViewModel.loadPage(webTab.getUrl())
    }

    fun release() {
        if (!started) return
        started = false

        videoDetectionTabViewModel.cancelAllCheckJobs()
        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()

        regularJobsStorage.values.flatten().forEach { it.dispose() }
        regularJobsStorage.clear()

        webTab.getWebView()?.let { webView ->
            activity.unregisterForContextMenu(webView)
            webViewContainer.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
        webTab.setWebView(null)

        webViewContainer.removeAllViews()
        fullscreenContainer.removeAllViews()
        showDetectedSheet = false
        detectedVideos = emptyList()
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
                if (tabModelViewModel.getSelectedTabModel() == null) {
                    tabModelViewModel.insertTabModel(
                        TabModel(url = webTab.getUrl(), isSelected = true),
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

        videoDetectionTabViewModel.videoPushedEvent.observe(activity) { onVideoPushed() }
    }

    private fun observeTabState() {
        tabModelViewModel.queryAllTabModel().observe(activity) {
            uiState = uiState.copy(tabCount = it.size)
        }

        // The event only carries the URL to load. It must not replace [webTab]:
        // `WebTabViewModel.loadPage` builds a tab with no WebView at all, and this
        // controller owns the only one there is.
        tabViewModel.loadPageEvent.observe(activity) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.setUrl(tab.getUrl())
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())
            }
        }

        tabViewModel.tabUrl.addOnPropertyChangedCallback(
            object : OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    uiState = uiState.copy(url = tabViewModel.tabUrl.get().orEmpty())
                }
            },
        )

        tabViewModel.progress.addOnPropertyChangedCallback(
            object : OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    val progress = tabViewModel.progress.get()
                    uiState = uiState.copy(progress = progress, showProgress = progress != 100)
                }
            },
        )

        tabViewModel.isShowProgress.addOnPropertyChangedCallback(
            object : OnPropertyChangedCallback() {
                override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                    val loading = tabViewModel.isShowProgress.get()
                    uiState = uiState.copy(isLoadingPage = loading)
                    isReload = !loading
                }
            },
        )
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
                            DialogManager.showRatingAfterDoFunction(
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

        videoDetectionTabViewModel.cancelAllCheckJobs()
        tabViewModel.openPage(url)
    }

    fun onUrlChange(url: String) {
        uiState = uiState.copy(url = url)
    }

    fun closeTab() {
        videoDetectionTabViewModel.cancelAllCheckJobs()
        onCloseBrowser()
    }

    fun openTabs() {
        onOpenTabs()
    }

    fun requestDetectedVideos() {
        videoDetectionTabViewModel.showVideoInfo()
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, webTab.getTitle())
            putExtra(Intent.EXTRA_TEXT, webTab.getUrl())
        }
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.string_share)))
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
            tabViewModel.onGoBack(webView)
            videoDetectionTabViewModel.cancelAllCheckJobs()
            refreshNavState()
        } else {
            canGoCounter = if (canGoCounter >= 1) 0 else canGoCounter + 1
        }
    }

    fun navigateForward() {
        val webView = webTab.getWebView() ?: return

        if (webView.canGoForward()) {
            webView.goForward()
            tabViewModel.onGoForward(webView)
            videoDetectionTabViewModel.cancelAllCheckJobs()
            refreshNavState()
        }
    }

    /**
     * Doubles as stop-loading: while a page is in flight the control shows a close
     * icon, matching the View implementation's [isReload] flag.
     */
    fun reloadPage() {
        if (!isReload) {
            tabViewModel.onPageStop(webTab.getWebView())
            return
        }

        var url = webTab.getWebView()?.url
        var urlWasChange = false

        if (url?.contains("m.facebook") == true) {
            url = url.replace("m.facebook", "www.facebook")
            urlWasChange = true
        }

        val userAgent = webTab.getWebView()?.settings?.userAgentString
            ?: tabViewModel.userAgent.get()
            ?: BrowserUserAgent.MOBILE

        if (url == null) return

        videoDetectionTabViewModel.viewModelScope.launch(
            videoDetectionTabViewModel.executorReload,
        ) {
            videoDetectionTabViewModel.onStartPage(url, userAgent)
        }

        if (url.contains("www.facebook") && urlWasChange) {
            tabViewModel.openPage(url)
            tabViewModel.closeTab(webTab)
        } else {
            tabViewModel.onPageReload(webTab.getWebView())
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
    }

    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
        val formats =
            videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        formats[videoInfo.id] = format
        videoDetectionTabViewModel.selectedFormats.set(formats)
    }

    private fun onVideoPushed() {
        toast(R.string.string_video_found)

        val isDownloadsVisible = true
        val isCond = !tabViewModel.isDownloadDialogShown.get() && !isDownloadsVisible

        if (settingsViewModel.getVideoAlertState().get() && isCond) {
            showAlertVideoFound()
        }
    }

    private fun showAlertVideoFound() {
        if (tabViewModel.isDownloadDialogShown.get()) return

        tabViewModel.isDownloadDialogShown.set(true)

        videoAlert = MaterialAlertDialogBuilder(activity).setTitle(R.string.string_video_found)

        videoAlert?.setOnDismissListener { videoAlert = null }
        videoAlert?.setMessage(R.string.whatshould)
            ?.setPositiveButton(R.string.view) { dialog, _ ->
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }
            ?.setNeutralButton(R.string.dontshow) { dialog, _ ->
                settingsViewModel.setShowVideoAlertOff()
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }
            ?.setNegativeButton(R.string.string_cancel) { dialog, _ ->
                tabViewModel.isDownloadDialogShown.set(false)
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
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.isScrollbarFadingEnabled = true

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
            mediaPlaybackRequiresUserGesture = false
            userAgentString = BrowserUserAgent.MOBILE
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        AppLogger.d(webTab.toString())

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
            val title = tabViewModel.currentTitle.get()
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

                        videoDetectionTabViewModel.onStartPage(url, userAgent)
                        tabViewModel.onUpdateVisitedHistory(url, title, userAgent)

                        val tabModel = tabModelViewModel.getSelectedTabModel()
                        if (tabModel == null) {
                            tabModelViewModel.insertTabModel(
                                TabModel(url = url, isSelected = true, favicon = outputFavicon),
                            )
                        } else {
                            tabModel.url = url
                            tabModelViewModel.updateInfoTabModel(
                                tabModel.id,
                                url,
                                outputFavicon,
                                tabModel.isSelected,
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
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
            videoAlert = null

            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)

            activity.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val selected = tabModelViewModel.getSelectedTabModel()
                    if (selected == null) {
                        tabModelViewModel.insertTabModel(
                            TabModel(url = url, isSelected = true, favicon = outputFavicon),
                        )
                    } else {
                        selected.url = url
                        tabModelViewModel.updateInfoTabModel(
                            selected.id,
                            url,
                            outputFavicon,
                            selected.isSelected,
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            val isAd = tabViewModel.isAd(url)

            return if (url.startsWith("http") && request.isForMainFrame && !isAd) {
                if (!tabViewModel.isTabInputFocused.get()) {
                    tabViewModel.setTabTextInput(url)
                }
                false
            } else {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            refreshNavState()
            super.onPageFinished(view, url)
            tabViewModel.finishPage(url)

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

    /**
     * Long-lived MP4 probes are keyed by the page that started them, so navigating
     * away disposes the previous page's checks instead of leaking them.
     */
    private fun trackRegularMp4(requestWithCookies: okhttp3.Request?) {
        val disposable = videoDetectionTabViewModel.checkRegularMp4(requestWithCookies)
        val currentUrl = tabViewModel.getTabTextInput().get() ?: ""

        if (currentUrl != lastRegularCheckUrl) {
            regularJobsStorage[lastRegularCheckUrl]?.forEach { it.dispose() }
            regularJobsStorage.remove(lastRegularCheckUrl)
            lastRegularCheckUrl = currentUrl
        }

        if (disposable != null) {
            regularJobsStorage[currentUrl] =
                (regularJobsStorage[currentUrl].orEmpty()) + disposable
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
            activity.requestedOrientation = if (settingsViewModel.isLockPortrait.get()) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            appUtil.showSystemUI(activity.window, fullscreenContainer)
        }
    }

    private val serviceWorkerClient = object : ServiceWorkerClient() {
        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
            val url = request.url.toString()

            val requestWithCookies = try {
                CookieUtils.webRequestToHttpWithCookies(request)
            } catch (e: Throwable) {
                null
            }

            val contentType = VideoUtils.getContentTypeByUrl(
                url,
                requestWithCookies?.headers,
                okHttpProxyClient,
            )

            val isManifest = contentType == ContentType.MPD ||
                contentType == ContentType.M3U8 ||
                url.contains(".m3u8") ||
                url.contains(".mpd") ||
                url.contains(".txt")

            if (isManifest) {
                if (requestWithCookies != null) {
                    activity.lifecycleScope.launch(Dispatchers.Main) {
                        videoDetectionModel.verifyLinkStatus(requestWithCookies, "", true)
                    }
                }
            } else if (contentType == ContentType.MP4) {
                videoDetectionModel.checkRegularMp4(requestWithCookies)
            }

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
                AppLogger.e("Image download failed: ${response.code}")
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
            e.printStackTrace()
            null
        }
    }

    private suspend fun saveBase64Image(base64String: String): File? = withContext(Dispatchers.IO) {
        try {
            val base64Data = base64String.substringAfter("base64,", "")
            if (base64Data.isBlank()) {
                AppLogger.e("Invalid base64 data")
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
            e.printStackTrace()
            null
        }
    }

    private suspend fun saveDownloadedImage(
        outputFile: File,
        fileName: String,
        fileSize: Long,
        sourceUrl: String,
    ) {
        privateVideoViewModel.insertVideoTaskItem(
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
