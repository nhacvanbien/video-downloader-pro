package com.smarttool.videodownloader.feature.tab.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

interface TabsContract {
    sealed interface Event : UiEvent {
        data class DeleteTab(val tab: TabModel) : Event
        data object CloseAll : Event
        data class OpenTab(val tab: TabModel) : Event
        data class CreateTab(val tab: TabModel) : Event
    }

    sealed interface Effect : UiEffect {
        /** Tab was marked active; the host should now open the web view on it. */
        data class TabOpened(val tab: TabModel) : Effect

        /** Tab was persisted; the host should now open the web view on it. */
        data class TabCreated(val tab: TabModel) : Effect
    }
}
