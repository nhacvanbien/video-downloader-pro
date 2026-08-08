package com.smarttool.videodownloader.feature.main.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.main.domain.usecase.RecordAppExitUseCase
import kotlinx.coroutines.launch

/** The preference work the host Activity used to do inline. */
class MainViewModel(
    private val recordAppExit: RecordAppExitUseCase,
) : ViewModel() {

    /**
     * @param onRecorded run once the counter is on disk — the caller tears the process
     *   down straight afterwards, so the write cannot be left in flight.
     */
    fun onExitConfirmed(onRecorded: () -> Unit) {
        viewModelScope.launch {
            recordAppExit()
            onRecorded()
        }
    }
}
