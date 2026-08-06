package com.smarttool.videodownloader.feature.pin.domain

interface PinRepository {
    fun isPinConfigured(): Boolean

    fun savedPin(): String

    fun savePin(pin: String)

    fun securityQuestionIndex(): Int

    fun securityAnswer(): String

    fun saveSecurityQuestion(index: Int, answer: String)

    fun clearPin()
}
