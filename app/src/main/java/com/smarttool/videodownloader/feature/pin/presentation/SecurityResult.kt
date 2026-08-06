package com.smarttool.videodownloader.feature.pin.presentation

/** Outcome of pressing confirm, so the host can navigate or surface a message. */
enum class SecurityResult {
    EmptyAnswer,
    RecoveryCorrect,
    RecoveryIncorrect,
    Saved,
}
