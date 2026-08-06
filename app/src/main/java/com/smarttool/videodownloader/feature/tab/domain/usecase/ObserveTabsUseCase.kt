package com.smarttool.videodownloader.feature.tab.domain.usecase

import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import kotlinx.coroutines.flow.Flow

class ObserveTabsUseCase(private val repository: TabsRepository) {
    operator fun invoke(): Flow<List<TabModel>> = repository.observeTabs()
}
