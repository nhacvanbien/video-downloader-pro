package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.usecase.CancelDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ObserveActiveDownloadsUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.PauseDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ResumeDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StartDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StopAndSaveDownloadUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProcessingViewModel(
    observeActiveDownloads: ObserveActiveDownloadsUseCase,
    private val startDownload: StartDownloadUseCase,
    private val pauseDownload: PauseDownloadUseCase,
    private val resumeDownload: ResumeDownloadUseCase,
    private val cancelDownload: CancelDownloadUseCase,
    private val stopAndSaveDownload: StopAndSaveDownloadUseCase,
) : ViewModel() {

    val downloads: StateFlow<List<ProgressInfo>> = observeActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start(videoInfo: VideoInfo) {
        viewModelScope.launch { startDownload(videoInfo) }
    }

    fun pause(progressInfo: ProgressInfo) {
        viewModelScope.launch { pauseDownload(progressInfo) }
    }

    fun resume(progressInfo: ProgressInfo) {
        viewModelScope.launch { resumeDownload(progressInfo) }
    }

    fun cancel(progressInfo: ProgressInfo, removeFile: Boolean) {
        viewModelScope.launch { cancelDownload(progressInfo, removeFile) }
    }

    fun stopAndSave(progressInfo: ProgressInfo) {
        stopAndSaveDownload(progressInfo)
    }
}
