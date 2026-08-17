package com.smarttool.videodownloader.feature.browser.presentation

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.android.R
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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
 * are downloadable videos, and accumulates what it finds in [DetectedVideosContract.State.detectedVideos].
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

    private val _uiState = MutableStateFlow(DetectedVideosContract.State())
    val uiState: StateFlow<DetectedVideosContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<DetectedVideosContract.Effect>(Channel.BUFFERED)
    val effect: Flow<DetectedVideosContract.Effect> = _effect.receiveAsFlow()

    val executorReload = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    /** The page this detector watches; set once by its host through [attach]. */
    var webTabModel: WebTabViewModel? = null
        private set

    private val executorRegular = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Volatile
    private var verifyVideoLinkJobStorage = mutableMapOf<String, Job>()

    /**
     * Binds the detector to the page it watches and primes the ad-host list it filters
     * requests against. The host calls this once, right after building both view models.
     */
    fun attach(webTabModel: WebTabViewModel) {
        Timber.d("Video detector attached")
        this.webTabModel = webTabModel
        viewModelScope.launch {
            webTabModel.onEvent(WebTabPipelineContract.Event.SetListHost)
        }
    }

    override fun onCleared() {
        Timber.d("Video detector cleared")
        cancelAllChecks()
        super.onCleared()
    }

    fun onEvent(event: DetectedVideosContract.Event) {
        when (event) {
            is DetectedVideosContract.Event.StartPage -> startPage(event.url, event.userAgent)
            DetectedVideosContract.Event.ShowVideoInfo -> showVideoInfo()
            is DetectedVideosContract.Event.VerifyLinkStatus ->
                verifyLinkStatus(event.request, event.hlsTitle, event.isM3u8)

            DetectedVideosContract.Event.CancelAllChecks -> cancelAllChecks()

            is DetectedVideosContract.Event.SelectFormat -> {
                val formats = _uiState.value.selectedFormats + (event.videoId to event.format)
                _uiState.update { it.copy(selectedFormats = formats) }
            }

            is DetectedVideosContract.Event.RenameTitle -> {
                val titles = _uiState.value.formatTitles + (event.videoId to event.title)
                _uiState.update { it.copy(formatTitles = titles) }
            }

            DetectedVideosContract.Event.MarkCanNotDownload ->
                setButtonState(DownloadButtonStateCanNotDownload())
        }
    }

    private fun startPage(url: String, userAgent: String) {
        _uiState.update {
            it.copy(downloadButtonState = DownloadButtonStateCanNotDownload(), detectedVideos = emptySet())
        }
        // Not killProcesses=true: this runs on every `doUpdateVisitedHistory` WebView
        // callback, which SPA-heavy pages (TikTok) fire repeatedly for the *same* video —
        // each hop differs only in a tracking query param, so taskUrlCleaned (below) stays
        // identical across them. Killing in-flight extractions here would abort a request
        // that's about to succeed every time such a page re-navigates internally; only an
        // actual tab close/back/forward/new-URL-submit (which dispatch CancelAllChecks
        // directly) should do that.
        cancelAllChecks(killProcesses = false)

        if (url.isBlank() || !url.startsWith("http")) return

        val req = getRequestWithHeadersForUrl(url, url, userAgent)?.build()
        if (req != null) verifyLinkStatus(req)
    }

    private fun showVideoInfo() {
        val state = _uiState.value
        Timber.d("showVideoInfo: state=${state.downloadButtonState}")

        if (state.downloadButtonState is DownloadButtonStateCanNotDownload) {
            val tabUrl = webTabModel?.uiState?.value?.tabUrl.orEmpty()
            if (tabUrl.startsWith("http")) {
                viewModelScope.launch(executorRegular) {
                    startPage(tabUrl.trim(), webTabModel?.uiState?.value?.userAgent ?: BrowserUserAgent.MOBILE)
                }
            }
        }

        // Sent even when empty: the host tells the sheet and the "no media found" dialog
        // apart by re-checking `detectedVideos` itself when this effect lands.
        _effect.trySend(DetectedVideosContract.Effect.ShowDetectedVideos)
    }

    private fun verifyLinkStatus(
        resourceRequest: Request,
        hlsTitle: String? = null,
        isM3u8: Boolean = false,
    ) {
        val urlToVerify = resourceRequest.url.toString()
        if (isM3u8) {
            startVerifyProcess(resourceRequest, true, hlsTitle)
        } else {
            if (urlToVerify.contains(".txt")) {
                return
            }
            startVerifyProcess(resourceRequest, false)
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

        val url = resourceRequest.url.toString()
        Timber.d("m3u8Loading add: $url")
        updateM3u8Loading { it + url }
        setButtonState(DownloadButtonStateLoading())

        verifyVideoLinkJobStorage[taskUrlCleaned] =
            viewModelScope.launch(baseSchedulers.videoService) {
                try {
                    val info = try {
                        videoServiceLocal.getVideoInfo(resourceRequest, isM3u8, taskUrlCleaned)?.videoInfo
                    } catch (e: LoginRequiredException) {
                        _effect.trySend(DetectedVideosContract.Effect.LoginRequired(e.host))
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
                            } else {
                                // No tab-wide title stamped over it — this is whatever
                                // videoServiceLocal parsed straight off this specific
                                // video's own page, so it's trustworthy per-item, unlike
                                // hlsTitle (see pushNewVideoInfoToAll).
                                result.isTitleTrusted = true
                            }
                            pushNewVideoInfoToAll(result)
                        } else {
                            setButtonState(DownloadButtonStateCanNotDownload())
                        }
                    }
                } finally {
                    Timber.d("m3u8Loading remove: $url")
                    updateM3u8Loading { it - url }
                    verifyVideoLinkJobStorage.remove(taskUrlCleaned)
                }
            }
    }

    private fun pushNewVideoInfoToAll(newInfo: VideoInfo) {
        if (newInfo.id.isEmpty()) {
            return
        }

        // An HLS page fetches both the master playlist and each quality's own media
        // playlist directly (e.g. ".../240p/index-v1-a1.m3u8"); WebView surfaces both as
        // separate requests, so both end up verified here independently. yt-dlp parses a
        // media playlist "successfully" but without the resolution/codec tags that only
        // live in the master, producing a real-but-unusable single-format VideoInfo (see
        // VideoFormatEntity.isUsable). If that lands first, the dedup check below — which
        // matches on shared format URLs — then rejects the *good* master result as a
        // "duplicate" of this junk one, since the master's own format list references the
        // same per-quality URL. Dropping these before they can occupy that dedup slot keeps
        // the accurate master entry free to land.
        if (newInfo.formats.formats.isNotEmpty() && newInfo.formats.formats.none { it.isUsable }) {
            Timber.d("pushNewVideoInfoToAll: dropping ${newInfo.originalUrl} — no usable format metadata")
            return
        }

        val currentTabUrl = webTabModel?.uiState?.value?.tabUrl
        val isTwitch = currentTabUrl?.contains(".twitch.") == true

        if ((isTwitch) && !newInfo.isMaster) {
            return
        }

        val detected = _uiState.value.detectedVideos.toList()

        // An untrusted title is a snapshot of the tab's *current* document.title, stamped
        // onto whichever manifest happened to be in flight at that moment — see
        // VideoInfo.isTitleTrusted. A feed page (several videos on one screen, e.g. a
        // Reels-style widget that prefetches upcoming items' manifests well before the
        // user swipes to them) fires many such requests while the title is stuck on
        // whichever item is currently visible, so an identical stamped title landing more
        // than once is a red flag that these are *different* videos sharing a coincidental
        // label, not proof they're the same one (their format/manifest URLs plainly
        // differ — that's exactly why the content-based dedup below doesn't catch them).
        // Relabel *both* sides to something each actually owns instead of either hiding
        // this one or leaving a stale, now-known-unreliable label on the earlier one — a
        // trusted push later still overwrites either with the real title (see
        // upgradeTitleIfMoreTrusted).
        if (!newInfo.isTitleTrusted && newInfo.title.isNotBlank()) {
            val colliding = detected.firstOrNull { it.title == newInfo.title }
            if (colliding != null) {
                Timber.d("pushNewVideoInfoToAll: relabeling collision on '${newInfo.title}'")
                newInfo.title = distinctFallbackTitle(newInfo)

                if (!colliding.isTitleTrusted) {
                    val relabeled = colliding.copy(title = distinctFallbackTitle(colliding))
                    val updated = _uiState.value.detectedVideos.map {
                        if (it.id == colliding.id) relabeled else it
                    }.toSet()
                    _uiState.update { it.copy(detectedVideos = updated) }
                }
            }
        }

        var contains = false
        var matchedExisting: VideoInfo? = null
        if (newInfo.isRegularDownload) {
            for (vid in detected) {
                val one = vid.firstUrlToString
                val searching = newInfo.firstUrlToString
                contains = one == searching
                if (contains) {
                    matchedExisting = vid
                    break
                }
            }
        } else {
            // CDN format URLs are signed per extraction call (fresh signature/expiry every
            // time, even for the exact same video), and TikTok's own short-link redirect
            // chain can resolve the same video to several differently-shaped page URLs
            // (with/without username, sec_uid form...) — see the WebTabViewHost redirect
            // log. Both existing checks below (format URL overlap, exact originalUrl match)
            // silently fail against each other for these, so the same video lands twice.
            // The numeric id in the path is the one thing TikTok keeps stable across all of
            // that, so fall back to it before giving up on a match.
            val newTikTokId = tikTokVideoId(newInfo.originalUrl)
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
                }
                if (!contains && newTikTokId != null && newTikTokId == tikTokVideoId(vid.originalUrl)) {
                    contains = true
                }
                if (contains) {
                    matchedExisting = vid
                    break
                }
            }
        }
        if (contains) {
            upgradeTitleIfMoreTrusted(matchedExisting, newInfo)
            return
        }

        Timber.d("PUSHING $newInfo  to list: \n  ${_uiState.value.detectedVideos}")
        val updated = _uiState.value.detectedVideos + newInfo
        _uiState.update { it.copy(detectedVideos = updated) }
        _effect.trySend(DetectedVideosContract.Effect.VideoPushed(newInfo))
        setButtonState(DownloadButtonStateCanDownload(newInfo))
    }

    /** TikTok's own numeric video id out of the URL path, e.g. `7665934019430862100` — stable across the `@user/`, `@/` and `@sec_uid/` page-URL shapes the same video can resolve to. */
    private fun tikTokVideoId(url: String): String? =
        Regex("""tiktok\.[a-z.]+/[^?#]*?/video/(\d+)""").find(url)?.groupValues?.getOrNull(1)

    /**
     * A page listing several videos (a feed) fires one manifest request per item, but the
     * tab only ever has one current title, so [startVerifyProcess] stamps every one of them
     * with whatever `document.title` happened to be at that moment — correct for at most
     * one item, wrong for the rest. The requests aren't ordered by relevance, so the first
     * copy of a given video to land here can easily be one of the wrongly-stamped ones,
     * while a later request for the *same* video (matched via [matchedExisting]) resolves
     * its title straight from that video's own page ([VideoInfo.isTitleTrusted]) and is
     * dropped as a plain "duplicate" by the caller. Patch the earlier entry's title in place
     * instead of losing that better title, so the sheet doesn't end up with several rows
     * that share a name none of them actually has.
     */
    private fun upgradeTitleIfMoreTrusted(matchedExisting: VideoInfo?, newInfo: VideoInfo) {
        if (matchedExisting == null) return
        if (matchedExisting.isTitleTrusted || !newInfo.isTitleTrusted) return
        if (newInfo.title.isBlank() || newInfo.title == matchedExisting.title) return

        Timber.d(
            "upgradeTitleIfMoreTrusted: '${matchedExisting.title}' -> '${newInfo.title}' " +
                "(id=${matchedExisting.id}, originalUrl=${matchedExisting.originalUrl})",
        )

        val updated = _uiState.value.detectedVideos.map { video ->
            if (video.id == matchedExisting.id) {
                video.copy(title = newInfo.title, isTitleTrusted = true)
            } else {
                video
            }
        }.toSet()

        _uiState.update { it.copy(detectedVideos = updated) }
    }

    /**
     * A readable stand-in title derived from [info]'s own manifest/original URL — used only
     * once we've proven (via a title collision in [pushNewVideoInfoToAll]) that the tab-title
     * stamp can't be trusted for this entry. Picks the longest hyphen-heavy path segment
     * (article/asset slugs are long and word-separated; template segments a CDN reuses
     * across every video on the site — encoded quality lists, "index-v1-a1", "master" —
     * don't have enough hyphens or length to qualify), strips a trailing "-<digits>" id, and
     * turns the hyphens into spaces. Distinct per video by construction, since it comes from
     * the same URL the content-based dedup above already treats as this video's identity —
     * falls back to the (colliding) stamped title if nothing slug-like is found.
     */
    private fun distinctFallbackTitle(info: VideoInfo): String {
        val source = info.originalUrl.ifBlank { info.formats.formats.firstOrNull()?.url.orEmpty() }
        val path = try {
            java.net.URI(source).path.orEmpty()
        } catch (e: Throwable) {
            source
        }

        val segment = path.split("/")
            .filter { it.length >= 12 && it.count { c -> c == '-' } >= 2 }
            .maxByOrNull { it.length }
            ?: return info.title

        return segment.replace(Regex("-\\d+$"), "")
            .replace('-', ' ')
            .trim()
            .takeIf { it.length >= 8 }
            ?.replaceFirstChar { it.uppercase() }
            ?: info.title
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
                updateRegularLoading { it + request.url.toString() }
                propagateCheckJob(uriString, headers)
            } catch (e: Throwable) {
                Timber.w(e, "checkRegularMp4 failed: $clearedUrl")
            } finally {
                updateRegularLoading { it - request.url.toString() }
            }
        }
    }

    /**
     * @param killProcesses Cancelling a Job alone only stops it at its next suspension
     * point — it can't interrupt the blocking yt-dlp subprocess call a job is parked on
     * (e.g. TikTok's up-to-45s retry loop); the taskId keyed here (see [startVerifyProcess])
     * is what actually reaches into [VideoServiceLocal] to kill it. Real exits (tab close,
     * back/forward, submitting a new URL) want that. [startPage] does not: it also runs on
     * every same-page `doUpdateVisitedHistory` hop an SPA like TikTok fires while a check
     * for that exact video is still in flight, where killing it would abort a request that
     * was about to succeed.
     */
    private fun cancelAllChecks(killProcesses: Boolean = true) {
        executorReload.cancel()
        executorRegular.cancel()
        if (!killProcesses) return
        // Only touched when checks are actually being torn down: these sets are what
        // drive the floating button's spinner (see setButtonState), and the jobs/processes
        // left alive on the killProcesses=false path are still legitimately populating
        // them — clearing it out from under still-running work stops the spinner while
        // detection is still genuinely in progress, then the button jumps straight to
        // "detected" once that preserved job finally lands.
        _uiState.update { it.copy(regularLoading = emptySet(), m3u8Loading = emptySet()) }
        verifyVideoLinkJobStorage.keys.toList().forEach { taskId ->
            videoServiceLocal.cancelExtraction(taskId)
        }
        verifyVideoLinkJobStorage.values.toList().forEach { process ->
            process.cancel()
        }
        verifyVideoLinkJobStorage.clear()
    }

    /**
     * [m3u8Loading]/[regularLoading] used to be `ObservableField`s with a property-changed
     * callback that re-ran [setButtonState] whenever either became non-empty; these two
     * helpers replicate that trigger explicitly now that the sets live in immutable state.
     */
    /**
     * The transform and the [setButtonState] trigger both need to see the same, up-to-date
     * set: applying [transform] inside [MutableStateFlow.update]'s closure (rather than
     * reading [MutableStateFlow.value] once before writing) keeps the read-modify-write
     * atomic under the CAS retry loop `update` already runs, so two concurrent callers (this
     * runs from up to a dozen+ threads — WebView's own request threads plus the video-service
     * pool) can no longer race and silently drop one another's addition/removal.
     */
    private fun updateM3u8Loading(transform: (Set<String>) -> Set<String>) {
        var resultingSet: Set<String> = emptySet()
        _uiState.update {
            val updated = transform(it.m3u8Loading)
            resultingSet = updated
            it.copy(m3u8Loading = updated)
        }
        if (resultingSet.isNotEmpty()) setButtonState(DownloadButtonStateCanNotDownload())
    }

    private fun updateRegularLoading(transform: (Set<String>) -> Set<String>) {
        var resultingSet: Set<String> = emptySet()
        _uiState.update {
            val updated = transform(it.regularLoading)
            resultingSet = updated
            it.copy(regularLoading = updated)
        }
        if (resultingSet.isNotEmpty()) setButtonState(DownloadButtonStateCanNotDownload())
    }

    /**
     * Derives and commits the new button state from whatever [DetectedVideosContract.State]
     * is actually current at write time. The branch decision used to be computed from a
     * [MutableStateFlow.value] snapshot taken *before* calling [MutableStateFlow.update],
     * which let concurrent callers commit a decision based on a state that was already stale
     * by the time it landed — the button would then flap between Loading/CanNotDownload as
     * later, fresher decisions got overwritten by earlier, staler ones still in flight.
     * Computing the decision inside the `update` closure itself removes that race: the CAS
     * retry loop reruns this block against the real current state on every contended retry.
     */
    private fun setButtonState(state: DownloadButtonState) {
        _uiState.update { current ->
            val newButtonState = when (state) {
                is DownloadButtonStateCanDownload -> state

                is DownloadButtonStateCanNotDownload -> {
                    if (current.detectedVideos.isEmpty()) {
                        val impEl = current.regularLoading.find { it.contains(".mp4") }
                        if (current.m3u8Loading.isNotEmpty() ||
                            (current.m3u8Loading.isEmpty() && impEl != null)
                        ) {
                            DownloadButtonStateLoading()
                        } else {
                            DownloadButtonStateCanNotDownload()
                        }
                    } else {
                        DownloadButtonStateCanDownload(current.detectedVideos.first())
                    }
                }

                is DownloadButtonStateLoading -> {
                    if (current.detectedVideos.isEmpty()) {
                        DownloadButtonStateLoading()
                    } else {
                        DownloadButtonStateCanDownload(current.detectedVideos.first())
                    }
                }

                else -> state
            }
            current.copy(downloadButtonState = newButtonState)
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
                    val emptyResType = emptyRes.body.contentType().toString()
                    if ((emptyResType.contains("video") || emptyResType.contains("audio")) &&
                        length > treshold
                    ) {
                        setVideoInfoWrapperFromUrl(
                            finlUrlPairEmpty.first,
                            webTabModel?.uiState?.value?.tabUrl,
                            finlUrlPairEmpty.second.toMap(),
                            length,
                            emptyResType
                        )
                        emptyRes.close()

                        return
                    }
                }
            }

            val isTikTok = url.contains(".tiktok.com/")
            val typeStr = type.toString()
            if ((typeStr.contains("video") || typeStr.contains("audio")) &&
                (length > treshold || (isTikTok && length > 1024 * 1024 / 3))
            ) {
                setVideoInfoWrapperFromUrl(
                    finlUrlPair.first,
                    webTabModel?.uiState?.value?.tabUrl,
                    finlUrlPair.second.toMap(),
                    length,
                    typeStr
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
        contentLength: Long,
        contentType: String? = null,
    ) {
        Timber.d("setVideoInfoWrapperFromUrl: url=$url contentLength=$contentLength contentType=$contentType")
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

            // Extract format/resolution label from URL or fallback to file size
            val formatLabel = inferFormatLabel(url.toString(), alternativeHeaders, contentLength)

            val isAudio = contentType?.contains("audio", ignoreCase = true) == true
            val ext = if (isAudio) inferAudioExt(contentType, url.toString()) else "mp4"

            val video = VideoInfoWrapper(
                VideoInfo(
                    downloadUrls = downloadUrls,
                    title = webTabModel?.uiState?.value?.currentTitle ?: "no_title",
                    ext = ext,
                    originalUrl = webTabModel?.uiState?.value?.tabUrl ?: "",
                    formats = VideFormatEntityList(
                        mutableListOf(
                            VideoFormatEntity(
                                formatId = "0",
                                format = formatLabel,
                                ext = ext,
                                url = downloadUrls.first().url.toString(),
                                httpHeaders = downloadUrls.first().headers.toMap(),
                                fileSize = contentLength,
                                vcodec = if (isAudio) "none" else null,
                                acodec = if (isAudio) "aac" else null,
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

    /**
     * Best-effort audio container guess for a regular (non-yt-dlp) download: the URL's own
     * extension is more reliable than the server's Content-Type (many hosts just say
     * "audio/mpeg" for anything), so it's tried first.
     */
    private fun inferAudioExt(contentType: String?, url: String): String {
        val urlExt = url.substringBefore("?").substringAfterLast('.', "").lowercase()
        val knownAudioExts = setOf("mp3", "m4a", "aac", "wav", "flac", "opus", "wma", "oga", "ogg")
        if (urlExt in knownAudioExts) return urlExt

        val type = contentType.orEmpty().lowercase()
        return when {
            type.contains("mpeg") -> "mp3"
            type.contains("mp4") || type.contains("m4a") -> "m4a"
            type.contains("aac") -> "aac"
            type.contains("wav") -> "wav"
            type.contains("flac") -> "flac"
            type.contains("ogg") -> "ogg"
            type.contains("webm") -> "weba"
            else -> "mp3"
        }
    }

    /**
     * Infer a meaningful format label for a regular MP4 download. Since we only have the
     * HTTP response (Content-Length, headers) and no format metadata, try these in order:
     * 1. Extract resolution from URL (e.g., "video_720p.mp4" → "720P")
     * 2. Extract from Content-Disposition filename (e.g., "video-1080p-final.mp4")
     * 3. Infer from file size (larger files → HD, smaller → SD)
     * 4. Fallback to generic "MP4"
     *
     * GetVideoFormatOptionsUseCase.shortLabel() will later parse this to display a clean label.
     */
    private fun inferFormatLabel(
        urlString: String,
        headers: Map<String, String>,
        fileSize: Long
    ): String {
        // Try 1: Extract resolution from URL path/query
        val urlFormat = extractResolutionFromUrl(urlString)
        if (urlFormat.isNotEmpty()) {
            return urlFormat
        }

        // Try 2: Extract from Content-Disposition filename
        val contentDisposition = headers["Content-Disposition"] ?: headers["content-disposition"]
        if (!contentDisposition.isNullOrEmpty()) {
            val filenameFormat = extractResolutionFromUrl(contentDisposition)
            if (filenameFormat.isNotEmpty()) {
                return filenameFormat
            }
        }

        // Try 3: Infer from file size (rough heuristic)
        val sizeFormat = inferResolutionFromSize(fileSize)
        if (sizeFormat.isNotEmpty()) {
            return sizeFormat
        }

        // Try 4: Generic fallback
        return "MP4"
    }

    /** Extract resolution pattern like "720p", "1080p", "480p" from URL or filename. */
    private fun extractResolutionFromUrl(text: String): String {
        val resolutionPattern = Regex("\\b(\\d{3,4})[pP]\\b|([4k]{2}|2k|hd|sd|uhd)", RegexOption.IGNORE_CASE)
        val match = resolutionPattern.find(text)
        return match?.groupValues?.firstOrNull { it.isNotEmpty() } ?: ""
    }

    /** Rough heuristic: file size >= 100MB → HD, else SD. */
    private fun inferResolutionFromSize(fileSize: Long): String {
        return when {
            fileSize >= 200 * 1024 * 1024 -> "1080P"  // >= 200MB
            fileSize >= 100 * 1024 * 1024 -> "720P"   // >= 100MB
            fileSize >= 30 * 1024 * 1024 -> "480P"    // >= 30MB
            else -> ""
        }
    }
}
