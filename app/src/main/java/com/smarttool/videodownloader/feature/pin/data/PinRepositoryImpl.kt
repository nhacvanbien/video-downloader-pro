package com.smarttool.videodownloader.feature.pin.data

import com.smarttool.videodownloader.feature.pin.domain.PinRepository
import com.smarttool.videodownloader.helper.PreferenceHelper

class PinRepositoryImpl(
    private val preferenceHelper: PreferenceHelper,
) : PinRepository {

    override fun isPinConfigured(): Boolean = preferenceHelper.getIsSetupPinCode()

    override fun savedPin(): String = preferenceHelper.getPinCode().orEmpty()

    override fun savePin(pin: String) {
        preferenceHelper.setIsSetupPinCode(true)
        preferenceHelper.setPinCode(pin)
    }

    override fun securityQuestionIndex(): Int = preferenceHelper.getNumSecurityQuestion()

    override fun securityAnswer(): String = preferenceHelper.getSecurityAnswer().orEmpty()

    override fun saveSecurityQuestion(index: Int, answer: String) {
        preferenceHelper.setNumSecurityQuestion(index)
        preferenceHelper.setSecurityAnswer(answer)
    }

    override fun clearPin() {
        preferenceHelper.setIsSetupPinCode(false)
        preferenceHelper.setNumSecurityQuestion(1)
        preferenceHelper.setPinCode("")
        preferenceHelper.setSecurityAnswer("")
    }
}
