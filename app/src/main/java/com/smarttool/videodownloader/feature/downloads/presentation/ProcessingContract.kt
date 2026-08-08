package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo

interface ProcessingContract {
    data class State(val url: String = "") : UiState

    sealed interface Event : UiEvent {
        data class UrlChange(val url: String) : Event
        data class Start(val videoInfo: VideoInfo) : Event
        data class Pause(val progressInfo: ProgressInfo) : Event
        data class Resume(val progressInfo: ProgressInfo) : Event
        data class Cancel(val progressInfo: ProgressInfo, val removeFile: Boolean) : Event
        data class StopAndSave(val progressInfo: ProgressInfo) : Event
    }

    sealed interface Effect : UiEffect {
        /** The pasted/typed text is a usable link — the probe WebView should load it. */
        data class LoadUrl(val url: String) : Effect

        /** Anything else — nothing to probe, so the download button resets. */
        data object ResetDetection : Effect
    }
}
