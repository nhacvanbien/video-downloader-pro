package com.smarttool.videodownloader.feature.browser.domain

/** The browser preferences the in-page WebView flow itself reads. */
interface BrowserPreferencesRepository {
    suspend fun isShowVideoAlert(): Boolean

    suspend fun setShowVideoAlert(show: Boolean)

    suspend fun isLockPortrait(): Boolean

    /** Bytes a response's `Content-Length` must clear to be treated as a video. */
    suspend fun videoDetectionThreshold(): Int
}
