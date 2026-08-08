package com.smarttool.videodownloader.feature.language.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun onEvent(event: LanguageContract.Event) {
        when (event) {
            is LanguageContract.Event.Load -> load(event.currentCode, event.preselect)
            is LanguageContract.Event.Select -> select(event.code)
            is LanguageContract.Event.Confirm -> confirm(event.markStartShown)
        }
    }

    /**
     * @param preselect false on the first-open (splash) flow, where no row starts
     *   selected so the user must make an explicit choice.
     */
    private fun load(currentCode: String, preselect: Boolean) {
        _uiState.value = LanguageContract.State(
            languages = getLanguages(currentCode),
            selectedCode = currentCode.takeIf { preselect },
        )
    }

    private fun select(code: String) {
        _uiState.update { it.copy(selectedCode = code, showHintAnimation = false) }
    }

    private fun confirm(markStartShown: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val selected = state.languages.firstOrNull { it.code == state.selectedCode }

            if (selected == null) {
                _effect.send(LanguageContract.Effect.ConfirmFailed)
                return@launch
            }

            applyLanguage(selected.code, markStartShown)
            _effect.send(LanguageContract.Effect.Confirmed(selected))
        }
    }
}
