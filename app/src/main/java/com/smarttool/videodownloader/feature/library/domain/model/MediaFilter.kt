package com.smarttool.videodownloader.feature.library.domain.model

/** Which slice of the downloaded library a query covers. */
enum class MediaFilter(val typeValue: String) {
    All("all"),
    Video("video"),
    Image("image"),
}
