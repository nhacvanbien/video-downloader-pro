package com.smarttool.videodownloader.data.repository

import androidx.lifecycle.LiveData
import com.smarttool.videodownloader.data.dao.TabModelDao
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

class TabModelRepository  constructor(
    private val tabModelDao: TabModelDao
) {

    fun getAllTabModel(): LiveData<List<TabModel>> {
        return tabModelDao.getAllTabModel()
    }

    suspend fun insertTabModel(item: TabModel) {

        tabModelDao.insertAndUnselectOthers(item)

    }

    suspend fun deleteTabModel(item: TabModel) {
        tabModelDao.deleteTabModel(item)
    }

    suspend fun clearAllTabModel() {
        tabModelDao.clear()
    }

    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    ) {
        tabModelDao.updateAndUnselectOthers(id, newUrl, favicon, isSelected)
    }

    suspend fun getSelectedTabModel(): TabModel? {
        return tabModelDao.getSelectedTabModel()
    }

    fun findVideoTaskItemByName(id: String): TabModel {
        return tabModelDao.findVideoTaskItemByName(id)
    }
}