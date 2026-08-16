package com.smarttool.videodownloader.feature.downloads.domain.usecase

import com.smarttool.videodownloader.feature.downloads.domain.WifiOnlyRepository
import kotlinx.coroutines.flow.first

class GetWifiOnlyUseCase(private val repository: WifiOnlyRepository) {
    suspend operator fun invoke(): Boolean = repository.wifiOnly.first()
}
