package com.smarttool.videodownloader.feature.browser.presentation

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.SingleLiveEvent
import com.smarttool.videodownloader.core.browser.BrowserUserAgent
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.scheduler.BaseSchedulers
import com.smarttool.videodownloader.data.model.VideoInfoWrapper
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoFormatEntity
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.data.remote.service.LoginRequiredException
import com.smarttool.videodownloader.data.remote.service.VideoServiceLocal
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonState
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanNotDownload
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateLoading
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetVideoDetectionThresholdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.InterruptedIOException
import java.net.HttpCookie
import java.net.URL
import java.util.concurrent.Executors

/**
 * The video-detection pipeline: takes the requests a page makes, works out which of them
 * are downloadable videos, and accumulates what it finds in [detectedVideosList].
 *
 * Requests reach it through a [VideoSniffer], which every WebView host owns one of.
 */
class DetectedVideosTabViewModel(
    private val getVideoDetectionThreshold: GetVideoDetectionThresholdUseCase,
    private val baseSchedulers: BaseSchedulers,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val videoServiceLocal: VideoServiceLocal,
    private val appContext: Context,
) : ViewModel() {
    val selectedFormats = ObservableField<Map<String, String>>()

    val formatsTitles = ObservableField<Map<String, String>>()

    @Volatile
    var m3u8LoadingList = ObservableField<MutableSet<String>>()

    @Volatile
    var regularLoadingList = ObservableField<MutableSet<String>>()

    val showDetectedVideosEvent = SingleLiveEvent<Void?>()

    val videoPushedEvent = SingleLiveEvent<Void?>()

    val loginRequiredEvent = SingleLiveEvent<String>()

    @Volatile
    var downloadButtonState =
        ObservableField<DownloadButtonState>(DownloadButtonStateCanNotDownload())

    val executorReload = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    /** The page this detector watches; set once by its host through [attach]. */
    var webTabModel: WebTabViewModel? = null
        private set

    val detectedVideosList = ObservableField(mutableSetOf<VideoInfo>())

    private val executorRegular = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Volatile
    private var verifyVideoLinkJobStorage = mutableMapOf<String, Job>()

    init {
        regularLoadingList.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (regularLoadingList.get()?.isNotEmpty() == true) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
        m3u8LoadingList.addOnPropertyChangedCallback(object :
            OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if (m3u8LoadingList.get()?.isNotEmpty() == true) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
    }

    /**
     * Binds the detector to the page it watches and primes the ad-host list it filters
     * requests against. The host calls this once, right after building both view models.
     */
    fun attach(webTabModel: WebTabViewModel) {
        Timber.d("Video detector attached")
        this.webTabModel = webTabModel
        viewModelScope.launch {
            webTabModel.setListHost()
        }
    }

    override fun onCleared() {
        Timber.d("Video detector cleared")
        cancelAllCheckJobs()
        super.onCleared()
    }

    fun onStartPage(url: String, userAgentString: String) {
        // Nếu url rỗng hoặc không hợp lệ, reset trạng thái và return
        if (url.isBlank() || !url.startsWith("http")) {
            downloadButtonState.set(DownloadButtonStateCanNotDownload())
            detectedVideosList.set(mutableSetOf())
            cancelAllCheckJobs()
            return
        }

        downloadButtonState.set(DownloadButtonStateCanNotDownload())
        detectedVideosList.set(mutableSetOf())
        cancelAllCheckJobs()

        val req = getRequestWithHeadersForUrl(
            url,
            url,
            userAgentString
        )?.build()

        if (req != null) {
            verifyLinkStatus(req)
        }
    }

    fun showVideoInfo() {
        Timber.d("showVideoInfo: state=${downloadButtonState.get()}")
        val state = downloadButtonState.get()

        if (state is DownloadButtonStateCanNotDownload) {
            webTabModel?.getTabTextInput()?.get()?.let {
                if (it.startsWith("http")) {
                    viewModelScope.launch(executorRegular) {
                        onStartPage(
                            it.trim(),
                            webTabModel?.userAgent?.get() ?: BrowserUserAgent.MOBILE
                        )
                    }
                }
            }
        }

        if (detectedVideosList.get()?.isNotEmpty() == true) {
            showDetectedVideosEvent.call()
        }
    }

    fun verifyLinkStatus(
        resourceRequest: Request,
        hlsTitle: String? = null,
        isM3u8: Boolean = false,
    ) {
        // TODO list of sites, where youtube dl should be disabled
        if (resourceRequest.url.toString().contains("tiktok.")) {
            return
        }

        val urlToVerify = resourceRequest.url.toString()
        if (isM3u8) {
            startVerifyProcess(resourceRequest, true, hlsTitle)
        } else {
            if (urlToVerify.contains(
                    ".txt"
                )
            ) {
                return
            }
//            if (settingsViewModel.getIsFindVideoByUrl().get()) {
            startVerifyProcess(resourceRequest, false)
//            }
        }
    }


    private fun startVerifyProcess(
        resourceRequest: Request, isM3u8: Boolean, hlsTitle: String? = null
    ) {
        val taskUrlCleaned = resourceRequest.url.toString().split("?").firstOrNull()?.trim() ?: ""

        val job = verifyVideoLinkJobStorage[taskUrlCleaned]
        if (job != null && job.isActive || taskUrlCleaned.isEmpty()) {
            return
        }

        val loadings = m3u8LoadingList.get()?.toMutableSet()
        val url = resourceRequest.url.toString()
        loadings?.add(url)
        Timber.d("m3u8LoadingList add: $url -> $loadings")
        m3u8LoadingList.set(loadings?.toMutableSet())
        setButtonState(DownloadButtonStateLoading())

        verifyVideoLinkJobStorage[taskUrlCleaned] =
            viewModelScope.launch(baseSchedulers.videoService) {
                try {
                    val info = try {
                        videoServiceLocal.getVideoInfo(resourceRequest, isM3u8)?.videoInfo
                    } catch (e: LoginRequiredException) {
                        loginRequiredEvent.postValue(e.host)
                        null
                    } catch (e: Throwable) {
                        Timber.w(e, "startVerifyProcess failed: ${resourceRequest.url}")
                        null
                    }

                    val result = info ?: VideoInfo(id = "")

                    withContext(baseSchedulers.computation) {
                        if (result.id.isNotEmpty()) {
                            if (result.isM3u8 && !hlsTitle.isNullOrEmpty()) {
                                result.title = hlsTitle
                            }
                            pushNewVideoInfoToAll(result)
                        } else {
                            setButtonState(DownloadButtonStateCanNotDownload())
                        }
                    }
                } finally {
                    val loadings2 = m3u8LoadingList.get()?.toMutableSet()
                    loadings2?.remove(url)
                    Timber.d("m3u8LoadingList remove: $url -> $loadings2")
                    m3u8LoadingList.set(loadings2?.toMutableSet())
                    verifyVideoLinkJobStorage.remove(taskUrlCleaned)
                }
            }
    }

    fun pushNewVideoInfoToAll(newInfo: VideoInfo) {
        if (newInfo.id.isEmpty()) {
            return
        }

        val currentTabUrl = webTabModel?.getTabTextInput()?.get()
        val isTwitch = currentTabUrl?.contains(".twitch.") == true

        if ((isTwitch) && !newInfo.isMaster) {
            return
        }
//
//        if(newInfo.downloadUrls.isNullOrEmpty()){
//            return
//        }

        val detected = detectedVideosList.get()?.toList() ?: emptyList()
        var contains = false
        if (newInfo.isRegularDownload) {
            for (vid in detected) {
                val one = vid.firstUrlToString
                val searching = newInfo.firstUrlToString
                contains = one == searching
                if (contains) {
                    break
                }
            }
        } else {
            for (vid in detected) {
                for (vF in vid.formats.formats) {
                    for (k in newInfo.formats.formats) {
                        if (vF.url == k.url) {
                            contains = true
                            break
                        }
                    }
                    if (contains) {
                        break
                    }
                }
                if (vid.originalUrl == newInfo.originalUrl) {
                    contains = true
                    break
                }
            }
        }
        if (contains) {
            return
        }

        Timber.d("PUSHING $newInfo  to list: \n  ${detectedVideosList.get()}")
        val list = detectedVideosList.get()?.toMutableSet() ?: mutableSetOf()
        list.add(newInfo)
        detectedVideosList.set(list)
        viewModelScope.launch(Dispatchers.Main) {
            videoPushedEvent.call()
        }
        setButtonState(DownloadButtonStateCanDownload(newInfo))
    }

    fun checkRegularMp4(request: Request?): Job? {
        if (request == null) {
            return null
        }

        val uriString = request.url.toString()

        val isAd = webTabModel?.isAd(uriString) ?: false
        if (!uriString.startsWith("http") || isAd) {
            return null
        }

        val clearedUrl = uriString.split("?").first().trim()

        if (clearedUrl.contains(Regex("^(.*\\.(apk|html|xml|ico|css|js|png|gif|json|jpg|jpeg|svg|woff|woff2|m3u8|mpd|ts|php|ttf|otf|eot|cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|vtt|srt|swf|jar|log|txt))?$"))) {
            return null
        }

        val headers = try {
            request.headers.toMap().toMutableMap()
        } catch (e: Throwable) {
            mutableMapOf()
        }

        return viewModelScope.launch(baseSchedulers.io) {
            try {
                if (request.url.toString().contains(".mp4")) {
                    Timber.d("setButtonState(DownloadButtonStateLoading()) in checkRegularMp4 for url: ${request.url}")
                    setButtonState(DownloadButtonStateLoading())
                }
                val loadings = regularLoadingList.get()
                loadings?.add(request.url.toString())
                regularLoadingList.set(loadings?.toMutableSet())
                propagateCheckJob(uriString, headers)
            } catch (e: Throwable) {
                Timber.w("checkRegularMp4 failed: $clearedUrl")
            } finally {
                val loadings = regularLoadingList.get()
                loadings?.remove(request.url.toString())
                regularLoadingList.set(loadings?.toMutableSet())
            }
        }
    }

    fun cancelAllCheckJobs() {
        regularLoadingList.set(mutableSetOf())
        m3u8LoadingList.set(mutableSetOf())
        executorReload.cancel()
        executorRegular.cancel()
        verifyVideoLinkJobStorage.values.toList().forEach { process ->
            process.cancel()
        }
        verifyVideoLinkJobStorage.clear()
    }

    fun setButtonState(state: DownloadButtonState) {
        when (state) {
            is DownloadButtonStateCanDownload -> {
                downloadButtonState.set(state)
            }

            is DownloadButtonStateCanNotDownload -> {
                val detectedSize = detectedVideosList.get()?.size
                if (detectedSize == null || detectedSize == 0) {
                    val impEl = regularLoadingList.get()?.find { it.contains(".mp4") }
                    if (m3u8LoadingList.get()?.isEmpty() != true || (m3u8LoadingList.get()?.isEmpty() == true && impEl != null)
                    ) {
                        Timber.d("setButtonState(DownloadButtonStateLoading()) in setButtonState (CanNotDownload branch) ")
                        Timber.d("(CanNotDownload branch) -> ${m3u8LoadingList.get()?.isEmpty()} - $impEl")

                        downloadButtonState.set(DownloadButtonStateLoading())
                    } else {
                        downloadButtonState.set(DownloadButtonStateCanNotDownload())
                    }
                } else {
                    downloadButtonState.set(
                        DownloadButtonStateCanDownload(
                            detectedVideosList.get()?.first()
                        )
                    )
                }
            }

            is DownloadButtonStateLoading -> {
                val list = detectedVideosList.get() ?: emptySet()
                if (list.isEmpty()) {
                    Timber.d("setButtonState(DownloadButtonStateLoading()) in setButtonState (Loading branch)")
                    downloadButtonState.set(DownloadButtonStateLoading())
                } else {
                    downloadButtonState.set(DownloadButtonStateCanDownload(list.first()))
                }
            }
        }
    }

    private fun getRequestWithHeadersForUrl(
        url: String,
        originalUrl: String,
        userAgent: String,
        alternativeHeaders: Map<String, String> = emptyMap()
    ): Request.Builder? {
        try {
            val cookies = try {
                CookieManager.getInstance().getCookie(url) ?: CookieManager.getInstance()
                    .getCookie(originalUrl) ?: ""
            } catch (e: Throwable) {
                ""
            }
            val stringBuilder = StringBuilder()
            if (cookies.isNotEmpty()) {
                for (cookie in cookies.split(";")) {
                    val parsedCookies = HttpCookie.parse(cookie)

                    for (httpCookie in parsedCookies) {
                        stringBuilder.append("${httpCookie.name}=${httpCookie.value};")
                    }
                }
            }

            if (alternativeHeaders.isEmpty()) {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (e: Exception) {
                    null
                }
                builder?.addHeader("Referer", "https://${Uri.parse(originalUrl).host}/")

                builder?.addHeader("User-Agent", userAgent)

                try {
                    if (cookies.isNotEmpty()) {
                        builder?.addHeader("Cookie", stringBuilder.toString())
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to attach cookie header")
                }
                return builder

            } else {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (e: Exception) {
                    null
                }
                builder?.headers(alternativeHeaders.toHeaders())
                if (cookies.isNotEmpty() && alternativeHeaders["Cookie"] == null) {
                    builder?.addHeader("Cookie", stringBuilder.toString())
                }

                return builder
            }
        } catch (e: Throwable) {
            Timber.w(e, "Failed to build request headers")
        }

        return null
    }

    private suspend fun propagateCheckJob(url: String, headersMap: Map<String, String>) {
        val treshold = getVideoDetectionThreshold()
        var headers = headersMap.toMutableMap()
        val finlUrlPair = try {
            CookieUtils.getFinalRedirectURL(URL(Uri.parse(url).toString()), headers)
        } catch (e: Throwable) {
            null
        } ?: return

        try {
            val cookies = CookieManager.getInstance().getCookie(finlUrlPair.first.toString())
                ?: CookieManager.getInstance().getCookie(url) ?: ""
            if (cookies.isNotEmpty()) {
                headers["Cookie"] = cookies
            }
        } catch (_: Throwable) {

        }

        var respons: Response? = null
        try {
            headers = finlUrlPair.second.toMap().toMutableMap()
            val requestOk: Request =
                Request.Builder().url(finlUrlPair.first).headers(headers.toHeaders()).build()
            respons = okHttpProxyClient.getProxyOkHttpClient().newCall(requestOk).execute()

            val length = respons.body.contentLength()
            val type = respons.body.contentType()
            respons.body.close()

            if (respons.code == 403 || respons.code == 401) {
                val finlUrlPairEmpty = try {
                    CookieUtils.getFinalRedirectURL(URL(Uri.parse(url).toString()), emptyMap())
                } catch (e: Throwable) {
                    null
                }

                if (finlUrlPairEmpty != null) {
                    val emptyHeadersReq = Request.Builder().url(finlUrlPairEmpty.first).build()
                    val emptyRes =
                        okHttpProxyClient.getProxyOkHttpClient().newCall(emptyHeadersReq).execute()
                    if (emptyRes.body.contentType().toString()
                            .contains("video") && length > treshold
                    ) {
                        setVideoInfoWrapperFromUrl(
                            finlUrlPairEmpty.first,
                            webTabModel?.getTabTextInput()?.get(),
                            finlUrlPairEmpty.second.toMap(),
                            length
                        )
                        emptyRes.close()

                        return
                    }
                }
            }

            val isTikTok = url.contains(".tiktok.com/")
            if (type.toString()
                    .contains("video") && (length > treshold || (isTikTok && length > 1024 * 1024 / 3))
            ) {
                setVideoInfoWrapperFromUrl(
                    finlUrlPair.first,
                    webTabModel?.getTabTextInput()?.get(),
                    finlUrlPair.second.toMap(),
                    length
                )
            }
        } catch (e: InterruptedIOException) {
            Timber.d("propagateCheckJob cancelled (interrupted) for $url")
        } catch (e: Throwable) {
            Timber.e(e, "propagateCheckJob failed for $url")
        } finally {
            respons?.close()
        }
    }

    private fun setVideoInfoWrapperFromUrl(
        url: URL,
        originalUrl: String?,
        alternativeHeaders: Map<String, String> = emptyMap(),
        contentLength: Long
    ) {
        Timber.d("setVideoInfoWrapperFromUrl: url=$url contentLength=$contentLength")
        try {
            if (!url.toString().startsWith("http")) {
                return
            }

            val builder = if (originalUrl != null) {
                Request.Builder().url(url.toString()).headers(alternativeHeaders.toHeaders())
            } else {
                null
            }

            val downloadUrls = listOfNotNull(
                builder?.build()
            )

            val video = VideoInfoWrapper(
                VideoInfo(
                    downloadUrls = downloadUrls,
                    title = webTabModel?.currentTitle?.get() ?: "no_title",
                    ext = "mp4",
                    originalUrl = webTabModel?.getTabTextInput()?.get() ?: "",
                    // TODO format regular file link
                    formats = VideFormatEntityList(
                        mutableListOf(
                            VideoFormatEntity(
                                formatId = "0",
                                format = appContext.getString(R.string.player_resolution),
                                ext = "mp4",
                                url = downloadUrls.first().url.toString(),
                                httpHeaders = downloadUrls.first().headers.toMap(),
                                fileSize = contentLength
                            )
                        )
                    ),
                    isRegularDownload = true
                )
            )
            video.videoInfo?.let { pushNewVideoInfoToAll(it) }
        } catch (e: Throwable) {
            Timber.e(e, "setVideoInfoWrapperFromUrl failed")
        }
    }
}

