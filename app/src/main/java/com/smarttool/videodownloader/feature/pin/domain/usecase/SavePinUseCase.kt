package com.smarttool.videodownloader.feature.pin.domain.usecase

import com.smarttool.videodownloader.feature.pin.domain.PinRepository

class SavePinUseCase(private val repository: PinRepository) {
    suspend operator fun invoke(pin: String) = repository.savePin(pin)
}
