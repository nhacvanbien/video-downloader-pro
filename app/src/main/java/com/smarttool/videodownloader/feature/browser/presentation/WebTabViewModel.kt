package com.smarttool.videodownloader.feature.browser.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WebTabViewModel(
    private val adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebTabPipelineContract.State())
    val uiState: StateFlow<WebTabPipelineContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<WebTabPipelineContract.Effect>(Channel.BUFFERED)
    val effect: Flow<WebTabPipelineContract.Effect> = _effect.receiveAsFlow()

    fun isAd(url: String): Boolean = adBlockHostsRemoteDataSource.isAds(url)

    fun onEvent(event: WebTabPipelineContract.Event) {
        when (event) {
            WebTabPipelineContract.Event.SetListHost -> viewModelScope.launch {
                adBlockHostsRemoteDataSource.setListHost()
            }

            is WebTabPipelineContract.Event.FinishPage -> {
                setTabTextInput(event.url, true)
                _uiState.update { it.copy(isShowProgress = false) }
            }

            is WebTabPipelineContract.Event.StartPage -> {
                setTabTextInput(event.url)
                _uiState.update {
                    it.copy(isShowProgress = true, currentTitle = event.title, tabUrl = event.url)
                }
            }

            is WebTabPipelineContract.Event.UpdateVisitedHistory -> {
                if (event.url.startsWith("http")) {
                    setTabTextInput(event.url)
                    _uiState.update { it.copy(isShowProgress = true, tabUrl = event.url) }
                }
            }

            is WebTabPipelineContract.Event.ChangeTabFocus -> changeTabFocus(event.focused)

            is WebTabPipelineContract.Event.SetDownloadDialogShown ->
                _uiState.update { it.copy(isDownloadDialogShown = event.shown) }

            is WebTabPipelineContract.Event.OpenPage -> {
                if (event.input.isNotEmpty()) {
                    changeTabFocus(false)
                    val tab = WebTabFactory.createWebTabFromInput(event.input)
                    _effect.trySend(WebTabPipelineContract.Effect.OpenPage(tab))
                }
            }

            is WebTabPipelineContract.Event.LoadPage -> {
                if (event.input.isNotEmpty()) {
                    changeTabFocus(false)
                    val tab = WebTabFactory.createWebTabFromInput(event.input)
                    setTabTextInput(tab.getUrl())
                    _effect.trySend(WebTabPipelineContract.Effect.LoadPage(tab))
                }
            }

            is WebTabPipelineContract.Event.SetTabTextInput ->
                setTabTextInput(event.input, event.force)

            is WebTabPipelineContract.Event.CloseTab -> Unit

            is WebTabPipelineContract.Event.PageReload -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView?.reload()
            }

            is WebTabPipelineContract.Event.PageStop -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = false) }
                event.webView?.stopLoading()
            }

            is WebTabPipelineContract.Event.GoBack -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView.goBack()
            }

            is WebTabPipelineContract.Event.GoForward -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView.goForward()
            }

            is WebTabPipelineContract.Event.SetProgress ->
                // Every caller pairs this with "show progress while not yet at 100%";
                // folded in here since nothing ever sets progress without it.
                _uiState.update {
                    it.copy(progress = event.progress, isShowProgress = event.progress != 100)
                }

            is WebTabPipelineContract.Event.NotifyPageOpened ->
                _effect.trySend(WebTabPipelineContract.Effect.OpenPage(event.tab))
        }
    }

    private fun changeTabFocus(focused: Boolean) {
        _uiState.update { it.copy(isTabInputFocused = focused) }
        _effect.trySend(WebTabPipelineContract.Effect.ChangeTabFocus(focused))
    }

    private fun setTabTextInput(input: String?, force: Boolean = false) {
        if (input.isNullOrEmpty()) return
        if (!_uiState.value.isTabInputFocused || force) {
            _uiState.update { it.copy(tabUrl = input) }
        }
    }
}
