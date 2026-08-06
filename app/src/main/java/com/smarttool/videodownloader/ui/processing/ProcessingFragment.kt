package com.smarttool.videodownloader.ui.processing

import android.Manifest
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.webkit.WebViewCompat.setAudioMuted
import com.bumptech.glide.Glide
import com.google.android.gms.ads.nativead.NativeAd
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseFragment
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.android.databinding.LayoutBottomSheetPermissionBinding
import com.smarttool.videodownloader.dialog.DialogRename
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoDetectionAlgVModel
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import com.smarttool.videodownloader.ui.guide.GuideActivity
import com.smarttool.videodownloader.ui.media.PlayMediaActivity
import com.smarttool.videodownloader.feature.browser.domain.AdBlockerHelper
import com.smarttool.videodownloader.core.AppLogger
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.core.file.FileNameCleaner
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.SystemUtil
import com.smarttool.videodownloader.feature.browser.domain.VideoUtils
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.smarttool.videodownloader.VideoDownloaderApplication
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.smarttool.videodownloader.core.ads.setOnClickListenerWithShowInterAd
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import org.koin.androidx.viewmodel.ext.android.viewModel as koinViewModel
import com.smarttool.videodownloader.core.ads.showInterAll
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingUiState
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingScreen
import com.smarttool.videodownloader.feature.downloads.presentation.DownloadButtonUiState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.Fragment
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import android.view.LayoutInflater
import com.smarttool.videodownloader.feature.tab.presentation.TabViewModel
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanNotDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonState
import org.koin.android.ext.android.inject
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUi
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideosSheet
import com.smarttool.videodownloader.feature.downloads.domain.model.DownloadTabListener

class ProcessingFragment : Fragment(), DownloadTabListener {

    private val processingViewModel: ProcessingViewModel by koinViewModel()

    /** Global tab store, previously inherited from BaseFragment. */
    private val tabViewModels: TabViewModel by lazy {
        (requireActivity().application as VideoDownloaderApplication).globalViewModel
    }

    /** Mirrors the detection pipeline's button state into Compose. */
    private var uiState by mutableStateOf(ProcessingUiState())

    private val videoDetectionTabViewModel: DetectedVideosTabViewModel by koinViewModel()


    private lateinit var bottomSheetDialog: BottomSheetDialog

    private lateinit var permissionLayoutBinding: LayoutBottomSheetPermissionBinding

    private lateinit var bottomSheetPermissionDialog: BottomSheetDialog
    private val appUtil: SystemUiController by inject()
    private val okHttpProxyClient: OkHttpProxyClient by inject()

    private val detectedVideoUiMapper: DetectedVideoUiMapper by inject()
    private val sanitizeFileName: SanitizeFileNameUseCase by inject()

    private var detectedVideos by mutableStateOf<List<DetectedVideoUi>>(emptyList())
    private var showDetectedSheet by mutableStateOf(false)

    private lateinit var webTab: WebTab

    private val tabViewModel: WebTabViewModel by koinViewModel()
    private val fileUtil: FileUtil by inject()

    private var currentUrl = ""
    private val proxyController: CustomProxyController by inject()

    private val regularJobsStorage: MutableMap<String, List<Disposable>> = mutableMapOf()

    private var lastRegularCheckUrl = ""

    private var lastSavedHistoryUrl: String = ""

    private val videoDetectionModel: VideoDetectionAlgVModel by koinViewModel()

    private lateinit var animation: AlphaAnimation

    private lateinit var handler: Handler

    private lateinit var runnable: Runnable

    private var nativePopupPermission: NativeAd? = null

    private var webViewClient = object : WebViewClient() {
        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            val viewTitle = view?.title
            val title = tabViewModel.currentTitle.get()
            val userAgent = view?.settings?.userAgentString ?: BrowserUserAgent.MOBILE

            if (url != null && lastSavedHistoryUrl != url) {
                lifecycleScope.launch(Dispatchers.IO) {

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

                }
            }
            super.doUpdateVisitedHistory(view, url, isReload)
        }

        override fun onReceivedHttpAuthRequest(
            view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?
        ) {
            Log.d("ntt", "onReceivedHttpAuthRequest: ")
            val creds = proxyController.getProxyCredentials()
            handler?.proceed(creds.first, creds.second)
        }

        override fun shouldInterceptRequest(
            view: WebView?, request: WebResourceRequest?
        ): WebResourceResponse? {
            val isAdBlockerOn = true
            val url = request?.url.toString()

            val isUrlAd: Boolean = isAdBlockerOn && tabViewModel.isAd(url)

            if (isUrlAd) {
                return AdBlockerHelper.createEmptyResource()
            }

//            val isCheckM3u8 = settingsModel.isCheckIfEveryRequestOnM3u8.get()
//            val isCheckOnMp4 = settingsModel.getIsCheckEveryRequestOnMp4Video().get()

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

            tabViewModel.onStartPage(url, view.title)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: WebResourceRequest): Boolean {
            Log.d("ntt", "shouldOverrideUrlLoading: ")
            val isAdBlockerOn = true
            val isAd = if (isAdBlockerOn) tabViewModel.isAd(url.url.toString()) else false

            return if (url.url.toString().startsWith("http") && url.isForMainFrame && !isAd) {
                if (!tabViewModel.isTabInputFocused.get()) {
                    tabViewModel.setTabTextInput(url.url.toString())
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
            view: WebView?, detail: RenderProcessGoneDetail?
        ): Boolean {
            val pageTab = tabViewModels.getTabAt(tabViewModels.currentPositionTabWeb.value!!)

            val webView = pageTab?.getWebView()
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    view == webView && detail?.didCrash() == true
                } else {
                    view == webView
                }
            ) {
                webView?.destroy()
                return true
            }

            return super.onRenderProcessGone(view, detail)
        }
    }

    private var webChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
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

                AppLogger.d("ON_CREATE_WINDOW::************* $url ${view.url} isAd:: $isAd  $isUserGesture")
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

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {

        }

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
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
        }
    }

    private fun recreateWebView(savedInstanceState: Bundle?) {
        if (webTab.getMessage() == null || webTab.getWebView() == null) {
            webTab.setWebView(WebView(requireContext()))
        }

        if (savedInstanceState != null) {
            webTab.getWebView()?.restoreState(savedInstanceState)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        // The tab and its WebView must exist before they are handed to the composition.
        webTab = WebTabFactory.createWebTabFromInput(currentUrl)
        recreateWebView(savedInstanceState)
        configureWebView()

        setContent {
            val downloads by processingViewModel.downloads.collectAsStateWithLifecycle()

            AppTheme {
                ProcessingScreen(
                    state = uiState,
                    downloads = downloads,
                    detectionWebView = requireNotNull(webTab.getWebView()),
                    onUrlChange = ::onUrlChange,
                    onPasteClick = ::pasteFromClipboard,
                    onDownloadClick = { videoDetectionTabViewModel.showVideoInfo() },
                    onGuideClick = ::openGuide,
                    onPauseResume = ::onPauseResume,
                    onCancel = { processingViewModel.cancel(it, removeFile = true) },
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
    }

    private fun onUrlChange(url: String) {
        uiState = uiState.copy(url = url)
        currentUrl = url

        // Anything that is not an http(s) link cannot be probed, so reset the button.
        if (url.isBlank() || !url.startsWith("http")) {
            videoDetectionTabViewModel.downloadButtonState.set(DownloadButtonStateCanNotDownload())
            return
        }

        webTab.setUrl(url)
        tabViewModel.loadPage(webTab.getUrl())
    }

    private fun onPauseResume(info: ProgressInfo) {
        if (info.downloadStatus == VideoTaskState.PAUSE) {
            processingViewModel.resume(info)
        } else {
            processingViewModel.pause(info)
        }
    }

    private fun openGuide() {
        showInterAll {
            startActivity(
                GuideActivity.newIntent(requireContext()).apply { putExtra("open", "process") },
            )
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        val hasText = clipboard.hasPrimaryClip() &&
            clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

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

    private fun toast(messageRes: Int) {
        Toast.makeText(requireContext(), getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoDetectionTabViewModel.start()

        videoDetectionModel.start()

        videoDetectionTabViewModel.webTabModel = tabViewModel

        val message = webTab.getMessage()
        if (message != null) {
            message.sendToTarget()
            webTab.flushMessage()
        } else {
            tabViewModel.loadPage(webTab.getUrl())
        }

        handler = Handler(Looper.getMainLooper())

        animation =
            AlphaAnimation(1f, 0.2f)

        animation.duration = 700

        animation.interpolator = LinearInterpolator()
        animation.repeatCount = Animation.INFINITE

        animation.repeatMode = Animation.REVERSE

        handleLoadPageEvent()

        tabViewModel.loadPageEvent.observe(viewLifecycleOwner) {
            Log.d("ntt", "onViewCreated: webTab: $webTab")
            webTab = it
        }


        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.CustomAlertBottomSheet)

        bottomSheetPermissionDialog =
            BottomSheetDialog(requireContext(), R.style.CustomAlertBottomSheet)






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

        videoDetectionTabViewModel.showDetectedVideosEvent.observe(viewLifecycleOwner) {

            val list = videoDetectionTabViewModel.detectedVideosList.get()
            val firstItem = list?.firstOrNull()

            if (firstItem != null) {
                if (!checkStoragePermission() || !checkNotificationPermission()) {
                    showBottomSheetPermission()
                } else {
                    openDetectedSheet()
                }
            } else {
                Log.d("ntt", "Danh sách rỗng, không có phần tử đầu tiên")
            }
        }
        videoDetectionTabViewModel.videoPushedEvent.observe(viewLifecycleOwner) {
            onVideoPushed()
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

        SystemUtil.setLocale(requireContext())

        bottomSheetPermissionDialog.behavior.addBottomSheetCallback(bottomSheetCallback)

//        showAdsNativePopupPermission(permissionLayoutBinding.frAds)

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

//            permissionLayoutBinding.frAds.visibility = View.GONE

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

//        permissionLayoutBinding.frAds.visibility = View.GONE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageImageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            //Android is below 13(R)
            ActivityCompat.requestPermissions(
                requireActivity(),
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

//            permissionLayoutBinding.frAds.visibility = View.VISIBLE

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

//        permissionLayoutBinding.frAds.visibility = View.VISIBLE

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

//        permissionLayoutBinding.frAds.visibility = View.VISIBLE

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
//        frAds.visibility = View.GONE
//        permissionLayoutBinding.frAds.visibility = View.GONE
        var builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.string_permission))
            .setMessage(getString(R.string.permission_setting))
            .setPositiveButton(getString(R.string.string_ok)) { _: DialogInterface, _: Int ->
                openAppSettings()
            }
            .setCancelable(false)

        var dialog = builder.create()
        builder.setOnDismissListener {
//            frAds.visibility = View.VISIBLE
//            permissionLayoutBinding.frAds.visibility = View.VISIBLE
        }
        dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
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
        }

    }

    override fun onPause() {
        super.onPause()
        onWebViewPause()
    }

    override fun onResume() {
        super.onResume()
        onWebViewResume()
        VideoDownloaderApplication.instance.appResumeAdHelper.setEnableAppResumeOnScreen()
    }

    private fun configureWebView() {

        val webSettings = webTab.getWebView()?.settings
        val webView = webTab.getWebView()

        webView?.webChromeClient = webChromeClient
        webView?.webViewClient = webViewClient

        webView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView?.isScrollbarFadingEnabled = true

        webView?.let { setAudioMuted(it, true) }

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
            mediaPlaybackRequiresUserGesture = true // Chặn autoplay
        }

        AppLogger.d(webTab.toString())

        // The WebView is hosted by ProcessingScreen via AndroidView, so it is not
        // attached to a container here.
    }

    private fun onWebViewPause() {
        webTab.getWebView()?.onPause()
    }

    private fun onWebViewResume() {
        webTab.getWebView()?.onResume()
    }

    private fun handleLoadPageEvent() {
        tabViewModel.loadPageEvent.observe(viewLifecycleOwner) { tab ->
            if (tab.getUrl().startsWith("http")) {
                webTab.getWebView()?.stopLoading()
                webTab.getWebView()?.loadUrl(tab.getUrl())


            }
        }
    }

    private fun onVideoPushed() {
        Toast.makeText(
            requireContext(), getString(R.string.string_video_found), Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }

    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }




    private fun findDownload(downloadId: Long): ProgressInfo? =
        processingViewModel.downloads.value.firstOrNull { it.downloadId == downloadId }

    override fun onDestroyView() {
        super.onDestroyView()
        tabViewModel.stop()
        videoDetectionModel.stop()
        videoDetectionTabViewModel.stop()
    }

    private fun onVideoDownloadPropagate(
        videoInfo: VideoInfo, videoTitle: String, format: String
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

        Toast.makeText(
            requireContext(), getString(R.string.download_started), Toast.LENGTH_SHORT
        ).show()

    }

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
        DialogRename(requireContext(), video.title) { newName ->
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

    override fun onCancel() {
    }

    @OptIn(UnstableApi::class)
    override fun onPreviewVideo(videoInfo: VideoInfo, format: String, isForce: Boolean) {
        startActivity(
            Intent(
                requireContext(), PlayMediaActivity::class.java
            ).apply {

                Log.d("ntt", "onPreviewVideo: format: $format")

                val selectedFormatTitle = videoDetectionTabViewModel.formatsTitles.get()
                val title = selectedFormatTitle?.get(videoInfo.id)
                // you can add values(if any) to pass to the next class or avoid using `.apply`
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


}

class WrapContentLinearLayoutManager : LinearLayoutManager {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            AppLogger.e("meet a IOOBE in RecyclerView")
        }
    }
}
