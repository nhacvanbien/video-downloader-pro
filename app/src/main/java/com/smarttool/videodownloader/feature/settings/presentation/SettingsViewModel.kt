package com.smarttool.videodownloader.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetSearchEngineUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ObserveActiveDownloadsUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SetWifiOnlyUseCase
import com.smarttool.videodownloader.feature.settings.domain.usecase.GetAppInfoUseCase
import com.smarttool.videodownloader.feature.settings.domain.usecase.GetDownloadStatsUseCase
import com.smarttool.videodownloader.feature.settings.domain.usecase.GetSettingsUseCase
import com.smarttool.videodownloader.feature.settings.domain.usecase.SetDownloadLocationSubfolderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettings: GetSettingsUseCase,
    private val setWifiOnly: SetWifiOnlyUseCase,
    private val setSearchEngine: SetSearchEngineUseCase,
    private val setDownloadLocationSubfolder: SetDownloadLocationSubfolderUseCase,
    private val getDownloadStats: GetDownloadStatsUseCase,
    private val getAppInfo: GetAppInfoUseCase,
    private val observeActiveDownloads: ObserveActiveDownloadsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsContract.State())
    val uiState: StateFlow<SettingsContract.State> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onEvent(event: SettingsContract.Event) {
        when (event) {
            is SettingsContract.Event.Refresh -> refresh()
            is SettingsContract.Event.SetWifiOnly -> onSetWifiOnly(event.enabled)
            is SettingsContract.Event.SetSearchEngine -> onSetSearchEngine(event.engine)
            is SettingsContract.Event.SetSearchEngineSheetVisible ->
                _uiState.update { it.copy(searchEngineSheetVisible = event.visible) }
            is SettingsContract.Event.SetDownloadLocationEditorVisible ->
                _uiState.update { it.copy(downloadLocationEditorVisible = event.visible) }
            is SettingsContract.Event.SetDownloadLocationSubfolder ->
                onSetDownloadLocationSubfolder(event.name)
        }
    }

    /** Re-read on resume: rating state, and download stats, change outside this screen. */
    private fun refresh() {
        viewModelScope.launch {
            val settings = getSettings()
            val stats = getDownloadStats()
            val appInfo = getAppInfo()
            val activeDownloadCount = observeActiveDownloads().first().size
            _uiState.value = SettingsContract.State(
                downloadLocation = settings.downloadLocation,
                downloadLocationSubfolder = settings.downloadLocationSubfolder,
                showRateRow = !settings.isRated,
                wifiOnly = settings.wifiOnly,
                searchEngine = settings.searchEngine,
                versionName = appInfo.versionName,
                lastUpdateTimeMillis = appInfo.lastUpdateTimeMillis,
                videoCount = stats.videoCount,
                usedBytes = stats.usedBytes,
                freeBytes = stats.freeBytes,
                activeDownloadCount = activeDownloadCount,
            )
        }
    }

    private fun onSetDownloadLocationSubfolder(name: String) {
        _uiState.update { it.copy(downloadLocationEditorVisible = false) }
        viewModelScope.launch {
            setDownloadLocationSubfolder(name)
            refresh()
        }
    }

    private fun onSetWifiOnly(enabled: Boolean) {
        _uiState.update { it.copy(wifiOnly = enabled) }
        viewModelScope.launch { setWifiOnly(enabled) }
    }

    private fun onSetSearchEngine(engine: SearchEngine) {
        _uiState.update { it.copy(searchEngine = engine, searchEngineSheetVisible = false) }
        viewModelScope.launch { setSearchEngine(engine) }
    }
}
