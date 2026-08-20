package com.smarttool.videodownloader.feature.browser.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonState
import com.smarttool.videodownloader.feature.browser.domain.model.DownloadButtonStateCanNotDownload
import okhttp3.Request

interface DetectedVideosContract {
    data class State(
        val downloadButtonState: DownloadButtonState = DownloadButtonStateCanNotDownload(),
        val detectedVideos: Set<VideoInfo> = emptySet(),
        val selectedFormats: Map<String, String> = emptyMap(),
        val formatTitles: Map<String, String> = emptyMap(),
        val m3u8Loading: Set<String> = emptySet(),
        val regularLoading: Set<String> = emptySet(),
        /** True once the current page has already had one manual retry (see [DetectedVideosContract.Event.ShowVideoInfo]) — a further tap while still empty shows "no media found" instead of retrying forever. */
        val retryAttempted: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        /**
         * @param isRetry True when this probe is a manual retry re-run (see
         * [DetectedVideosTabViewModel.showVideoInfo]) rather than a genuine navigation —
         * keeps [State.retryAttempted] from being reset back to false by the very probe
         * it triggered, which would otherwise let a manual retry loop forever.
         */
        data class StartPage(val url: String, val userAgent: String, val isRetry: Boolean = false) : Event
        data object ShowVideoInfo : Event
        data class VerifyLinkStatus(
            val request: Request,
            val hlsTitle: String? = null,
            val isM3u8: Boolean = false,
        ) : Event

        data object CancelAllChecks : Event
        data class SelectFormat(val videoId: String, val format: String) : Event
        data class RenameTitle(val videoId: String, val title: String) : Event

        /** The page just changed to something unprobeable; drop back to "nothing found". */
        data object MarkCanNotDownload : Event
    }

    sealed interface Effect : UiEffect {
        data object ShowDetectedVideos : Effect
        data class VideoPushed(val videoInfo: VideoInfo) : Effect
        data class LoginRequired(val host: String) : Effect

        /** The page being probed belongs to a platform we deliberately don't support (e.g. YouTube). */
        data object PlatformNotAllowed : Effect

        /**
         * A manual retry (first tap on the download button while nothing's been found)
         * needs the WebView host itself to reload the page, not just re-run yt-dlp against
         * the already-loaded URL: the sniffer that actually finds m3u8/mp4 requests only
         * sees them as the page makes them, and those already fired (or didn't) on the
         * first load. The host owns the WebView, so it's the one that has to act on this.
         */
        data object RequestReloadForRetry : Effect
    }
}
