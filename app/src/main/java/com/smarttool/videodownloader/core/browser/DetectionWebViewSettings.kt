package com.smarttool.videodownloader.core.browser

import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * The settings every video-detection WebView needs.
 *
 * Detection works by listening to what the page fetches, so the page has to be able to
 * fetch: mixed content is allowed through the proxy, JavaScript and DOM storage are on
 * because players are scripts, and the cache is off so a revisit re-issues the requests
 * instead of replaying them from disk where we would never see them.
 *
 * [allowAutoplay] is the one real difference between the hosts — the browser lets the
 * page play, the off-screen probe must not.
 */
fun WebView.applyDetectionDefaults(allowAutoplay: Boolean) {
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    isScrollbarFadingEnabled = true

    settings.apply {
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
        userAgentString = BrowserUserAgent.MOBILE
        mediaPlaybackRequiresUserGesture = !allowAutoplay
    }
}
