package com.smarttool.videodownloader.feature.pin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.pin.domain.PinRepository
import com.smarttool.videodownloader.feature.pin.domain.usecase.SavePinUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.VerifyPinUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PinViewModel(
    private val repository: PinRepository,
    private val verifyPin: VerifyPinUseCase,
    private val savePin: SavePinUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinContract.State())
    val uiState: StateFlow<PinContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<PinContract.Effect>(Channel.BUFFERED)
    val effect: Flow<PinContract.Effect> = _effect.receiveAsFlow()

    private var firstEntry = ""
    private var isChangingPin = false

    fun onEvent(event: PinContract.Event) {
        when (event) {
            is PinContract.Event.Start -> start(event.changingPin)
            is PinContract.Event.Digit -> append(event.digit)
            PinContract.Event.Backspace -> backspace()
        }
    }

    /**
     * @param changingPin true when reached from "change PIN", which forces the
     *   create/confirm flow even though a PIN already exists.
     */
    private fun start(changingPin: Boolean) {
        isChangingPin = changingPin

        viewModelScope.launch {
            val verifying = repository.isPinConfigured() && !changingPin

            _uiState.value = PinContract.State(
                step = if (verifying) PinStep.Verify else PinStep.Create,
                allowForgotPin = verifying,
            )
        }
    }

    private fun append(digit: String) {
        val state = _uiState.value
        if (state.entered.length >= PIN_LENGTH) return

        val entered = state.entered + digit
        _uiState.value = state.copy(entered = entered, showIncorrect = false)

        if (entered.length == PIN_LENGTH) onComplete(entered)
    }

    private fun backspace() {
        val state = _uiState.value
        if (state.entered.isEmpty()) return
        _uiState.value = state.copy(entered = state.entered.dropLast(1), showIncorrect = false)
    }

    private fun onComplete(entered: String) {
        when (_uiState.value.step) {
            PinStep.Verify -> viewModelScope.launch {
                if (verifyPin(entered)) {
                    _effect.send(PinContract.Effect.Unlocked)
                } else {
                    _uiState.value = _uiState.value.copy(entered = "", showIncorrect = true)
                }
            }

            PinStep.Create -> {
                firstEntry = entered
                _uiState.value = _uiState.value.copy(step = PinStep.Confirm, entered = "")
            }

            PinStep.Confirm -> {
                if (entered == firstEntry) {
                    if (isChangingPin) {
                        // The effect navigates away, so the PIN is persisted before it is sent.
                        viewModelScope.launch {
                            savePin(entered)
                            _effect.send(PinContract.Effect.PinChanged)
                        }
                    } else {
                        _effect.trySend(PinContract.Effect.PinChosen(entered))
                    }
                } else {
                    _uiState.value = _uiState.value.copy(entered = "")
                    _effect.trySend(PinContract.Effect.ConfirmMismatch)
                }
            }
        }
    }
}
