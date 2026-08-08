package com.smarttool.videodownloader.feature.language.domain

import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage

interface LanguageRepository {
    fun availableLanguages(): List<AppLanguage>

    suspend fun currentLanguageCode(): String

    fun currentLanguageCodeBlocking(): String

    suspend fun persistLanguage(code: String)

    suspend fun markStartLanguageShown()
}
