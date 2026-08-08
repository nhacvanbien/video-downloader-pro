package com.smarttool.videodownloader.feature.pin.domain.usecase

import com.smarttool.videodownloader.feature.pin.domain.PinRepository

class ResetPinUseCase(private val repository: PinRepository) {
    suspend operator fun invoke() = repository.clearPin()
}
