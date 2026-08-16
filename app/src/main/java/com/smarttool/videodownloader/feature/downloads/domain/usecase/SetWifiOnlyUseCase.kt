package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.feature.downloads.domain.WifiOnlyRepository

class SetWifiOnlyUseCase(private val repository: WifiOnlyRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setWifiOnly(enabled)
}
