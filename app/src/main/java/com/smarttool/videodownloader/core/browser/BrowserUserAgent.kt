package com.smarttool.videodownloader.core.browser

/**
 * User-agent the in-app WebViews present. Sites gate video markup on this, so the
 * detection pipeline and the browser must send the same string.
 *
 * Previously a `var` on `BrowserFragment`'s companion; moved here when that screen
 * became Compose, since it is shared by the WebView machinery rather than owned by
 * any one screen.
 */
object BrowserUserAgent {
    // TODO different agents for different androids
    const val MOBILE =
        "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/92.0.4515.131 Mobile Safari/537.36"
}
