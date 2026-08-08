package com.smarttool.videodownloader.data.remote.service

import com.smarttool.videodownloader.data.model.VideoInfoWrapper
import com.smarttool.videodownloader.data.network.entity.VideFormatEntityList
import com.smarttool.videodownloader.data.network.entity.VideoFormatEntity
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.core.network.Proxy
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.google.common.net.InternetDomainName
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.*
import timber.log.Timber

interface VideoService {
    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean = false): VideoInfoWrapper?
}

/**
 * Thrown when yt-dlp reports that the target site requires the user to be logged-in
 * (e.g. Vimeo private/impersonation-gated videos) before the info/formats can be extracted.
 */
class LoginRequiredException(val host: String, message: String?) : Exception(message)

open class VideoServiceLocal(
    private val proxyController: CustomProxyController, private val helper: YoutubedlHelper
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

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfoWrapper? {
        Timber.d("getVideoInfo: url=${url.url} isM3u8OrMpd=$isM3u8OrMpd hasCookie=${url.header("Cookie") != null}")

        var result: VideoInfoWrapper? = null

        try {
            result = handleYoutubeDlUrl(url, isM3u8OrMpd)
        } catch (e: Throwable) {
            Timber.e(e, "youtube-dl failed for ${url.url}")
            if (isLoginRequiredError(e.message)) {
                throw LoginRequiredException(url.url.host ?: "", e.message)
            }
        }

        return result
    }

    private fun handleYoutubeDlUrl(url: Request, isM3u8OrMpd: Boolean = false): VideoInfoWrapper {
        if (!isM3u8OrMpd && !isYotubeDlSupportedHost(url.url.host)) {
            throw Throwable("host not in supported list")
        }
        val request = YoutubeDLRequest(url.url.toString())
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

        val currentProxy = proxyController.getCurrentRunningProxy()
        if (currentProxy != Proxy.noProxy()) {
            attachProxyToRequest(request, currentProxy)
        }

        val tmpCookieFile = CookieUtils.addCookiesToRequest(url.url.toString(), request)

        try {
            val info = YoutubeDL.getInstance().getInfo(request)
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

            val listFormats =
                VideFormatEntityList(filtered.ifEmpty { formats?.filter { !(it.acodec != "none" && it.vcodec == "none") } }
                    ?: emptyList())

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
            tmpCookieFile.delete()
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
    private val sharedPrefHelper: PreferenceHelper
) {
    companion object {
        private const val SUPPORTED_SITES_URL =
            "https://ytb-dl.github.io/ytb-dl/supportedsites.html"
    }

    private val sites: HashSet<String> = HashSet()
    private var isLoading = false

    fun isHostSupported(host: String): Boolean {
        val isCheck = sharedPrefHelper.getIsCheckByList()

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
