package com.smarttool.videodownloader.core.navigation

import android.net.Uri

/**
 * Every destination in the single-Activity graph.
 *
 * Path arguments are URL-encoded by the `…()` builders, so callers pass raw values
 * (page URLs, file paths, JSON header blobs) and never have to escape them. Optional
 * arguments use query syntax because Navigation cannot express an absent path segment.
 */
object AppRoute {

    const val MAIN = "main"

    const val TABS = "tabs"

    const val PRIVATE_VIDEO = "privateVideo"

    const val SELECT_VIDEO = "selectVideo"

    const val INTRO = "intro"

    const val PERMISSION = "permission"

    const val DISCLAIMERS = "disclaimers"

    const val NATIVE_FULL = "nativeFull"

    const val ARG_OPEN = "open"
    const val GUIDE = "guide/{$ARG_OPEN}"

    const val ARG_MODE = "mode"
    const val HISTORY = "history/{$ARG_MODE}"

    const val ARG_URL = "url"
    const val WEB_TAB = "webTab/{$ARG_URL}"

    const val ARG_ACTION = "action"
    const val PIN = "pin/{$ARG_ACTION}"

    const val ARG_STATUS = "status"
    const val ARG_PASS = "pass"
    const val SECURITY = "security/{$ARG_STATUS}/{$ARG_PASS}"

    const val ARG_FROM_SPLASH = "fromSplash"
    const val LANGUAGE = "language/{$ARG_FROM_SPLASH}"

    const val ARG_NAME = "name"
    const val ARG_ICON = "icon"
    const val LANGUAGE_APPLIED = "languageApplied/{$ARG_NAME}/{$ARG_ICON}"

    const val ARG_TITLE = "title"
    const val ARG_TYPE = "type"
    const val ARG_DOWNLOADED = "downloaded"
    const val ARG_HEADERS = "headers"
    const val PLAY_MEDIA =
        "playMedia/{$ARG_URL}?$ARG_TITLE={$ARG_TITLE}&$ARG_TYPE={$ARG_TYPE}" +
            "&$ARG_DOWNLOADED={$ARG_DOWNLOADED}&$ARG_HEADERS={$ARG_HEADERS}"

    /** [open] tells [GUIDE] which entry point it was reached from ("home"/"process"). */
    fun guide(open: String): String = "guide/${encode(open)}"

    /** [mode] is `bookmark` or `history`; anything else falls back to history. */
    fun history(mode: String): String = "history/${encode(mode)}"

    fun webTab(url: String): String = "webTab/${encode(url)}"

    fun pin(action: String = ""): String = "pin/${encode(action)}"

    fun security(status: String = "", pass: String = ""): String =
        "security/${encode(status)}/${encode(pass)}"

    fun language(fromSplash: Boolean): String = "language/$fromSplash"

    fun languageApplied(name: String, iconRes: Int): String =
        "languageApplied/${encode(name)}/$iconRes"

    fun playMedia(
        url: String,
        title: String = "",
        type: String = "video",
        isDownloaded: Boolean = false,
        headers: String = "",
    ): String = "playMedia/${encode(url)}" +
        "?$ARG_TITLE=${encode(title)}" +
        "&$ARG_TYPE=${encode(type)}" +
        "&$ARG_DOWNLOADED=$isDownloaded" +
        "&$ARG_HEADERS=${encode(headers)}"

    /**
     * Blank values would collapse a path segment and make the route unmatchable, so
     * they are carried as a placeholder and mapped back to "" on the reading side.
     */
    const val EMPTY = "-"

    fun encode(value: String): String =
        if (value.isEmpty()) EMPTY else Uri.encode(value)

    fun decode(value: String?): String =
        if (value == null || value == EMPTY) "" else Uri.decode(value)
}
