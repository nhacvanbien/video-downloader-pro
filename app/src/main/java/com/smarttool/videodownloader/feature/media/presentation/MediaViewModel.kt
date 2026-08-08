package com.smarttool.videodownloader.feature.media.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.media.domain.usecase.GetPlaybackSettingsUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetFillModeUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetLoopingUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetPlaybackSpeedUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Speeds the control cycles through, matching the View player's order. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

class MediaViewModel(
    private val getPlaybackSettings: GetPlaybackSettingsUseCase,
    private val setPlaybackSpeed: SetPlaybackSpeedUseCase,
    private val setLooping: SetLoopingUseCase,
    private val setFillMode: SetFillModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaContract.State())
    val uiState: StateFlow<MediaContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<MediaContract.Effect>(Channel.BUFFERED)
    val effect: Flow<MediaContract.Effect> = _effect.receiveAsFlow()

    fun onEvent(event: MediaContract.Event) {
        when (event) {
            is MediaContract.Event.Load -> load(event.title, event.showMoreAction)
            is MediaContract.Event.PlaybackStateChanged -> onPlaybackStateChanged(event.isPlaying)
            is MediaContract.Event.Progress -> onProgress(event.positionMs, event.durationMs)
            MediaContract.Event.CycleSpeed -> cycleSpeed()
            MediaContract.Event.ToggleLooping -> toggleLooping()
            MediaContract.Event.ToggleFillMode -> toggleFillMode()
        }
    }

    private fun load(title: String, showMoreAction: Boolean) {
        viewModelScope.launch {
            val settings = getPlaybackSettings()

            _uiState.value = MediaContract.State(
                title = title,
                speed = settings.speed,
                looping = settings.looping,
                fillMode = settings.fillMode,
                showMoreAction = showMoreAction,
            )
        }
    }

    private fun onPlaybackStateChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    private fun onProgress(positionMs: Long, durationMs: Long) {
        _uiState.update { it.copy(positionMs = positionMs, durationMs = durationMs) }
    }

    /** Advances to the next speed in [PLAYBACK_SPEEDS], wrapping around. */
    private fun cycleSpeed() {
        val current = _uiState.value.speed
        val nextIndex = (PLAYBACK_SPEEDS.indexOf(current) + 1) % PLAYBACK_SPEEDS.size
        val next = PLAYBACK_SPEEDS[nextIndex]

        _uiState.update { it.copy(speed = next) }
        viewModelScope.launch { setPlaybackSpeed(next) }
        _effect.trySend(MediaContract.Effect.SpeedChanged(next))
    }

    private fun toggleLooping() {
        val looping = !_uiState.value.looping
        _uiState.update { it.copy(looping = looping) }
        viewModelScope.launch { setLooping(looping) }
        _effect.trySend(MediaContract.Effect.LoopingChanged(looping))
    }

    private fun toggleFillMode() {
        val fill = !_uiState.value.fillMode
        _uiState.update { it.copy(fillMode = fill) }
        viewModelScope.launch { setFillMode(fill) }
        _effect.trySend(MediaContract.Effect.FillModeChanged(fill))
    }
}
