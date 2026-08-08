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

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    /** Emitted once [confirm] has finished reading from, or writing to, storage. */
    private val _results = Channel<SecurityResult>(Channel.BUFFERED)
    val results: Flow<SecurityResult> = _results.receiveAsFlow()

    fun onAnswerChange(answer: String) {
        _uiState.update { it.copy(answer = answer) }
    }

    fun onQuestionSelected(index: Int) {
        _uiState.update { it.copy(questionIndex = index, showQuestionPicker = false) }
    }

    fun setQuestionPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showQuestionPicker = visible) }
    }

    /**
     * @param isRecovery true when the user is answering the question to recover a
     *   forgotten PIN, false when they are setting the question up for a new PIN.
     */
    fun confirm(isRecovery: Boolean, pendingPin: String) {
        val state = _uiState.value
        val answer = state.answer.trim()

        if (answer.isEmpty()) {
            _results.trySend(SecurityResult.EmptyAnswer)
            return
        }

        viewModelScope.launch {
            if (!isRecovery) {
                saveSecurityQuestion(pendingPin, state.questionIndex, answer)
                _results.send(SecurityResult.Saved)
                return@launch
            }

            if (verifySecurityAnswer(state.questionIndex, answer)) {
                resetPin()
                _results.send(SecurityResult.RecoveryCorrect)
            } else {
                _results.send(SecurityResult.RecoveryIncorrect)
            }
        }
    }
}
