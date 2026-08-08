package com.smarttool.videodownloader.feature.pin.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

const val PIN_LENGTH = 4

interface PinContract {
    data class State(
        val step: PinStep = PinStep.Create,
        val entered: String = "",
        val showIncorrect: Boolean = false,
        val allowForgotPin: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        /**
         * @param changingPin true when reached from "change PIN", which forces the
         *   create/confirm flow even though a PIN already exists.
         */
        data class Start(val changingPin: Boolean) : Event

        data class Digit(val digit: String) : Event

        data object Backspace : Event
    }

    sealed interface Effect : UiEffect {
        /** Existing PIN verified — proceed into the private area. */
        data object Unlocked : Effect

        /** A brand-new PIN was confirmed and still needs a security question attached. */
        data class PinChosen(val pin: String) : Effect

        /** An existing PIN was replaced in place; nothing further to collect. */
        data object PinChanged : Effect

        data object ConfirmMismatch : Effect
    }
}
