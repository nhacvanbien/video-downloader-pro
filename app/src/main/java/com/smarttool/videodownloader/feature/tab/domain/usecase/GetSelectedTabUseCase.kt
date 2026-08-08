package com.smarttool.videodownloader.feature.tab.domain.usecase

import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel

class GetSelectedTabUseCase(private val repository: TabsRepository) {
    suspend operator fun invoke(): TabModel? = repository.getSelectedTab()
}
