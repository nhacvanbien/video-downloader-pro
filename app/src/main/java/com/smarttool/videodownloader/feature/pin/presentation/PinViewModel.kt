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

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    private val _events = Channel<PinEvent>(Channel.BUFFERED)
    val events: Flow<PinEvent> = _events.receiveAsFlow()

    private var firstEntry = ""
    private var isChangingPin = false

    /**
     * @param changingPin true when reached from "change PIN", which forces the
     *   create/confirm flow even though a PIN already exists.
     */
    suspend fun start(changingPin: Boolean) {
        isChangingPin = changingPin
        val verifying = repository.isPinConfigured() && !changingPin

        _uiState.value = PinUiState(
            step = if (verifying) PinStep.Verify else PinStep.Create,
            allowForgotPin = verifying,
        )
    }

    fun append(digit: String) {
        val state = _uiState.value
        if (state.entered.length >= PIN_LENGTH) return

        val entered = state.entered + digit
        _uiState.value = state.copy(entered = entered, showIncorrect = false)

        if (entered.length == PIN_LENGTH) onComplete(entered)
    }

    fun backspace() {
        val state = _uiState.value
        if (state.entered.isEmpty()) return
        _uiState.value = state.copy(entered = state.entered.dropLast(1), showIncorrect = false)
    }

    private fun onComplete(entered: String) {
        when (_uiState.value.step) {
            PinStep.Verify -> viewModelScope.launch {
                if (verifyPin(entered)) {
                    _events.send(PinEvent.Unlocked)
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
                        // The event navigates away, so the PIN is persisted before it is sent.
                        viewModelScope.launch {
                            savePin(entered)
                            _events.send(PinEvent.PinChanged)
                        }
                    } else {
                        _events.trySend(PinEvent.PinChosen(entered))
                    }
                } else {
                    _uiState.value = _uiState.value.copy(entered = "")
                    _events.trySend(PinEvent.ConfirmMismatch)
                }
            }
        }
    }
}
