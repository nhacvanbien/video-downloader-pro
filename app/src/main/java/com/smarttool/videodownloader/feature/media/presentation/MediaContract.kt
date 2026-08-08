package com.smarttool.videodownloader.feature.media.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface MediaContract {
    data class State(
        val title: String = "",
        val isPlaying: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
        val speed: Float = 1f,
        val looping: Boolean = false,
        val fillMode: Boolean = false,
        /** Only downloaded items expose the detail/more action. */
        val showMoreAction: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class Load(val title: String, val showMoreAction: Boolean) : Event

        data class PlaybackStateChanged(val isPlaying: Boolean) : Event

        data class Progress(val positionMs: Long, val durationMs: Long) : Event

        /** Advances to the next speed in [PLAYBACK_SPEEDS], wrapping around. */
        data object CycleSpeed : Event

        data object ToggleLooping : Event

        data object ToggleFillMode : Event
    }

    sealed interface Effect : UiEffect {
        data class SpeedChanged(val speed: Float) : Effect

        data class LoopingChanged(val looping: Boolean) : Effect

        data class FillModeChanged(val fillMode: Boolean) : Effect
    }
}
