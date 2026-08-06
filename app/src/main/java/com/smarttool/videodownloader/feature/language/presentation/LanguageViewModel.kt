package com.smarttool.videodownloader.feature.language.presentation

import androidx.lifecycle.ViewModel
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.language.domain.usecase.ApplyLanguageUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetLanguagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LanguageViewModel(
    private val getLanguages: GetLanguagesUseCase,
    private val applyLanguage: ApplyLanguageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageUiState())
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    /**
     * @param preselect false on the first-open (splash) flow, where no row starts
     *   selected so the user must make an explicit choice.
     */
    fun load(currentCode: String, preselect: Boolean) {
        _uiState.value = LanguageUiState(
            languages = getLanguages(currentCode),
            selectedCode = currentCode.takeIf { preselect },
        )
    }

    fun select(code: String) {
        _uiState.update { it.copy(selectedCode = code, showHintAnimation = false) }
    }

    fun confirm(markStartShown: Boolean): AppLanguage? {
        val state = _uiState.value
        val selected = state.languages.firstOrNull { it.code == state.selectedCode }
            ?: return null

        applyLanguage(selected.code, markStartShown)
        return selected
    }
}
