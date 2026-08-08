package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.core.coroutines.AppScope
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.downloads.domain.usecase.CancelDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ObserveActiveDownloadsUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.PauseDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ResumeDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StartDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StopAndSaveDownloadUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the download list and the actions on it.
 *
 * Every host that can start a download owns one of these, so an instance dies whenever
 * its host does — which is why the actions below run on [appScope] rather than
 * `viewModelScope`. Closing the browser right after tapping Download used to clear that
 * tab's ViewModel store while the enqueue was still suspended in the repository write,
 * cancelling it and losing the download with nothing shown to the user.
 */
class ProcessingViewModel(
    observeActiveDownloads: ObserveActiveDownloadsUseCase,
    private val appScope: AppScope,
    private val startDownload: StartDownloadUseCase,
    private val pauseDownload: PauseDownloadUseCase,
    private val resumeDownload: ResumeDownloadUseCase,
    private val cancelDownload: CancelDownloadUseCase,
    private val stopAndSaveDownload: StopAndSaveDownloadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcessingContract.State())
    val uiState: StateFlow<ProcessingContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<ProcessingContract.Effect>(Channel.BUFFERED)
    val effect: Flow<ProcessingContract.Effect> = _effect.receiveAsFlow()

    /** Screen state, so this one is right to stop with the screen. */
    val downloads: StateFlow<List<ProgressInfo>> = observeActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onEvent(event: ProcessingContract.Event) {
        when (event) {
            is ProcessingContract.Event.UrlChange -> onUrlChange(event.url)
            is ProcessingContract.Event.Start -> appScope.launch { startDownload(event.videoInfo) }
            is ProcessingContract.Event.Pause -> appScope.launch { pauseDownload(event.progressInfo) }
            is ProcessingContract.Event.Resume -> appScope.launch { resumeDownload(event.progressInfo) }
            is ProcessingContract.Event.Cancel ->
                appScope.launch { cancelDownload(event.progressInfo, event.removeFile) }

            is ProcessingContract.Event.StopAndSave -> stopAndSaveDownload(event.progressInfo)
        }
    }

    private fun onUrlChange(url: String) {
        _uiState.update { it.copy(url = url) }

        // Anything that is not an http(s) link cannot be probed, so reset the button.
        if (url.isBlank() || !url.startsWith("http")) {
            _effect.trySend(ProcessingContract.Effect.ResetDetection)
        } else {
            _effect.trySend(ProcessingContract.Effect.LoadUrl(url))
        }
    }
}
