package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

interface MediaContract {
    data class State(
        val title: String = "",
        val isPlaying: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val speed: Float = 1f,
        val looping: Boolean = false,
        val fillMode: Boolean = false,
        /** True while the device is held in landscape or the user asked for fullscreen. */
        val isFullscreen: Boolean = false,
        /** Only downloaded items expose the detail/more action. */
        val showMoreAction: Boolean = false,
        /** The library row backing this playback session; null until [Event.Load] resolves it. */
        val item: VideoTaskItem? = null,
        val moreMenuVisible: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val title: String, val showMoreAction: Boolean, val filePath: String) : Event

        data class PlaybackStateChanged(val isPlaying: Boolean) : Event

        data class Progress(val positionMs: Long, val durationMs: Long) : Event

        /** Advances to the next speed in [PLAYBACK_SPEEDS], wrapping around. */
        data object CycleSpeed : Event

        data object ToggleLooping : Event

        data object ToggleFillMode : Event

        data object ToggleFullscreen : Event

        data object ShowMoreMenu : Event

        data object HideMoreMenu : Event

        data class Rename(val context: Context, val newName: String) : Event

        data object Delete : Event
    }

    sealed interface Effect : UiEffect {
        data class SpeedChanged(val speed: Float) : Effect

        data class LoopingChanged(val looping: Boolean) : Effect

        data class FillModeChanged(val fillMode: Boolean) : Effect

        data object RenameFailed : Effect

        data object Deleted : Effect
    }
}
