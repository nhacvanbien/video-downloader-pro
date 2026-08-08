package com.smarttool.videodownloader.feature.main.domain.usecase

import com.smarttool.videodownloader.feature.main.domain.AppUsageRepository

class RecordAppExitUseCase(private val repository: AppUsageRepository) {
    suspend operator fun invoke() = repository.recordAppExit()
}
