package com.smarttool.videodownloader.feature.pin.presentation

data class SecurityUiState(
    val questionIndex: Int = 1,
    val answer: String = "",
    val showQuestionPicker: Boolean = false,
)
