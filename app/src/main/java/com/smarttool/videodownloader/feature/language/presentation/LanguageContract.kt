package com.smarttool.videodownloader.feature.language.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage

interface LanguageContract {
    data class State(
        val languages: List<AppLanguage> = emptyList(),
        val selectedCode: String? = null,
        /** True until the user picks a row; drives the attention animation on the hoisted item. */
        val showHintAnimation: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        /**
         * @param preselect false on the first-open (splash) flow, where no row starts
         *   selected so the user must make an explicit choice.
         */
        data class Load(val currentCode: String, val preselect: Boolean) : Event

        data class Select(val code: String) : Event

        data class Confirm(val markStartShown: Boolean) : Event
    }

    sealed interface Effect : UiEffect {
        data class Confirmed(val language: AppLanguage) : Effect

        data object ConfirmFailed : Effect
    }
}
