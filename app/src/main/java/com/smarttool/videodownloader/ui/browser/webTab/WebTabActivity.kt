package com.smarttool.videodownloader.ui.browser.webTab

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.webkit.HttpAuthHandler
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.ads.admob.data.ContentAd
import com.ads.admob.helper.banner.BannerAdConfig
import com.ads.admob.helper.banner.BannerAdHelper
import com.ads.admob.helper.banner.params.BannerAdParam
import com.ads.admob.listener.BannerAdCallBack
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.smarttool.videodownloader.android.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.smarttool.videodownloader.MainActivity
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.base.BaseActivity
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.android.databinding.LayoutBottomSheetPermissionBinding
import com.smarttool.videodownloader.dialog.DialogInformationImage
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import com.smarttool.videodownloader.feature.browser.presentation.BrowserSettingsViewModel
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoDetectionAlgVModel
import com.smarttool.videodownloader.ui.media.PlayMediaActivity
import com.smarttool.videodownloader.feature.library.presentation.PrivateVideoViewModel
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.presentation.TabModelViewModel
import com.smarttool.videodownloader.feature.browser.domain.AdBlockerHelper
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.AppLogger
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.feature.browser.domain.FaviconUtils
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.SystemUtil
import com.smarttool.videodownloader.feature.browser.domain.VideoUtils
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.ads.setOnClickListenerWithShowInterAd
import com.smarttool.videodownloader.core.ads.showInterAll
import com.vimalcvs.materialrating.DialogManager
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel
import android.widget.FrameLayout
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smarttool.videodownloader.android.databinding.LayoutBannerContainerBinding
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.browser.presentation.WebTabScreen
import com.smarttool.videodownloader.feature.browser.presentation.WebTabUiState
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonState
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.usecase.SaveHistoryEntryUseCase
import org.koin.android.ext.android.inject
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUi
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosSheet
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import com.smarttool.videodownloader.feature.downloads.domain.model.DownloadTabListener
import com.smarttool.videodownloader.ui.tab.TabsActivity

const val HOME_TAB_INDEX = 0

class WebTabActivity : BaseComposeActivity(), DownloadTabListener, IDownloadImage {

    private lateinit var webTab: WebTab

    private val tabViewModel: WebTabViewModel by koinViewModel()

    private val privateVideoViewModel: PrivateVideoViewModel by koinViewModel()

    private val settingsViewModel: BrowserSettingsViewModel by koinViewModel()

    private val saveHistoryEntry: SaveHistoryEntryUseCase by inject()

    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by koinViewModel()

    private val videoDetectionModel: VideoDetectionAlgVModel by koinViewModel()

    private lateinit var historyItemCurrent: HistoryItem

    private var bundle: Bundle? = null

    private var isReload = false
    private val appUtil: SystemUiController by inject()
    private val proxyController: CustomProxyController by inject()
    private val okHttpProxyClient: OkHttpProxyClient by inject()
    private val fileUtil: FileUtil by inject()

    private val tabModelViewModel: TabModelViewModel by koinViewModel()


    private lateinit var bottomSheetDialog: BottomSheetDialog

    private val processingViewModel: ProcessingViewModel by koinViewModel()

    private var uiState by mutableStateOf(WebTabUiState())

    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()

    private var detectedVideos by mutableStateOf<List<DetectedVideoUi>>(emptyList())
    private var showDetectedSheet by mutableStateOf(false)

    /** Hosts the WebView so existing show/hide logic keeps working under Compose. */
    private val webViewContainer by lazy { FrameLayout(this) }

    /** Receives the page's fullscreen video via WebChromeClient.onShowCustomView. */
    private val fullscreenContainer by lazy { FrameLayout(this) }

    private val bannerBinding by lazy { LayoutBannerContainerBinding.inflate(layoutInflater) }

    var videoAlert: MaterialAlertDialogBuilder? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""
    private var lastRegularCheckUrl = ""
    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()


    private lateinit var permissionLayoutBinding: LayoutBottomSheetPermissionBinding

    private lateinit var bottomSheetPermissionDialog: BottomSheetDialog

    private lateinit var animation: AlphaAnimation

    private lateinit var handler: Handler

    private lateinit var runnable: Runnable

    private var webViewClient = object : WebViewClient() {

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val viewTitle = view?.title
            val title = tabViewModel.currentTitle.get()
            val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

            if (url != null && lastSavedHistoryUrl != url) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val icon = try {
                            FaviconUtils.getEncodedFaviconFromUrl(
                                okHttpProxyClient.getProxyOkHttpClient(), url
                            )
                        } catch (e: Throwable) {
                            null
                        }

                        val outputFavicon = FaviconUtils.bitmapToBytes(icon)

                        saveUrlToHistory(url, icon, viewTitle ?: title)

                        videoDetectionTabViewModel.onStartPage(
                            url,
                            userAgent
                                ?: BrowserUserAgent.MOBILE
                        )
                        tabViewModel.onUpdateVisitedHistory(
                            url,
                            title,
                            userAgent
                        )

                        // Add null check and create new tab if none exists
                        var tabModel = tabModelViewModel.getSelectedTabModel()
                        if (tabModel == null) {
                            // Create a new tab
                            tabModel =
                                TabModel(url = url, isSelected = true, favicon = outputFavicon)
                            tabModelViewModel.insertTabModel(tabModel)
                        } else {
                            // Update existing tab
                            tabModel.url = url
                            tabModelViewModel.updateInfoTabModel(
                                tabModel.id,
                                url,
                                outputFavicon,
                                tabModel.isSelected
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
            view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?,
        ) {
            val creds = proxyController.getProxyCredentials()
            handler?.proceed(creds.first, creds.second)
        }

        override fun shouldInterceptRequest(
            view: WebView?, request: WebResourceRequest?,
        ): WebResourceResponse? {
            val isAdBlockerOn = true
            val url = request?.url.toString()

            val isUrlAd: Boolean = isAdBlockerOn && tabViewModel.isAd(url)

            if (isUrlAd) {
                return AdBlockerHelper.createEmptyResource()
            }

            var isCheckM3u8 = true
            var isCheckOnMp4 = true

            if (isCheckOnMp4 || isCheckM3u8) {

                val requestWithCookies = request?.let { resourceRequest ->
                    try {
                        CookieUtils.webRequestToHttpWithCookies(
                            resourceRequest
                        )
                    } catch (e: Throwable) {
                        null
                    }
                }

                val contentType =
                    VideoUtils.getContentTypeByUrl(
                        url,
                        requestWithCookies?.headers,
                        okHttpProxyClient
                    )

                when {

                    contentType == ContentType.M3U8 || contentType == ContentType.MPD || url.contains(
                        ".m3u8"
                    ) || url.contains(
                        ".mpd"
                    ) || (url.contains(".txt") && url.contains("hentaihaven")) -> {
                        if (requestWithCookies != null && isCheckM3u8) {
                            videoDetectionTabViewModel.verifyLinkStatus(
                                requestWithCookies, tabViewModel.currentTitle.get(), true
                            )

                        }
                    }

                    else -> {
                        if (isCheckOnMp4) {
                            val disposable =
                                videoDetectionTabViewModel.checkRegularMp4(requestWithCookies)

                            val currentUrl = tabViewModel.getTabTextInput().get() ?: ""
                            if (currentUrl != lastRegularCheckUrl) {
                                regularJobsStorage[lastRegularCheckUrl]?.forEach {
                                    it.dispose()
                                }
                                regularJobsStorage.remove(lastRegularCheckUrl)
                                lastRegularCheckUrl = currentUrl
                            }
                            if (disposable != null) {
                                val overall = mutableListOf<Disposable>()
                                overall.addAll(
                                    regularJobsStorage[currentUrl]?.toList() ?: emptyList()
                                )
                                overall.add(disposable)
                                regularJobsStorage[currentUrl] = overall
                            }
                        }
                    }
                }
            }

            return super.shouldInterceptRequest(
                view, request
            )
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            videoAlert = null

            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    var tabModelSelected = tabModelViewModel.getSelectedTabModel()
                    if (tabModelSelected == null) {
                        // Create a new tab
                        tabModelSelected = TabModel(
                            url = url,
                            isSelected = true,
                            favicon = outputFavicon
                        )
                        tabModelViewModel.insertTabModel(tabModelSelected)
                    } else {
                        tabModelSelected.url = url

                        tabModelViewModel.updateInfoTabModel(
                            tabModelSelected.id,
                            url, outputFavicon, tabModelSelected.isSelected
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

            val isAdBlockerOn = true
            val isAd = if (isAdBlockerOn) tabViewModel.isAd(url) else false

            return if (url.startsWith("http") && request.isForMainFrame && !isAd) {
                if (!tabViewModel.isTabInputFocused.get()) {
                    tabViewModel.setTabTextInput(url)
                }
                false
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
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
            view: WebView?, detail: RenderProcessGoneDetail?,
        ): Boolean {
            view?.destroy() // Hủy WebView cũ
            return true // Trả về true nếu đã xử lý, false nếu muốn để WebView mặc định xử lý
        }
    }

    private var webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?,
        ): Boolean {
            if (view != null && view.handler != null) {
                val href = view.handler.obtainMessage()
                view.requestFocusNodeHref(href)
                val url = href.data.getString("url") ?: ""

                val isBlockAds = true

                val isAd = if (isBlockAds) {
                    tabViewModel.isAd(url)
                } else {
                    false
                }

                if (url.isEmpty() || !url.startsWith("http") || isAd || !isUserGesture) {
                    return false
                }

                val transport = resultMsg!!.obj as WebView.WebViewTransport
                transport.webView = WebView(view.context)

                tabViewModel.openPageEvent.value =
                    WebTab(
                        webview = transport.webView,
                        resultMsg = resultMsg,
                        url = "url",
                        title = view.title,
                        iconBytes = null
                    )
                return true
            }
            return false
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {}

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)

            tabViewModel.setProgress(newProgress)
            if (newProgress == 100) {
                tabViewModel.isShowProgress.set(false)
            } else {
                tabViewModel.isShowProgress.set(true)
            }

        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            webViewContainer.visibility = View.GONE

            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            fullscreenContainer.addView(view)
            appUtil.hideSystemUI(window, fullscreenContainer)
            fullscreenContainer.visibility = View.VISIBLE
            uiState = uiState.copy(isFullscreen = true)
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
            fullscreenContainer.removeAllViews()
            webViewContainer.visibility = View.VISIBLE
            fullscreenContainer.visibility = View.GONE
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            uiState = uiState.copy(isFullscreen = false)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            if (settingsViewModel.isLockPortrait.get()) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            appUtil.showSystemUI(window, fullscreenContainer)
        }
    }

    private var canGoCounter = 0

    fun injectDownloadButton() {
        webTab.getWebView()?.evaluateJavascript(
            """
(function() {
    function isValidImage(img) {
        let imageUrl = img.src || img.style.backgroundImage.replace(/url\(["']?|["']?\)/g, "");

        // Loại bỏ ảnh từ domain static.xx.fbcdn.net (icon, emoji)
        if (imageUrl.includes("static.xx.fbcdn.net")) {
            return false;
        }

        // Loại bỏ ảnh nhỏ hơn 100x100 (đây là mức tương đối để lọc icon)
        if (img.naturalWidth < 100 || img.naturalHeight < 100) {
            return false;
        }

        return true;
    }

    function addDownloadButton(img) {
        if (!isValidImage(img) || img.dataset.downloadAdded) return;
        img.dataset.downloadAdded = true;

        console.log("✅ Adding download button to:", img.src); // Debug log

        let btn = document.createElement('button');
        btn.innerText = '📥';
        btn.style.position = 'absolute';
        btn.style.top = '10px';
        btn.style.right = '10px';
        btn.style.zIndex = '9999';
        btn.style.background = 'rgba(0,0,0,0.7)';
        btn.style.color = 'white';
        btn.style.border = 'none';
        btn.style.padding = '6px 11px';
        btn.style.cursor = 'pointer';
        btn.style.fontSize = '17px';
        btn.style.pointerEvents = 'auto';
        btn.style.borderRadius = '5px';

        btn.addEventListener('click', function(event) {
            event.stopPropagation();
            event.preventDefault();
            window.Android.downloadImageUpdate(img.src);
        });

        let parent = img.closest('div[role="img"]') || img.parentNode;
        if (parent) {
            parent.style.position = 'relative';
            parent.appendChild(btn);
        }
    }

    function scanImages() {
        document.querySelectorAll('div[role="img"], img').forEach(img => {
            addDownloadButton(img);
        });
    }

    // Gọi hàm lần đầu tiên khi trang load
    scanImages();

    // Dùng MutationObserver để theo dõi khi có ảnh mới xuất hiện khi cuộn
    let observer = new MutationObserver((mutations) => {
        mutations.forEach(mutation => {
            mutation.addedNodes.forEach(node => {
                if (node.nodeType === 1) {
                    let imgs = node.querySelectorAll('div[role="img"], img');
                    imgs.forEach(img => addDownloadButton(img));
                }
            });
        });
    });

    observer.observe(document.body, { childList: true, subtree: true });

})();

        """, null
        )
    }


    private val serviceWorkerClient = object : ServiceWorkerClient() {
        override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
            Log.d("ntt", "shouldInterceptRequest: serviceWorkerClient")
            val url = request.url.toString()

            val isM3u8Check = true
            val isMp4Check = true

            if (isM3u8Check || isMp4Check) {
                val requestWithCookies = request.let { resourceRequest ->
                    try {
                        CookieUtils.webRequestToHttpWithCookies(
                            resourceRequest
                        )
                    } catch (e: Throwable) {
                        null
                    }
                }

                val contentType =
                    VideoUtils.getContentTypeByUrl(
                        url,
                        requestWithCookies?.headers,
                        okHttpProxyClient
                    )

                if (contentType == ContentType.MPD || contentType == ContentType.M3U8 || url.contains(
                        ".m3u8"
                    ) || url.contains(
                        ".mpd"
                    ) || url.contains(".txt")
                ) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (requestWithCookies != null && isM3u8Check) {
                            videoDetectionModel.verifyLinkStatus(requestWithCookies, "", true)
                        }
                    }
                } else if (contentType == ContentType.MP4 && isMp4Check) {
                    videoDetectionModel.checkRegularMp4(requestWithCookies)
                }
            }

            return super.shouldInterceptRequest(request)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bundle = savedInstanceState

        setContent {
            AppTheme {
                WebTabScreen(
                    state = uiState,
                    webView = webViewContainer,
                    fullscreenContainer = fullscreenContainer,
                    bannerAd = bannerBinding.root,
                    onUrlChange = { uiState = uiState.copy(url = it) },
                    onSubmitUrl = ::submitUrl,
                    onBack = ::onCloseTab,
                    onNavigateBack = ::navigateBack,
                    onNavigateForward = ::navigateForward,
                    onReload = ::reloadPage,
                    onShare = {
                        shareUrlWithDescription(this, webTab.getUrl(), webTab.getTitle(), webTab.getUrl())
                    },
                    onBookmark = ::saveUrlToHistoryBookmark,
                    onOpenTabs = ::openTabs,
                    onDownload = { videoDetectionTabViewModel.showVideoInfo() },
                )

                if (showDetectedSheet) {
                    DetectedVideosSheet(
                        videos = detectedVideos,
                        onSelectFormat = { video, option ->
                            onSelectFormatById(video.id, option.format)
                        },
                        onRename = ::renameDetectedVideo,
                        onPreview = ::previewDetectedVideo,
                        onDownload = ::downloadDetectedVideo,
                        onDismiss = { showDetectedSheet = false },
                    )
                }
            }
        }

        initView()
    }

    private val bannerAdHelper by lazy { initBannerAd() }
    private fun initBannerAd(): BannerAdHelper {
        val config = BannerAdConfig(
            idAds = BuildConfig.BANNER_ALL,
            canShowAds = AdsConstant.showBannerAll,
            canReloadAds = true,
            adPlacement = "banner_web_tab",
        )
        return BannerAdHelper(activity = this, lifecycleOwner = this, config = config)
    }

    private fun loadAd() {
        if (AdsConstant.showBannerAll) {
            bannerAdHelper.setBannerContentView(bannerBinding.frAdsBanner)
            bannerAdHelper.requestAds(BannerAdParam.Request)
            bannerAdHelper.registerAdListener(object : BannerAdCallBack {
                override fun onAdImpression() {
                }

                override fun onAdClicked() {
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                }

                override fun onAdFailedToShow(adError: AdError) {
                }

                override fun onAdLoaded(data: ContentAd) {
                }
            })
        }
    }

    private fun initView() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val selectedTab = tabModelViewModel.getSelectedTabModel()
                if (selectedTab == null) {
                    // Create initial tab if none exists
                    val initialTab = TabModel(
                        url = webTab.getUrl(),
                        isSelected = true
                    )
                    tabModelViewModel.insertTabModel(initialTab)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        loadAd()
        videoDetectionTabViewModel.webTabModel = tabViewModel

        videoDetectionTabViewModel.start()

        videoDetectionModel.start()

        tabViewModel.start()

        handler = Handler(Looper.getMainLooper())

        animation =
            AlphaAnimation(1f, 0.2f)

        animation.duration = 700

        animation.interpolator = LinearInterpolator()
        animation.repeatCount = Animation.INFINITE

        animation.repeatMode = Animation.REVERSE


        val swController = ServiceWorkerController.getInstance()
        swController.setServiceWorkerClient(serviceWorkerClient)
        swController.serviceWorkerWebSettings.allowContentAccess = true


        bottomSheetDialog = BottomSheetDialog(this, R.style.FullScreenBottomSheetDialogTheme)

        bottomSheetPermissionDialog = BottomSheetDialog(this, R.style.CustomAlertBottomSheet)

        observeDownloadOutcomes()

        videoDetectionTabViewModel.downloadButtonState.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                uiState = uiState.copy(
                    downloadButtonState = when (videoDetectionTabViewModel.downloadButtonState.get()) {
                        is DownloadButtonStateCanDownload -> DownloadButtonUiState.Enabled
                        is DownloadButtonStateLoading -> DownloadButtonUiState.Loading
                        else -> DownloadButtonUiState.Disabled
                    },
                )
            }
        })

        webTab = intent.extras?.getSerializable("webtab") as WebTab

        recreateWebView(bundle)

        val message = webTab.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab.flushMessage()
        } else {
            tabViewModel.loadPage(webTab.getUrl())
        }

        handleLoadPageEvent()

        handleOpenDetectedVideos()

        handleVideoPushed()

        configureWebView()

        registerForContextMenu(webTab.getWebView())

        tabModelViewModel.queryAllTabModel().observe(this) {
            uiState = uiState.copy(tabCount = it.size)
        }

        tabViewModel.loadPageEvent.observe(this) {
            webTab = it
        }

        tabViewModel.tabUrl.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
                    uiState = uiState.copy(url = tabViewModel.tabUrl.get().orEmpty())
                }
            }

        })

        tabViewModel.progress.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {

                    uiState = uiState.copy(progress = tabViewModel.progress.get())

                    if (tabViewModel.progress.get() == 100) {
                        uiState = uiState.copy(showProgress = false)
                    } else {
                        uiState = uiState.copy(showProgress = true)
                    }
                }
            }

        })

        tabViewModel.isShowProgress.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                lifecycleScope.launch(Dispatchers.Main) {
                    if (tabViewModel.isShowProgress.get()) {
                        uiState = uiState.copy(isLoadingPage = true)
                        isReload = false
                    } else {
                        uiState = uiState.copy(isLoadingPage = false)
                        isReload = true
                    }
                }
            }

        })

    }

    private fun submitUrl() {
        val url = uiState.url
        if (url.isEmpty()) return

        videoDetectionTabViewModel.cancelAllCheckJobs()
        tabViewModel.openPage(url)
    }

    private fun onCloseTab() {
        videoDetectionTabViewModel.cancelAllCheckJobs()

        if (intent.getStringExtra("open") == "tab") {
            showInterAll {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
            }
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Mirrors the WebView's history state into the Compose chrome. */
    private fun refreshNavState() {
        val webView = webTab.getWebView() ?: return
        uiState = uiState.copy(
            canGoBack = webView.canGoBack(),
            canGoForward = webView.canGoForward(),
        )
    }

    private fun navigateBack() {
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

    private fun navigateForward() {
        val webView = webTab.getWebView() ?: return

        if (webView.canGoForward()) {
            webView.goForward()
            tabViewModel.onGoForward(webView)
            videoDetectionTabViewModel.cancelAllCheckJobs()
            refreshNavState()
        }
    }

    private fun openTabs() {
        showInterAll {
            startActivity(Intent(this, TabsActivity::class.java))
        }
    }

    /**
     * Doubles as stop-loading: while a page is in flight the control shows a close
     * icon, matching the View implementation's [isReload] flag.
     */
    private fun reloadPage() {
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

    private fun configureWebView() {

        val webSettings = webTab.getWebView()?.settings
        val webView = webTab.getWebView()

        webView?.webChromeClient = webChromeClient
        webView?.webViewClient = webViewClient

        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView?.isScrollbarFadingEnabled = true

        webSettings?.apply {
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

        webView?.addJavascriptInterface(WebAppInterface(this), "Android")

        AppLogger.d(webTab.toString())

        webViewContainer.addView(
            webTab.getWebView(),
            LinearLayout.LayoutParams(-1, -1)
        )
    }

    private fun generateFileName(url: String): String {
        val extension = url.substringAfterLast(".", "jpg") // Lấy đuôi file hoặc mặc định là "jpg"
        return "downloaded_image_${System.currentTimeMillis()}.$extension"
    }

    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    private fun handleLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(this) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())


            }
        }
    }

    private fun handleOpenDetectedVideos() {
        videoDetectionTabViewModel.showDetectedVideosEvent.observe(this) {
            val list = videoDetectionTabViewModel.detectedVideosList.get()
            val firstItem = list?.firstOrNull()

            if (firstItem != null) {
                if (!checkStoragePermission() || !checkNotificationPermission()) {
                    showBottomSheetPermission()
                } else {
                    openDetectedSheet()
                }
            } else {
                // Xử lý khi danh sách rỗng
                Log.d("ntt", "Danh sách rỗng, không có phần tử đầu tiên")
            }
        }
    }

    private fun handleVideoPushed() {
        videoDetectionTabViewModel.videoPushedEvent.observe(this) {
            onVideoPushed()
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = v as WebView
        val result = webView.hitTestResult

        val imageUrl = result.extra

        if (imageUrl != null) {
            val dialogInformationImage =
                DialogInformationImage(
                    this@WebTabActivity,
                    imageUrl,
                    result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE,
                    true,
                    onClickOpenNewTab = {
                        if (imageUrl.startsWith("http")) {
                            webTab.getWebView()?.stopLoading()
                            webTab.getWebView()?.loadUrl(imageUrl)

                        }
                    },
                    onClickShare = {
                        shareUrl(this@WebTabActivity, imageUrl)
                    },
                    onClickCopyLink = {
                        val clipboard =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("copied_text", imageUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            this,
                            getString(R.string.string_copied_to_clipboard), Toast.LENGTH_SHORT
                        )
                            .show()
                    },
                    onClickDownloadImage = {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (!checkNotificationPermission() || !checkStoragePermission()) {
                                showBottomSheetPermission()
                            } else {
                                downloadImage(this@WebTabActivity, imageUrl)
                            }
                        }
                    })

            dialogInformationImage.show()
        }
    }

    override fun onPause() {
        super.onPause()
        onWebViewPause()
    }

    override fun onResume() {
        super.onResume()
        onWebViewResume()
    }

    private fun onVideoPushed() {
        showToastVideoFound()

        val isDownloadsVisible = true
        val isCond = !tabViewModel.isDownloadDialogShown.get() && !isDownloadsVisible
        if (settingsViewModel.getVideoAlertState()
                .get() && isCond
        ) {
            lifecycleScope.launch(Dispatchers.Main) {
                showAlertVideoFound()
            }
        }
    }

    private fun onWebViewPause() {
        webTab.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab.getWebView()?.onResume()
    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(this))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }

    private fun shareUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ liên kết qua:"))
    }

    fun shareUrlWithDescription(context: Context, url: String, title: String, description: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title) // Tiêu đề
            putExtra(Intent.EXTRA_TEXT, description) // Nội dung và đường dẫn
        }
        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"))
    }

    private fun showAlertVideoFound() {
        if (!tabViewModel.isDownloadDialogShown.get()) {
            tabViewModel.isDownloadDialogShown.set(true)

            videoAlert =
                MaterialAlertDialogBuilder(this).setTitle(R.string.string_video_found)

            videoAlert?.setOnDismissListener {
                videoAlert = null
            }
            videoAlert?.setMessage(R.string.whatshould)?.setPositiveButton(
                R.string.view
            ) { dialog, _ ->
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.setNeutralButton(R.string.dontshow) { dialog, _ ->
                settingsViewModel.setShowVideoAlertOff()
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.setNegativeButton(R.string.string_cancel) { dialog, _ ->
                tabViewModel.isDownloadDialogShown.set(false)
                dialog.dismiss()
            }?.show()
        }
    }

    private fun showToastVideoFound() {

        Toast.makeText(
            this, getString(R.string.string_video_found), Toast.LENGTH_SHORT
        ).show()

    }

    private suspend fun downloadImage(context: Context, imageUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                if (imageUrl.startsWith("data:image")) {
                    return@withContext saveBase64Image(context, imageUrl)
                }

                val client = OkHttpClient()
                val request = Request.Builder().url(imageUrl).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("Download", "Download failed: ${response.code}")
                    return@withContext null
                }

                val url = URL(imageUrl)
                var fileName = generateFileName(url.toString())

                if (fileName.isBlank()) fileName = "downloaded_image.jpg"

                val outputFile: File
                val fileSize: Long

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext null

                    val fileDescriptor =
                        resolver.openFileDescriptor(uri, "w") ?: return@withContext null
                    fileSize = response.body?.contentLength() ?: 0

                    FileOutputStream(fileDescriptor.fileDescriptor).use { outputStream ->
                        response.body?.byteStream()?.copyTo(outputStream)
                    }

                    fileDescriptor.close()

                    outputFile = File(getRealPathFromURI(context, uri) ?: return@withContext null)
                } else {
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    outputFile = File(downloadsDir, fileName)

                    response.body?.byteStream()?.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    fileSize = outputFile.length()
                }

                // Lưu vào Database
                val fileDate = System.currentTimeMillis()

                // 🔥 **Lưu vào Database**
                val videoTaskItem = VideoTaskItem(
                    mId = outputFile.absolutePath.hashCode().toString(),
                    fileName = fileName,
                    filePath = outputFile.absolutePath,
                    fileSize = fileSize,
                    fileDate = fileDate,
                    url = imageUrl,
                    mimeType = "image"
                )
                privateVideoViewModel.insertVideoTaskItem(videoTaskItem)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "File saved: ${outputFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@withContext outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private suspend fun saveBase64Image(
        context: Context,
        base64String: String,
    ): File? {
        return withContext(Dispatchers.IO) {
            try {
                val base64Data = base64String.substringAfter("base64,", "")

                if (base64Data.isBlank()) {
                    Log.e("ntt", "Invalid base64 data")
                    return@withContext null
                }

                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val fileName = "downloaded_base64_image_${System.currentTimeMillis()}.jpg"

                val outputFile: File?
                val fileSize: Long

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            ?: return@withContext null

                    val fileDescriptor =
                        resolver.openFileDescriptor(uri, "w") ?: return@withContext null
                    FileOutputStream(fileDescriptor.fileDescriptor).use { it.write(decodedBytes) }
                    fileDescriptor.close()

                    outputFile = File(getRealPathFromURI(context, uri) ?: return@withContext null)
                    fileSize = decodedBytes.size.toLong()
                } else {
                    val picturesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    outputFile = File(picturesDir, fileName)

                    FileOutputStream(outputFile).use { it.write(decodedBytes) }
                    fileSize = outputFile.length()
                }

                val fileDate = System.currentTimeMillis()

                val videoTaskItem = VideoTaskItem(
                    mId = outputFile.absolutePath.hashCode().toString(),
                    fileName = fileName,
                    filePath = outputFile.absolutePath,
                    fileSize = fileSize,
                    fileDate = fileDate,
                    url = base64String,
                    mimeType = "image"
                )
                privateVideoViewModel.insertVideoTaskItem(videoTaskItem)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "File saved: ${outputFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return@withContext outputFile
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private suspend fun saveUrlToHistory(url: String, favicon: Bitmap?, title: String?) {
        val isTitleEmpty = title?.trim()?.isEmpty() == true

        if (!isTitleEmpty && lastSavedTitleHistory != title && lastSavedHistoryUrl != url && url.isNotEmpty() && !url.contains(
                "about:blank"
            )
        ) {
            lastSavedHistoryUrl = url
            lastSavedTitleHistory = title ?: ""

            val outputFavicon = FaviconUtils.bitmapToBytes(favicon)

            yield()

            historyItemCurrent = HistoryItem(
                url = url, favicon = outputFavicon, title = title
            )

            saveHistoryEntry(historyItemCurrent.toEntry())
        }
    }

    private fun HistoryItem.toEntry() = HistoryEntry(
        id = id,
        title = title.orEmpty(),
        url = url,
        datetime = datetime,
        isBookmark = isBookmark,
        favicon = favicon,
    )

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

    private fun onSelectFormatById(id: String, format: String) {
        val info = findVideoInfo(id) ?: return
        onSelectFormat(info, format)
        refreshDetectedVideos()
    }

    private fun renameDetectedVideo(video: DetectedVideoUi) {
        DialogRename(this, video.title) { newName ->
            val titles = videoDetectionTabViewModel.formatsTitles.get()?.toMutableMap()
                ?: mutableMapOf()
            titles[video.id] = newName
            videoDetectionTabViewModel.formatsTitles.set(titles)
            refreshDetectedVideos()
        }.show()
    }

    private fun previewDetectedVideo(video: DetectedVideoUi) {
        val info = findVideoInfo(video.id) ?: return
        val format = video.selectedFormat ?: return
        onPreviewVideo(info, format, false)
    }

    private fun downloadDetectedVideo(video: DetectedVideoUi) {
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

    private fun saveUrlToHistoryBookmark() {
        if (this::historyItemCurrent.isInitialized) {

            historyItemCurrent.isBookmark = true
            lifecycleScope.launch(Dispatchers.IO) {
                saveHistoryEntry(historyItemCurrent.toEntry())

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WebTabActivity,
                        getString(R.string.string_save_to_bookmarks_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }



    private fun showBottomSheetPermission() {

        permissionLayoutBinding = LayoutBottomSheetPermissionBinding.inflate(layoutInflater)

        bottomSheetPermissionDialog.setContentView(permissionLayoutBinding.root)

        bottomSheetPermissionDialog.setCanceledOnTouchOutside(true);

        val behavior = bottomSheetPermissionDialog.behavior

        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Xử lý sự kiện thay đổi trạng thái của bottom sheet
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }

                    BottomSheetBehavior.STATE_EXPANDED -> {
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }

                    BottomSheetBehavior.STATE_DRAGGING -> {
                        behavior.setState(BottomSheetBehavior.STATE_EXPANDED)
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Xử lý khi bottom sheet được trượt
            }
        }

        SystemUtil.setLocale(this@WebTabActivity)

        bottomSheetPermissionDialog.behavior.addBottomSheetCallback(bottomSheetCallback)

        runnable = Runnable {
            if (permissionLayoutBinding.btnStorage.isEnabled) {
                permissionLayoutBinding.btnStorage.startAnimation(animation)
            } else if (permissionLayoutBinding.btnNotification.isEnabled) {
                permissionLayoutBinding.btnNotification.startAnimation(animation)
            }
        }

        handler.postDelayed(runnable, 5000L)

        if (checkStoragePermission()) {

            permissionLayoutBinding.btnStorage.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            permissionLayoutBinding.btnStorage.isEnabled = false
            permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

            permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))

            permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
            permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

            permissionLayoutBinding.btnNotification.isEnabled = true
            permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)
            permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))

        } else {

            permissionLayoutBinding.btnNotification.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            permissionLayoutBinding.btnStorage.isEnabled = true
            permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)
            permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#FFFFFF"))

            permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
            permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)


            permissionLayoutBinding.btnNotification.isEnabled = false
            permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

            permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#808080"))

        }

        permissionLayoutBinding.btnClose.setOnClickListener {
            bottomSheetPermissionDialog.dismiss()
        }

        permissionLayoutBinding.btnNotification.setOnClickListener {
            permissionLayoutBinding.btnNotification.clearAnimation()

            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 5000L)

            requestNotificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        permissionLayoutBinding.btnStorage.setOnClickListener {
            requestPermissionStorage()
        }

        bottomSheetPermissionDialog.show()
    }

    private fun requestPermissionStorage() {

        permissionLayoutBinding.btnStorage.clearAnimation()

        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 5000L)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageImageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                99
            )
        }
    }

    private val storageImageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            storageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        }

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->

            if (isGranted) {
                if (checkNotificationPermission()) {

                    bottomSheetPermissionDialog.dismiss()
                } else {
                    permissionLayoutBinding.btnNotification.clearAnimation()
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, 5000L)
                    permissionLayoutBinding.btnStorage.isEnabled = false
                    permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                    permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                    permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                    permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))


                    permissionLayoutBinding.btnNotification.isEnabled = true
                    permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                    permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))
                }
            } else {
                showSettingsDialog()
            }

        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 99) {
            if (grantResults.isNotEmpty()) {
                val read = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (read && write) {
                    if (checkNotificationPermission()) {

                        bottomSheetPermissionDialog.dismiss()
                    } else {
                        permissionLayoutBinding.btnNotification.clearAnimation()
                        handler.removeCallbacks(runnable)
                        handler.postDelayed(runnable, 5000L)
                        permissionLayoutBinding.btnStorage.isEnabled = false
                        permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                        permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                        permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                        permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#808080"))


                        permissionLayoutBinding.btnNotification.isEnabled = true
                        permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                        permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#FFFFFF"))
                    }
                } else {
                    showSettingsDialog()
                }
            }
        }

    }


    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->

        if (isGranted) {
            if (checkStoragePermission()) {

                bottomSheetPermissionDialog.dismiss()
            } else {
                permissionLayoutBinding.btnNotification.clearAnimation()
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)
                permissionLayoutBinding.btnNotification.isEnabled = false
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)

                permissionLayoutBinding.btnNotification.setTextColor(Color.parseColor("#808080"))

                permissionLayoutBinding.btnStorage.isEnabled = true
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)

                permissionLayoutBinding.btnStorage.setTextColor(Color.parseColor("#FFFFFF"))
            }
        } else {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this@WebTabActivity)
        builder.setTitle(getString(R.string.string_permission))
            .setMessage(getString(R.string.permission_setting))
            .setPositiveButton(getString(R.string.string_ok)) { _: DialogInterface, _: Int ->
                openAppSettings()
            }
            .setCancelable(false)

        val dialog = builder.create()
        builder.setOnDismissListener {
        }
        dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()
        startSettingResult.launch(intent)
    }

    val startSettingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (checkStoragePermission() && checkNotificationPermission()) {
            bottomSheetDialog.dismiss()
        } else {

            if (checkStoragePermission()) {

                permissionLayoutBinding.btnStorage.clearAnimation()

                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)

                permissionLayoutBinding.btnStorage.isEnabled = false
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.btnStorage.setTextColor("#808080".toColorInt())

                permissionLayoutBinding.tvDes.text = getString(R.string.string_notification)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_notification)

                permissionLayoutBinding.btnNotification.isEnabled = true
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_skip_permission)
                permissionLayoutBinding.btnNotification.setTextColor("#FFFFFF".toColorInt())

            } else {

                permissionLayoutBinding.btnNotification.clearAnimation()

                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 5000L)

                permissionLayoutBinding.btnStorage.isEnabled = true
                permissionLayoutBinding.btnStorage.setBackgroundResource(R.drawable.bg_btn_skip_permission)
                permissionLayoutBinding.btnStorage.setTextColor("#FFFFFF".toColorInt())

                permissionLayoutBinding.tvDes.text = getString(R.string.string_storage)
                permissionLayoutBinding.imgStorage.setImageResource(R.drawable.ic_storage)

                permissionLayoutBinding.btnNotification.isEnabled = false
                permissionLayoutBinding.btnNotification.setBackgroundResource(R.drawable.bg_btn_exit)

                permissionLayoutBinding.btnNotification.setTextColor("#808080".toColorInt())

            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun onVideoDownloadPropagate(
        videoInfo: VideoInfo, videoTitle: String, format: String,
    ) {
        val info = videoInfo.copy(
            title = FileNameCleaner.cleanFileName(videoTitle),
            formats = VideFormatEntityList(videoInfo.formats.formats.filter {
                it.format?.contains(
                    format
                ) ?: false
            })
        )

        processingViewModel.start(info)

        bottomSheetDialog.dismiss()
    }



    override fun onDestroy() {
        super.onDestroy()
        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()
    }

    override fun onCancel() {
        bottomSheetDialog.dismiss()
    }

    @OptIn(UnstableApi::class)
    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        startActivity(
            Intent(
                this, PlayMediaActivity::class.java
            ).apply {

                val selectedFormatTitle = videoDetectionTabViewModel.formatsTitles.get()
                val title = selectedFormatTitle?.get(videoInfo.id)
                val currFormat = videoInfo.formats.formats.filter {
                    format?.let { it1 ->
                        it.format?.contains(
                            it1 as CharSequence
                        )
                    } ?: false
                }

                putExtra(PlayMediaActivity.VIDEO_NAME, title)

                if (currFormat.isNotEmpty()) {
                    val headers = currFormat.first().httpHeaders?.let {
                        JSONObject(
                            currFormat.first().httpHeaders ?: emptyMap<String, String>()
                        ).toString()
                    } ?: "{}"

                    putExtra(
                        PlayMediaActivity.VIDEO_URL, currFormat.first().url
                    )
                    putExtra(
                        PlayMediaActivity.ITEM_TYPE, "video"
                    )
                    val headersFinal = if (isForce) "{}" else headers
                    putExtra(
                        PlayMediaActivity.VIDEO_HEADERS, headersFinal
                    )
                }
            })
    }

    override fun onDownloadVideo(videoInfo: VideoInfo, format: String, videoTitle: String) {
        onVideoDownloadPropagate(videoInfo, videoTitle, format)
    }

    override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
        val formats =
            videoDetectionTabViewModel.selectedFormats.get()?.toMutableMap() ?: mutableMapOf()
        formats[videoInfo.id] = format
        videoDetectionTabViewModel.selectedFormats.set(formats)
    }

    override fun iDownloadImageUpdate(imageUrl: String) {
        val dialogInformationImage =
            DialogInformationImage(
                this@WebTabActivity,
                imageUrl.toString(),
                true,
                false,
                onClickOpenNewTab = {
                    if (imageUrl.startsWith("http")) {
                        webTab.getWebView()?.stopLoading()
                        webTab.getWebView()?.loadUrl(imageUrl)

                    }
                },
                onClickShare = {
                    shareUrl(this@WebTabActivity, imageUrl)
                },
                onClickCopyLink = {
                    val clipboard =
                        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("copied_text", imageUrl)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(
                        this,
                        getString(R.string.string_copied_to_clipboard), Toast.LENGTH_SHORT
                    )
                        .show()
                },
                onClickDownloadImage = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        if (!checkNotificationPermission() || !checkStoragePermission()) {
                            showBottomSheetPermission()
                        } else {
                            downloadImage(this@WebTabActivity, imageUrl)
                        }
                    }
                })

        dialogInformationImage.show()
    }


    /**
     * Surfaces one toast per finished download. The id sets guard against the polled
     * flow re-emitting the same terminal state on every tick.
     */
    private fun observeDownloadOutcomes() {
        val notifiedSuccess = mutableSetOf<String>()
        val notifiedError = mutableSetOf<String>()

        lifecycleScope.launch {
            processingViewModel.downloads.collect { downloads ->
                for (info in downloads) {
                    when (info.downloadStatus) {
                        VideoTaskState.SUCCESS -> if (notifiedSuccess.add(info.id)) {
                            toast(R.string.string_download_successful)
                            DialogManager.showRatingAfterDoFunction(
                                this@WebTabActivity,
                                AppConstant.FEEDBACK_EMAIL,
                            )
                        }

                        VideoTaskState.ERROR, VideoTaskState.ENOSPC -> if (notifiedError.add(info.id)) {
                            toast(R.string.string_download_failed)
                        }
                    }
                }
            }
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

}

interface IDownloadImage {
    fun iDownloadImageUpdate(imageUrl: String)
}

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun downloadImageUpdate(imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            val iDownloadImage = context as IDownloadImage
            iDownloadImage.iDownloadImageUpdate(imageUrl)
        }
    }

}

