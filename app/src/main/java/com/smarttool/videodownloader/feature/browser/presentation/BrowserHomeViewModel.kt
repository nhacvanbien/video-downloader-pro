package com.smarttool.videodownloader.feature.browser.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.browser.domain.model.PopularSite
import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetPopularSitesUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.ObserveSearchEngineUseCase
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserHomeViewModel(
    getPopularSites: GetPopularSitesUseCase,
    observeTabs: ObserveTabsUseCase,
    observeSearchEngine: ObserveSearchEngineUseCase,
    private val createTab: CreateTabUseCase,
) : ViewModel() {

    val sites: List<PopularSite> = getPopularSites()

    private val _uiState = MutableStateFlow(BrowserHomeContract.State())
    val uiState: StateFlow<BrowserHomeContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<BrowserHomeContract.Effect>(Channel.BUFFERED)
    val effect: Flow<BrowserHomeContract.Effect> = _effect.receiveAsFlow()

    /** Kept as a plain field — [openTab] is a synchronous event handler, not a suspend one. */
    private var searchEngine: SearchEngine = SearchEngine.GOOGLE

    init {
        viewModelScope.launch {
            observeTabs().collect { tabs ->
                _uiState.update { it.copy(tabCount = tabs.size) }
            }
        }

        viewModelScope.launch {
            observeSearchEngine().collect { searchEngine = it }
        }
    }

    fun onEvent(event: BrowserHomeContract.Event) {
        when (event) {
            is BrowserHomeContract.Event.QueryChange -> onQueryChange(event.query)
            is BrowserHomeContract.Event.ClearQuery -> clearQuery()
            is BrowserHomeContract.Event.OpenTab -> openTab(event.tab)
        }
    }

    private fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    private fun clearQuery() {
        _uiState.update { it.copy(query = "") }
    }

    /** Persists the new tab, then signals the host to open the web view. */
    private fun openTab(tab: TabModel) {
        viewModelScope.launch {
            createTab(tab)
            _effect.send(BrowserHomeContract.Effect.TabReady(tab))
        }
    }

    /** Builds the tab from raw address-bar/search-box input, honouring the current engine. */
    fun tabModelFromInput(input: String): TabModel =
        WebTabFactory.createTabModelFromInput(input, searchEngine.urlTemplate)
}
