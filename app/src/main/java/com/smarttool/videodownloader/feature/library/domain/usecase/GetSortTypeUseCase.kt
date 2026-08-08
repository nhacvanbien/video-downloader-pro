package com.smarttool.videodownloader.feature.library.domain.usecase

import com.smarttool.videodownloader.feature.library.domain.LibraryPreferencesRepository

class GetSortTypeUseCase(private val repository: LibraryPreferencesRepository) {
    suspend operator fun invoke(): Int = repository.sortType()
}
