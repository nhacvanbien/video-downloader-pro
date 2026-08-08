package com.smarttool.videodownloader.feature.pin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.pin.domain.usecase.ResetPinUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.SaveSecurityQuestionUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.VerifySecurityAnswerUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecurityViewModel(
    private val verifySecurityAnswer: VerifySecurityAnswerUseCase,
    private val saveSecurityQuestion: SaveSecurityQuestionUseCase,
    private val resetPin: ResetPinUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityContract.State())
    val uiState: StateFlow<SecurityContract.State> = _uiState.asStateFlow()

    /** Emitted once [confirm] has finished reading from, or writing to, storage. */
    private val _effect = Channel<SecurityContract.Effect>(Channel.BUFFERED)
    val effect: Flow<SecurityContract.Effect> = _effect.receiveAsFlow()

    fun onEvent(event: SecurityContract.Event) {
        when (event) {
            is SecurityContract.Event.AnswerChange -> onAnswerChange(event.answer)
            is SecurityContract.Event.QuestionSelected -> onQuestionSelected(event.index)
            is SecurityContract.Event.SetQuestionPickerVisible ->
                setQuestionPickerVisible(event.visible)
            is SecurityContract.Event.Confirm -> confirm(event.isRecovery, event.pendingPin)
        }
    }

    private fun onAnswerChange(answer: String) {
        _uiState.update { it.copy(answer = answer) }
    }

    private fun onQuestionSelected(index: Int) {
        _uiState.update { it.copy(questionIndex = index, showQuestionPicker = false) }
    }

    private fun setQuestionPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showQuestionPicker = visible) }
    }

    /**
     * @param isRecovery true when the user is answering the question to recover a
     *   forgotten PIN, false when they are setting the question up for a new PIN.
     */
    private fun confirm(isRecovery: Boolean, pendingPin: String) {
        val state = _uiState.value
        val answer = state.answer.trim()

        if (answer.isEmpty()) {
            _effect.trySend(SecurityContract.Effect.EmptyAnswer)
            return
        }

        viewModelScope.launch {
            if (!isRecovery) {
                saveSecurityQuestion(pendingPin, state.questionIndex, answer)
                _effect.send(SecurityContract.Effect.Saved)
                return@launch
            }

            if (verifySecurityAnswer(state.questionIndex, answer)) {
                resetPin()
                _effect.send(SecurityContract.Effect.RecoveryCorrect)
            } else {
                _effect.send(SecurityContract.Effect.RecoveryIncorrect)
            }
        }
    }
}
