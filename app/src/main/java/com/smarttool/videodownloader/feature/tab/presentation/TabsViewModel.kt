package com.smarttool.videodownloader.feature.tab.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.tab.domain.usecase.ClearTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.DeleteTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.OpenTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TabsViewModel(
    observeTabs: ObserveTabsUseCase,
    private val deleteTab: DeleteTabUseCase,
    private val clearTabs: ClearTabsUseCase,
    private val openTab: OpenTabUseCase,
    private val createTab: CreateTabUseCase,
) : ViewModel() {

    val tabs: StateFlow<List<TabModel>> = observeTabs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onDeleteTab(tab: TabModel) {
        viewModelScope.launch { deleteTab(tab) }
    }

    fun onCloseAll() {
        viewModelScope.launch { clearTabs() }
    }

    /** Marks the tab active, then reports back so the host can open the web view. */
    fun onOpenTab(tab: TabModel, onReady: () -> Unit) {
        viewModelScope.launch {
            openTab(tab)
            onReady()
        }
    }

    fun onCreateTab(tab: TabModel, onReady: () -> Unit) {
        viewModelScope.launch {
            createTab(tab)
            onReady()
        }
    }
}
