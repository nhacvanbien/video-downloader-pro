package com.smarttool.videodownloader.feature.browser.presentation

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.feature.browser.domain.AdBlockerHelper
import com.smarttool.videodownloader.feature.browser.domain.CookieUtils
import com.smarttool.videodownloader.feature.browser.domain.VideoUtils
import com.smarttool.videodownloader.feature.browser.domain.model.ContentType
import kotlinx.coroutines.Job
import okhttp3.Request

/**
 * Watches everything a WebView fetches and hands anything that looks like a video to
 * [detector].
 *
 * Both WebView hosts run this. The browser tab detects in the background while the user
 * surfs; the Processing tab loads one pasted link in an off-screen WebView for the same
 * purpose. They differ in what they show, not in how they sniff, so the sniffing lives
 * here and each host owns an instance — sharing one would cross-wire their results.
 */
class VideoSniffer(
    private val detector: DetectedVideosTabViewModel,
    private val tabViewModel: WebTabViewModel,
    private val okHttpProxyClient: OkHttpProxyClient,
) {

    /**
     * Probes of plain video files outlive the request that started them, so they are
     * keyed by the page that was open at the time and cancelled when it changes.
     */
    private val regularProbes: MutableMap<String, List<Job>> = mutableMapOf()
    private var lastProbedPageUrl = ""

    /**
     * Confirmed video/manifest content types already resolved this page, keyed by URL
     * without query string. A page's own script can re-request the same manifest/segment or
     * ambiguous (no-extension) endpoint repeatedly; this skips re-running
     * [VideoUtils.getContentTypeByUrl]'s network probe for one we already know is a video.
     * Only positive results are cached — a probe that came back [ContentType.OTHER] is never
     * stored, since that could be a transient failure (that function swallows request
     * exceptions as OTHER) and we'd rather re-probe a handful of extra times than silently
     * stop detecting a real video. Cleared on navigation in [sniff].
     */
    private val contentTypeCache: MutableMap<String, ContentType> = mutableMapOf()
    private var cachedForPageUrl = ""

    /**
     * Feeds a page request to the detector. Returns the stand-in response that blocks an
     * ad, or null to let the WebView load the request itself.
     */
    fun onPageRequest(request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString().orEmpty()

        if (tabViewModel.isAd(url)) return AdBlockerHelper.createEmptyResource()

        sniff(url, request, probeEveryNonManifest = true)
        return null
    }

    /**
     * Same, for a request the page's service worker made. Those cannot be blocked — the
     * callback has no ad-blocking contract — and only requests already typed as MP4 are
     * probed, because a worker can fan out far more requests than a page does.
     */
    fun onServiceWorkerRequest(request: WebResourceRequest) {
        sniff(request.url.toString(), request, probeEveryNonManifest = false)
    }

    /** Drops every probe still in flight; call it when the host tears its WebView down. */
    @Synchronized
    fun cancelPendingProbes() {
        regularProbes.values.flatten().forEach { it.cancel() }
        regularProbes.clear()
        lastProbedPageUrl = ""
        contentTypeCache.clear()
        cachedForPageUrl = ""
    }

    private fun sniff(
        url: String,
        request: WebResourceRequest?,
        probeEveryNonManifest: Boolean,
    ) {
        resetContentTypeCacheOnNavigation()

        val requestWithCookies = request?.let { resourceRequest ->
            try {
                CookieUtils.webRequestToHttpWithCookies(resourceRequest)
            } catch (e: Throwable) {
                null
            }
        }

        val cacheKey = url.substringBefore("?")
        val contentType = getCachedContentType(cacheKey) ?: VideoUtils.getContentTypeByUrl(
            url,
            requestWithCookies?.headers,
            okHttpProxyClient,
        ).also { putCachedContentType(cacheKey, it) }

        val isManifest = contentType == ContentType.M3U8 ||
            contentType == ContentType.MPD ||
            url.contains(".m3u8") ||
            url.contains(".mpd") ||
            (url.contains(".txt") && url.contains("hentaihaven"))

        if (isManifest) {
            if (requestWithCookies != null) {
                detector.onEvent(
                    DetectedVideosContract.Event.VerifyLinkStatus(
                        requestWithCookies,
                        tabViewModel.uiState.value.currentTitle,
                        true,
                    ),
                )
            }
        } else if (probeEveryNonManifest || contentType == ContentType.MP4 || contentType == ContentType.AUDIO) {
            trackRegularProbe(requestWithCookies)
        }
    }

    @Synchronized
    private fun resetContentTypeCacheOnNavigation() {
        val pageUrl = tabViewModel.uiState.value.tabUrl
        if (pageUrl != cachedForPageUrl) {
            contentTypeCache.clear()
            cachedForPageUrl = pageUrl
        }
    }

    @Synchronized
    private fun getCachedContentType(cacheKey: String): ContentType? = contentTypeCache[cacheKey]

    @Synchronized
    private fun putCachedContentType(cacheKey: String, contentType: ContentType) {
        if (contentType != ContentType.OTHER) {
            contentTypeCache[cacheKey] = contentType
        }
    }

    /**
     * Synchronized because the WebView delivers requests off the main thread while
     * [cancelPendingProbes] runs on it.
     */
    @Synchronized
    private fun trackRegularProbe(requestWithCookies: Request?) {
        val job = detector.checkRegularMp4(requestWithCookies)
        val pageUrl = tabViewModel.uiState.value.tabUrl

        if (pageUrl != lastProbedPageUrl) {
            regularProbes.remove(lastProbedPageUrl)?.forEach { it.cancel() }
            lastProbedPageUrl = pageUrl
        }

        if (job != null) {
            regularProbes[pageUrl] = regularProbes[pageUrl].orEmpty() + job
        }
    }
}
