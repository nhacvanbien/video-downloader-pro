package com.smarttool.videodownloader.feature.tab.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.domain.usecase.ClearTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.DeleteTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.OpenTabUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _effect = Channel<TabsContract.Effect>(Channel.BUFFERED)
    val effect: Flow<TabsContract.Effect> = _effect.receiveAsFlow()

    fun onEvent(event: TabsContract.Event) {
        when (event) {
            is TabsContract.Event.DeleteTab -> onDeleteTab(event.tab)
            is TabsContract.Event.CloseAll -> onCloseAll()
            is TabsContract.Event.OpenTab -> onOpenTab(event.tab)
            is TabsContract.Event.CreateTab -> onCreateTab(event.tab)
        }
    }

    private fun onDeleteTab(tab: TabModel) {
        viewModelScope.launch { deleteTab(tab) }
    }

    private fun onCloseAll() {
        viewModelScope.launch { clearTabs() }
    }

    /** Marks the tab active, then signals the host to open the web view. */
    private fun onOpenTab(tab: TabModel) {
        viewModelScope.launch {
            openTab(tab)
            _effect.send(TabsContract.Effect.TabOpened(tab))
        }
    }

    private fun onCreateTab(tab: TabModel) {
        viewModelScope.launch {
            createTab(tab)
            _effect.send(TabsContract.Effect.TabCreated(tab))
        }
    }
}
