package com.smarttool.videodownloader.feature.browser.domain

import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import okhttp3.Headers
import okhttp3.Request
import timber.log.Timber

class VideoUtils {
    companion object {

        fun getContentTypeByUrl(
            url: String,
            headers: Headers?,
            okHttpProxyClient: OkHttpProxyClient
        ): ContentType {
            if (url.contains(".js") || url.contains(".css") || url.startsWith("blob")) {
                return ContentType.OTHER
            }

            // Unambiguous by extension alone -> classify without a network round trip.
            // Deliberately NOT extended to other static extensions (images/fonts/docs/etc.):
            // the "application/octet-stream" branch below exists specifically to catch sites
            // that serve a manifest disguised under an unrelated extension (see the
            // `hentaihaven` .txt case in VideoSniffer.sniff), so skipping the probe by
            // extension for anything beyond these three would defeat that on any extension
            // an obfuscator happens to pick.
            val cleanedUrl = url.substringBefore("?")
            if (cleanedUrl.contains(".mp4")) return ContentType.MP4
            if (cleanedUrl.contains(".m3u8")) return ContentType.M3U8
            if (cleanedUrl.contains(".mpd")) return ContentType.MPD

            val client = okHttpProxyClient.getProxyOkHttpClient()
            val request = Request.Builder()
                .url(url)
                .headers(headers ?: Headers.headersOf())
                .get()
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (e: Throwable) {
                Timber.w(e, "Request failed: ${request.url}")
                null
            }
            val contentTypeStr = response?.header("Content-Type")
            var contentType: ContentType = ContentType.OTHER

            when {
                contentTypeStr?.contains("mpegurl") == true -> {
                    contentType = ContentType.M3U8
                }

                contentTypeStr?.contains("dash") == true -> {
                    contentType = ContentType.MPD
                }

                contentTypeStr?.contains("mp4") == true -> {
                    contentType = ContentType.MP4
                }

                contentTypeStr?.contains("application/octet-stream") == true -> {
                    val chars = CharArray(7)
                    response.body.charStream().read(chars, 0, 7)
                    response.body.charStream().close()
                    response.body.close()
                    val content = chars.toString()
                    if (content.startsWith("#EXTM3U")) {
                        contentType = ContentType.M3U8
                    } else if (content.contains("<MPD")) {
                        contentType = ContentType.MPD
                    }
                }

                else -> {
                    contentType = ContentType.OTHER
                }
            }

            return contentType
        }
    }
}