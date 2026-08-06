package com.smarttool.videodownloader.feature.pin.domain.usecase

import com.smarttool.videodownloader.feature.pin.domain.PinRepository

/** Checks the answer to the recovery question that unlocks a forgotten PIN. */
class VerifySecurityAnswerUseCase(private val repository: PinRepository) {
    operator fun invoke(questionIndex: Int, answer: String): Boolean =
        answer == repository.securityAnswer() &&
            questionIndex == repository.securityQuestionIndex()
}
