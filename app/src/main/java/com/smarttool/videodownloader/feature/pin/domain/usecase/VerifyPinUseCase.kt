package com.smarttool.videodownloader.feature.pin.domain.usecase

import com.smarttool.videodownloader.feature.pin.domain.PinRepository

class VerifyPinUseCase(private val repository: PinRepository) {
    suspend operator fun invoke(pin: String): Boolean = pin == repository.savedPin()
}
