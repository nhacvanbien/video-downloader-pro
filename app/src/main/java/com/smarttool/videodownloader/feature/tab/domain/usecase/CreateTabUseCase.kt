package com.smarttool.videodownloader.feature.tab.domain.usecase

import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

class CreateTabUseCase(private val repository: TabsRepository) {
    suspend operator fun invoke(tab: TabModel) = repository.addTab(tab)
}
