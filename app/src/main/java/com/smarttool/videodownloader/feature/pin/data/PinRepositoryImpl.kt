package com.smarttool.videodownloader.feature.pin.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.pin.domain.PinRepository
import kotlinx.coroutines.flow.first

class PinRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : PinRepository {

    override suspend fun isPinConfigured(): Boolean = preferences.isPinConfigured.first()

    override suspend fun savedPin(): String = preferences.pinCode.first()

    override suspend fun savePin(pin: String) = preferences.savePin(pin)

    override suspend fun securityQuestionIndex(): Int = preferences.securityQuestionIndex.first()

    override suspend fun securityAnswer(): String = preferences.securityAnswer.first()

    override suspend fun saveSecurityQuestion(index: Int, answer: String) =
        preferences.saveSecurityQuestion(index, answer)

    override suspend fun clearPin() = preferences.clearPin()
}
