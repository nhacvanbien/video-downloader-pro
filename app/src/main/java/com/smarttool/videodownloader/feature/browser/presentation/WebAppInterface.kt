package com.smarttool.videodownloader.feature.browser.presentation

import android.webkit.JavascriptInterface

/**
 * The `window.Android` bridge the injected download buttons call into.
 *
 * It takes the [DownloadImageHandler] directly rather than casting a `Context` to it:
 * the browser is no longer an Activity, so there is nothing in the context chain to
 * cast.
 */
class WebAppInterface(private val handler: DownloadImageHandler) {

    @JavascriptInterface
    fun downloadImageUpdate(imageUrl: String) {
        if (imageUrl.isNotEmpty()) {
            handler.onDownloadImageRequested(imageUrl)
        }
    }
}
