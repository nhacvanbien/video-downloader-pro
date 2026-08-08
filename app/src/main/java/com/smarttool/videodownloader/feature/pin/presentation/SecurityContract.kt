package com.smarttool.videodownloader.feature.pin.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface SecurityContract {
    data class State(
        val questionIndex: Int = 1,
        val answer: String = "",
        val showQuestionPicker: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnswerChange(val answer: String) : Event

        data class QuestionSelected(val index: Int) : Event

        data class SetQuestionPickerVisible(val visible: Boolean) : Event

        /**
         * @param isRecovery true when the user is answering the question to recover a
         *   forgotten PIN, false when they are setting the question up for a new PIN.
         */
        data class Confirm(val isRecovery: Boolean, val pendingPin: String) : Event
    }

    /** Outcome of pressing confirm, so the host can navigate or surface a message. */
    sealed interface Effect : UiEffect {
        data object EmptyAnswer : Effect

        data object RecoveryCorrect : Effect

        data object RecoveryIncorrect : Effect

        data object Saved : Effect
    }
}
