package com.smarttool.videodownloader.feature.browser.domain.model

/** Media container a detected URL resolves to, used to pick the download engine. */
enum class ContentType {
    M3U8,
    MPD,
    MP4,
    AUDIO,
    OTHER,
}
