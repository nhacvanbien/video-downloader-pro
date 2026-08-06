package com.smarttool.videodownloader.feature.media.presentation

import androidx.lifecycle.ViewModel
import com.smarttool.videodownloader.feature.media.domain.usecase.GetPlaybackSettingsUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetFillModeUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetLoopingUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetPlaybackSpeedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Speeds the control cycles through, matching the View player's order. */
val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

class MediaViewModel(
    private val getPlaybackSettings: GetPlaybackSettingsUseCase,
    private val setPlaybackSpeed: SetPlaybackSpeedUseCase,
    private val setLooping: SetLoopingUseCase,
    private val setFillMode: SetFillModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    fun load(title: String, showMoreAction: Boolean) {
        val settings = getPlaybackSettings()

        _uiState.value = MediaUiState(
            title = title,
            speed = settings.speed,
            looping = settings.looping,
            fillMode = settings.fillMode,
            showMoreAction = showMoreAction,
        )
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    fun onProgress(positionMs: Long, durationMs: Long) {
        _uiState.update { it.copy(positionMs = positionMs, durationMs = durationMs) }
    }

    /** Advances to the next speed in [PLAYBACK_SPEEDS], wrapping around. */
    fun cycleSpeed(): Float {
        val current = _uiState.value.speed
        val nextIndex = (PLAYBACK_SPEEDS.indexOf(current) + 1) % PLAYBACK_SPEEDS.size
        val next = PLAYBACK_SPEEDS[nextIndex]

        setPlaybackSpeed(next)
        _uiState.update { it.copy(speed = next) }
        return next
    }

    fun toggleLooping(): Boolean {
        val looping = !_uiState.value.looping
        setLooping(looping)
        _uiState.update { it.copy(looping = looping) }
        return looping
    }

    fun toggleFillMode(): Boolean {
        val fill = !_uiState.value.fillMode
        setFillMode(fill)
        _uiState.update { it.copy(fillMode = fill) }
        return fill
    }
}
