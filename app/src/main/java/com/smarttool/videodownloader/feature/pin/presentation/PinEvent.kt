package com.smarttool.videodownloader.feature.pin.presentation

sealed interface PinEvent {
    /** Existing PIN verified — proceed into the private area. */
    data object Unlocked : PinEvent

    /** A brand-new PIN was confirmed and still needs a security question attached. */
    data class PinChosen(val pin: String) : PinEvent

    /** An existing PIN was replaced in place; nothing further to collect. */
    data object PinChanged : PinEvent

    data object ConfirmMismatch : PinEvent
}
