package com.smarttool.videodownloader.feature.tab.domain.usecase

import com.smarttool.videodownloader.feature.tab.domain.TabsRepository

class ClearTabsUseCase(private val repository: TabsRepository) {
    suspend operator fun invoke() = repository.clearTabs()
}
