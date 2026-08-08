package com.smarttool.videodownloader.feature.language.data

import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.feature.language.domain.LanguageRepository
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.language.domain.model.SUPPORTED_LANGUAGES
import kotlinx.coroutines.flow.first

class LanguageRepositoryImpl(
    private val preferences: AppPreferencesDataSource,
) : LanguageRepository {

    override fun availableLanguages(): List<AppLanguage> = SUPPORTED_LANGUAGES

    override suspend fun currentLanguageCode(): String = preferences.currentLanguage.first()

    override suspend fun persistLanguage(code: String) = preferences.setCurrentLanguage(code)

    override suspend fun markStartLanguageShown() = preferences.setShowedStartLanguage(true)
}
