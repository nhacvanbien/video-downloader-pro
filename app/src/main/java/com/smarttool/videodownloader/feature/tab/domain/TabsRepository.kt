package com.smarttool.videodownloader.feature.tab.domain

import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.flow.Flow

interface TabsRepository {
    fun observeTabs(): Flow<List<TabModel>>

    suspend fun addTab(tab: TabModel)

    suspend fun deleteTab(tab: TabModel)

    suspend fun clearTabs()

    /** Marks [id] as the active tab, unselecting every other one. */
    suspend fun selectTab(id: String, url: String, favicon: ByteArray?)

    suspend fun getSelectedTab(): TabModel?
}
