package com.smarttool.videodownloader.feature.browser.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.browser.domain.model.PopularSite
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetPopularSitesUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserHomeViewModel(
    getPopularSites: GetPopularSitesUseCase,
    observeTabs: ObserveTabsUseCase,
    private val createTab: CreateTabUseCase,
) : ViewModel() {

    val sites: List<PopularSite> = getPopularSites()

    private val _uiState = MutableStateFlow(BrowserHomeUiState())
    val uiState: StateFlow<BrowserHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeTabs().collect { tabs ->
                _uiState.update { it.copy(tabCount = tabs.size) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun clearQuery() {
        _uiState.update { it.copy(query = "") }
    }

    /** Persists the new tab, then reports back so the host can open the web view. */
    fun openTab(tab: TabModel, onReady: () -> Unit) {
        viewModelScope.launch {
            createTab(tab)
            onReady()
        }
    }
}
