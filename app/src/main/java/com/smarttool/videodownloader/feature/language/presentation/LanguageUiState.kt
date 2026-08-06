package com.smarttool.videodownloader.feature.language.presentation

import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage

data class LanguageUiState(
    val languages: List<AppLanguage> = emptyList(),
    val selectedCode: String? = null,
    /** True until the user picks a row; drives the attention animation on the hoisted item. */
    val showHintAnimation: Boolean = true,
)
