package com.smarttool.videodownloader.feature.browser.domain.model

/** [urlTemplate] takes one `%s` — the URL-encoded query. */
enum class SearchEngine(val id: String, val urlTemplate: String) {
    GOOGLE("google", "https://www.google.com/search?q=%s"),
    BING("bing", "https://www.bing.com/search?q=%s"),
    YAHOO("yahoo", "https://search.yahoo.com/search?p=%s"),
    DUCK_DUCK_GO("duckduckgo", "https://duckduckgo.com/?q=%s"),
    ;

    companion object {
        fun fromId(id: String): SearchEngine = entries.firstOrNull { it.id == id } ?: GOOGLE
    }
}
