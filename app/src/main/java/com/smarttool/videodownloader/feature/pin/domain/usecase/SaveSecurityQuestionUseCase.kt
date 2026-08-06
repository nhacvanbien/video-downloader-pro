package com.smarttool.videodownloader.feature.pin.domain.usecase

import com.smarttool.videodownloader.feature.pin.domain.PinRepository

class SaveSecurityQuestionUseCase(private val repository: PinRepository) {
    operator fun invoke(pin: String, questionIndex: Int, answer: String) {
        repository.savePin(pin)
        repository.saveSecurityQuestion(questionIndex, answer)
    }
}
