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
    ) : UiState

    sealed interface Event : UiEvent {
        data class StartPage(val url: String, val userAgent: String) : Event
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
        data object VideoPushed : Effect
        data class LoginRequired(val host: String) : Effect
    }
}
