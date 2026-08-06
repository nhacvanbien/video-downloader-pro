package com.smarttool.videodownloader.feature.tab.domain.usecase

import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

class OpenTabUseCase(private val repository: TabsRepository) {
    suspend operator fun invoke(tab: TabModel) =
        repository.selectTab(tab.id, tab.url, tab.favicon)
}
