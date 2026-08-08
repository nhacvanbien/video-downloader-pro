package com.smarttool.videodownloader.feature.intro.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface IntroContract {
    data class State(val placeholder: Unit = Unit) : UiState

    sealed interface Event : UiEvent {
        data object Finish : Event
    }

    sealed interface Effect : UiEffect {
        data class Finished(val showPermission: Boolean) : Effect
    }
}
