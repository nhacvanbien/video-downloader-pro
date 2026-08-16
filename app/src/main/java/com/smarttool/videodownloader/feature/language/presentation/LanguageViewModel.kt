package com.smarttool.videodownloader.feature.language.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.language.domain.usecase.ApplyLanguageUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetLanguagesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LanguageViewModel(
    private val getLanguages: GetLanguagesUseCase,
    private val applyLanguage: ApplyLanguageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LanguageContract.State())
    val uiState: StateFlow<LanguageContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<LanguageContract.Effect>(Channel.BUFFERED)
    val effect: Flow<LanguageContract.Effect> = _effect.receiveAsFlow()

    private var allLanguages: List<AppLanguage> = emptyList()

    fun onEvent(event: LanguageContract.Event) {
        when (event) {
            is LanguageContract.Event.Load -> load(event.currentCode, event.preselect)
            is LanguageContract.Event.Select -> select(event.code)
            is LanguageContract.Event.Search -> search(event.query)
            is LanguageContract.Event.Confirm -> confirm(event.markStartShown)
        }
    }

    /**
     * @param preselect false on the first-open (splash) flow, where no row starts
     *   selected so the user must make an explicit choice.
     */
    private fun load(currentCode: String, preselect: Boolean) {
        allLanguages = getLanguages(currentCode)
        _uiState.value = LanguageContract.State(
            languages = allLanguages,
            selectedCode = currentCode.takeIf { preselect },
        )
    }

    private fun select(code: String) {
        _uiState.update { it.copy(selectedCode = code, showHintAnimation = false) }
    }

    private fun search(query: String) {
        val filtered = if (query.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.update { it.copy(languages = filtered, searchQuery = query) }
    }

    private fun confirm(markStartShown: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val selected = allLanguages.firstOrNull { it.code == state.selectedCode }

            if (selected == null) {
                _effect.send(LanguageContract.Effect.ConfirmFailed)
                return@launch
            }

            applyLanguage(selected.code, markStartShown)
            _effect.send(LanguageContract.Effect.Confirmed(selected))
        }
    }
}
