package com.smarttool.videodownloader.feature.pin.domain

interface PinRepository {
    suspend fun isPinConfigured(): Boolean

    suspend fun savedPin(): String

    suspend fun savePin(pin: String)

    suspend fun securityQuestionIndex(): Int

    suspend fun securityAnswer(): String

    suspend fun saveSecurityQuestion(index: Int, answer: String)

    suspend fun clearPin()
}
