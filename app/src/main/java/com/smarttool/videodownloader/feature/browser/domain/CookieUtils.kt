package com.smarttool.videodownloader.feature.browser.domain

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import com.yausername.youtubedl_android.YoutubeDLRequest
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


object CookieUtils {

    fun webRequestToHttpWithCookies(request: WebResourceRequest): Request? {
        val url = request.url.toString()

        val tmpHeaders = request.requestHeaders
        tmpHeaders["Cookie"] = try {
            CookieManager.getInstance().getCookie(url) ?: CookieManager.getInstance()
                .getCookie(url) ?: ""
        } catch (e: Throwable) {
            ""
        }
        val verReq = try {
            Request.Builder().headers(tmpHeaders.toHeaders()).url(url).build()
        } catch (e: Throwable) {
            null
        }

        return verReq
    }

    fun addCookiesToRequest(
        context: Context,
        url: String,
        request: YoutubeDLRequest,
        additionalUrl: String? = null
    ): File {
        val cookieFile = createTmpCookieFile(context, url.hashCode().toString())
        val cookies = cookiesForUrlAsNetscape(url, additionalUrl)

        if (cookieFile.exists() && cookieFile.isFile && cookies.isNotEmpty()) {
            cookieFile.writeText(cookies)
            request.addOption("--cookies", cookieFile.path)
        }

        return cookieFile
    }

    /**
     * Reads cookies via the WebView's own CookieManager (which transparently handles
     * decryption) instead of parsing the raw, encrypted app_webview Cookies sqlite db.
     * Works the same way on every Android version, unlike the previous approach which
     * relied on desktop-Chrome-only cookie extraction (`--cookies-from-browser`) on
     * API 33+ and on reading undecrypted cookie bytes on older versions.
     */
    private fun cookiesForUrlAsNetscape(url: String, additionalUrl: String?): String {
        var rawCookies = try {
            CookieManager.getInstance().getCookie(url)
        } catch (e: Throwable) {
            null
        }

        var cookieUrl = url
        if (rawCookies.isNullOrBlank() && additionalUrl != null) {
            rawCookies = try {
                CookieManager.getInstance().getCookie(additionalUrl)
            } catch (e: Throwable) {
                null
            }
            cookieUrl = additionalUrl
        }

        if (rawCookies.isNullOrBlank()) {
            return ""
        }

        val host = try {
            Uri.parse(cookieUrl).host
        } catch (e: Throwable) {
            null
        } ?: return ""

        val domain = if (host.startsWith(".")) host else ".$host"
        val isSecure = cookieUrl.startsWith("https", ignoreCase = true)

        val lines = mutableListOf(
            "# Netscape HTTP Cookie File",
            "# https://curl.haxx.se/rfc/cookie_spec.html",
            "# This is a generated file! Do not edit.",
            ""
        )

        rawCookies.split(";").forEach { pair ->
            val separatorIndex = pair.indexOf("=")
            if (separatorIndex <= 0) return@forEach

            val name = pair.substring(0, separatorIndex).trim()
            val value = pair.substring(separatorIndex + 1).trim()
            if (name.isEmpty()) return@forEach

            lines.add("$domain\tTRUE\t/\t${if (isSecure) "TRUE" else "FALSE"}\t2147483647\t$name\t$value")
        }

        return lines.joinToString("\n")
    }

    fun getFinalRedirectURL(url: URL, headers: Map<String, String>): Pair<URL, Headers>? {
        val currentHeaders = headers.toMutableMap()

        try {
            val con = url.openConnection() as HttpURLConnection
            con.instanceFollowRedirects = false
            for (header in currentHeaders) con.setRequestProperty(header.key, header.value)
            try {
                con.connect()
            } catch (_: Throwable) {

            }
            val resCode = con.responseCode
            if (resCode == HttpURLConnection.HTTP_SEE_OTHER || resCode == HttpURLConnection.HTTP_MOVED_PERM || resCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                var location = con.getHeaderField("Location")

                val origin = con.getHeaderField("Access-Control-Allow-Origin")

                if (location.startsWith("/")) {
                    location = if (location.startsWith("//")) {
                        url.protocol + "://" + location.replace("//", "")
                    } else {
                        url.protocol + "://" + url.host + location
                    }
                }
                if (origin != null) {
                    currentHeaders["Referer"] = origin
                }
                return getFinalRedirectURL(URL(location), currentHeaders)
            }
        } catch (_: Exception) {

        }

        return Pair(url, currentHeaders.toHeaders())
    }

    private fun createTmpCookieFile(context: Context, name: String): File {
        val file = File("${context.cacheDir}/$name")
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()

        return file
    }
}
