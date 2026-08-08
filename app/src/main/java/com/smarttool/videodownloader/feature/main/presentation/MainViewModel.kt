package com.smarttool.videodownloader.feature.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.main.domain.usecase.RecordAppExitUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The preference work the host Activity used to do inline. */
class MainViewModel(
    private val recordAppExit: RecordAppExitUseCase,
) : ViewModel() {

    // No screen state of its own; kept only for shape consistency across features.
    val uiState: StateFlow<MainContract.State> = MutableStateFlow(MainContract.State())

    private val _effect = Channel<MainContract.Effect>(Channel.BUFFERED)
    val effect: Flow<MainContract.Effect> = _effect.receiveAsFlow()

    fun onEvent(event: MainContract.Event) {
        when (event) {
            is MainContract.Event.ConfirmExit -> confirmExit()
        }
    }

    /**
     * The caller tears the process down right after [MainContract.Effect.ExitRecorded]
     * fires, so the write cannot be left in flight.
     */
    private fun confirmExit() {
        viewModelScope.launch {
            recordAppExit()
            _effect.send(MainContract.Effect.ExitRecorded)
        }
    }
}
