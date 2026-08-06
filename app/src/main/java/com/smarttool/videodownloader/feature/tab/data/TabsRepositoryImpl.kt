package com.smarttool.videodownloader.feature.tab.data

import androidx.lifecycle.asFlow
import com.smarttool.videodownloader.data.repository.TabModelRepository
import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.flow.Flow

/**
 * Adapts the existing Room-backed [TabModelRepository] (still LiveData-based, shared
 * with unmigrated screens) to the Flow-based domain contract.
 */
class TabsRepositoryImpl(
    private val tabModelRepository: TabModelRepository,
) : TabsRepository {

    override fun observeTabs(): Flow<List<TabModel>> =
        tabModelRepository.getAllTabModel().asFlow()

    override suspend fun addTab(tab: TabModel) = tabModelRepository.insertTabModel(tab)

    override suspend fun deleteTab(tab: TabModel) = tabModelRepository.deleteTabModel(tab)

    override suspend fun clearTabs() = tabModelRepository.clearAllTabModel()

    override suspend fun selectTab(id: String, url: String, favicon: ByteArray?) =
        tabModelRepository.updateInfoTabModel(id, url, favicon, true)
}
