package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.LibraryPreferencesRepository

class SetSortTypeUseCase(private val repository: LibraryPreferencesRepository) {
    suspend operator fun invoke(type: Int) = repository.setSortType(type)
}
