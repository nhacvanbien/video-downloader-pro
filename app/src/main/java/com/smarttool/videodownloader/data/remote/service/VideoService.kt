package com.smarttool.videodownloader.data.remote.service

import android.content.Context
import com.google.common.net.InternetDomainName
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.network.Proxy
import com.smarttool.videodownloader.core.network.TikTokExtractionSupport
import com.smarttool.videodownloader.data.model.VideoInfoWrapper
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoFormatEntity
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo as YoutubeDlVideoInfo
import okhttp3.Request
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.UUID

/**
 * Thrown when yt-dlp reports that the target site requires the user to be logged-in
 * (e.g. Vimeo private/impersonation-gated videos) before the info/formats can be extracted.
 */
class LoginRequiredException(val host: String, message: String?) : Exception(message)

open class VideoServiceLocal(
    private val proxyController: CustomProxyController,
    private val helper: YoutubedlHelper,
    private val appContext: Context,
    private val preferences: AppPreferencesDataSource,
) {
    companion object {
        const val MP4_EXT = "mp4"
        private const val FACEBOOK_HOST = ".facebook."
        private const val COOKIE_HEADER = "Cookie"
        private val LOGIN_REQUIRED_PATTERNS = listOf(
            "logged-in", "logged in", "log in to", "sign in to",
            "--cookies", "provide account credentials", "login required"
        )

        private fun isLoginRequiredError(message: String?): Boolean {
            if (message == null) return false
            val lower = message.lowercase()
            return LOGIN_REQUIRED_PATTERNS.any { lower.contains(it) }
        }
    }

    /**
     * Destroys the OS process backing an in-flight [getVideoInfo] call, keyed by the same
     * [taskId] passed to it. `getInfo(request)` (the library's own convenience wrapper)
     * calls `execute(request, processId = null, ...)`, which leaves the underlying yt-dlp
     * subprocess with no id the app can ever reference again — cancelling the coroutine
     * that's waiting on it (e.g. because the user backed out of the browser) only stops
     * the *Kotlin* side; the process (and, for TikTok, its up-to-45s retry-with-backoff
     * loop) keeps running to completion regardless. Calling [getVideoInfo] with an explicit
     * taskId and destroying that same id here is what actually makes it stoppable — the
     * download flow ([YoutubeDlDownloaderWorker]) already does this same thing.
     */
    fun cancelExtraction(taskId: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(taskId) }
    }

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean, taskId: String = UUID.randomUUID().toString()): VideoInfoWrapper? {
        Timber.d("getVideoInfo: url=${url.url} isM3u8OrMpd=$isM3u8OrMpd hasCookie=${url.header("Cookie") != null}")

        var result: VideoInfoWrapper? = null

        try {
            result = handleYoutubeDlUrl(url, isM3u8OrMpd, taskId)
        } catch (e: Throwable) {
            Timber.e(e, "youtube-dl failed for ${url.url}")
            if (isLoginRequiredError(e.message)) {
                throw LoginRequiredException(url.url.host ?: "", e.message)
            }
        }

        return result
    }

    /** Mirrors `YoutubeDL.getInfo(request)` exactly, but through the cancellable, taskId-tracked `execute` overload instead of the fire-and-forget one `getInfo` calls internally. */
    private fun fetchVideoInfo(request: YoutubeDLRequest, taskId: String): YoutubeDlVideoInfo {
        request.addOption("--dump-json")
        val response = YoutubeDL.getInstance().execute(request, taskId, null)
        val videoInfo = try {
            YoutubeDL.getInstance().objectMapper.readValue(response.out, YoutubeDlVideoInfo::class.java)
        } catch (e: IOException) {
            throw YoutubeDLException("Unable to parse video information", e)
        }
        return videoInfo ?: throw YoutubeDLException("Failed to fetch video information")
    }

    private fun handleYoutubeDlUrl(url: Request, isM3u8OrMpd: Boolean = false, taskId: String): VideoInfoWrapper {
        if (!isM3u8OrMpd && !isYotubeDlSupportedHost(url.url.host)) {
            throw Throwable("host not in supported list")
        }
        val request = YoutubeDLRequest(url.url.toString())

        // TikTok's WAF appears to flag the mismatch between a full set of Chromium
        // navigation headers (Sec-Fetch-*, X-Requested-With, Upgrade-Insecure-Requests —
        // whatever the sniffing WebView sent) and a request that isn't actually coming
        // from a browser (no TLS impersonation available on this yt-dlp build). Sending
        // yt-dlp's own bare, unmodified request — no forwarded headers at all — matches
        // what a plain `yt-dlp <url>` sends and is what actually gets through.
        if (!TikTokExtractionSupport.isTikTokHost(url.url.host)) {
            url.headers.forEach { (name, value) ->
                if (name != COOKIE_HEADER) {
                    request.addOption("--add-header", "$name:${value}")

                    if (url.url.host?.contains("facebook.com") == true) {
                        request.addOption(
                            "--add-header",
                            "User-Agent:Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                        )
                        request.addOption("--add-header", "Accept-Language:en-US,en;q=0.9")
                        request.addOption("--add-header", "Referer:https://m.facebook.com/")
                    }

                }
            }
        }

        val currentProxy = proxyController.getCurrentRunningProxy()
        if (currentProxy != Proxy.noProxy()) {
            attachProxyToRequest(request, currentProxy)
        }

        val tmpCookieFile = CookieUtils.addCookiesToRequest(appContext, url.url.toString(), request)

        try {
            val info = TikTokExtractionSupport.retryTikTokExtraction(url.url.host) { attempt ->
                if (attempt > 1 && TikTokExtractionSupport.isTikTokHost(url.url.host)) {
                    request.addOption(
                        "--add-header",
                        "User-Agent:${TikTokExtractionSupport.randomUserAgent()}"
                    )
                }
                fetchVideoInfo(request, taskId)
            }
            val formats = info.formats?.map {
                videoEntityFromFormat(
                    it
                )
            }
            val filtered = arrayListOf<VideoFormatEntity>()

            if (url.url.toString().contains(FACEBOOK_HOST)) {
                if (formats != null) {
                    filtered.addAll(formats.filter {
                        it.formatId?.lowercase(Locale.ROOT)?.contains(Regex("hd|sd")) == true
                    })
                }
            }

            val videoFormats = filtered.ifEmpty { formats?.filterNot { it.isAudioOnly } ?: emptyList() }

            // Surface the best audio-only variant as a single "Audio" chip alongside the
            // video qualities instead of discarding every audio-only format outright —
            // GetVideoFormatOptionsUseCase collapses it into the AUDIO_LABEL chip.
            val bestAudioFormat = formats?.filter { it.isAudioOnly }?.maxByOrNull { it.abr }

            val listFormats = VideFormatEntityList(videoFormats + listOfNotNull(bestAudioFormat))

            if (listFormats.formats.isEmpty()) throw Exception("Audio Only Detected")

            return VideoInfoWrapper(VideoInfo(title = info.title ?: "no title").also { videoInfo ->
                videoInfo.ext = info.ext ?: MP4_EXT
                videoInfo.thumbnail = info.thumbnail ?: ""
                videoInfo.duration = info.duration.toLong()
                videoInfo.originalUrl = url.url.toString()
                videoInfo.downloadUrls = if (isM3u8OrMpd) emptyList() else listOf(url)
                videoInfo.formats = listFormats
                videoInfo.isRegularDownload = false
            })
        } catch (e: Throwable) {
            throw e
        } finally {
            tmpCookieFile?.delete()
        }
    }

    private fun attachProxyToRequest(request: YoutubeDLRequest, currentProxy: Proxy) {
        val user = proxyController.getProxyCredentials().first
        val password = proxyController.getProxyCredentials().second
        if (user.isNotEmpty() && password.isNotEmpty()) {
            request.addOption(
                "--proxy", "http://${user}:${password}@${currentProxy.host}:${currentProxy.port}"
            )
        } else {
            request.addOption(
                "--proxy", "${currentProxy.host}:${currentProxy.port}"
            )
        }
    }

    private fun isYotubeDlSupportedHost(host: String): Boolean {
        return helper.isHostSupported(host)
    }

    private fun videoEntityFromFormat(videoFormat: VideoFormat): VideoFormatEntity {
        return VideoFormatEntity(
            asr = videoFormat.asr,
            tbr = videoFormat.tbr,
            abr = videoFormat.abr,
            format = videoFormat.format,
            formatId = videoFormat.formatId,
            formatNote = videoFormat.formatNote,
            ext = videoFormat.ext,
            preference = videoFormat.preference,
            vcodec = videoFormat.vcodec,
            acodec = videoFormat.acodec,
            width = videoFormat.width,
            height = videoFormat.height,
            fileSize = videoFormat.fileSize,
            fileSizeApproximate = videoFormat.fileSizeApproximate,
            fps = videoFormat.fps,
            url = videoFormat.url,
            manifestUrl = videoFormat.manifestUrl,
            httpHeaders = videoFormat.httpHeaders
        )
    }
}

class YoutubedlHelper  constructor(
    private val okHttpProxyClient: OkHttpProxyClient,
    private val preferences: AppPreferencesDataSource,
) {
    companion object {
        private const val SUPPORTED_SITES_URL =
            "https://ytb-dl.github.io/ytb-dl/supportedsites.html"
    }

    private val sites: HashSet<String> = HashSet()
    private var isLoading = false

    fun isHostSupported(host: String): Boolean {
        val isCheck = preferences.isCheckHostByListBlocking()

        if (!isCheck) {
            return true
        }

        if (sites.isEmpty() || isLoading) {
            try {
                loadFromAssets()
            } catch (e: Throwable) {
                Timber.e(e, "loadFromAssets failed")
                isLoading = false
            }

            return true
        }

        return try {
            val domainName: InternetDomainName = InternetDomainName.from(host).topPrivateDomain()
            val fixedName = domainName.toString().replace(Regex("\\.\\w{2,}$"), "")

            sites.contains(fixedName) || sites.contains("${fixedName}.com")
        } catch (e: Exception) {
            true
        }
    }

    private fun loadFromAssets() {
        if (!isLoading) {
            isLoading = true

            val response = okHttpProxyClient.getProxyOkHttpClient().newCall(
                Request.Builder().url(SUPPORTED_SITES_URL).build()
            ).execute()
            val doc = Jsoup.parse(response.body.string())
            response.body.close()
            val sitesB = doc.select("li > b")

            for (b in sitesB) {
                val value =
                    b.text().trim().split(":").first().trim().lowercase().replace("- **", "")
                        .replace("**", "").trim()
                sites.add(value)
            }
            isLoading = false
        }
    }
}
