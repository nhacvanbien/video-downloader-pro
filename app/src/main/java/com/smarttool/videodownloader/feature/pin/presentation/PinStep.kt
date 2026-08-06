package com.smarttool.videodownloader.feature.pin.presentation

/** Which prompt the keypad is currently collecting. */
enum class PinStep {
    /** Unlocking with an already-configured PIN. */
    Verify,

    /** First entry when choosing a new PIN. */
    Create,

    /** Second entry, must match [PinStep.Create]. */
    Confirm,
}
