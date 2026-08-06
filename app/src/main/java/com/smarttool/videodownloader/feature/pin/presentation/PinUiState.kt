package com.smarttool.videodownloader.feature.pin.presentation

const val PIN_LENGTH = 4

data class PinUiState(
    val step: PinStep = PinStep.Create,
    val entered: String = "",
    val showIncorrect: Boolean = false,
    val allowForgotPin: Boolean = false,
)
