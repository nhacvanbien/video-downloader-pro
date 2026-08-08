package com.smarttool.videodownloader.feature.tab.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.data.repository.TabModelRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TabModelViewModel constructor(
    private val tabModelRepository: TabModelRepository,
) :
    ViewModel() {

    fun queryAllTabModel(): LiveData<List<TabModel>> {
        return tabModelRepository.getAllTabModel()
    }


    suspend fun insertTabModel(item: TabModel) {
        tabModelRepository.insertTabModel(item)
    }


    fun deleteTabModel(item: TabModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tabModelRepository.deleteTabModel(item)
            } catch (e: Throwable) {
                Timber.e(e, "deleteTabModel failed: id=${item.id}")
            }
        }
    }

    suspend fun updateInfoTabModel(
        id: String,
        newUrl: String,
        favicon: ByteArray?,
        isSelected: Boolean
    ) {
        tabModelRepository.updateInfoTabModel(id, newUrl, favicon, isSelected)
    }

    suspend fun getSelectedTabModel(): TabModel? {
        return tabModelRepository.getSelectedTabModel()
    }

    suspend fun clearAllTabModel() {
        tabModelRepository.clearAllTabModel()
    }

}