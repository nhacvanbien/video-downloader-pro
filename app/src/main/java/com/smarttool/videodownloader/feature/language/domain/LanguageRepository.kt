package com.smarttool.videodownloader.feature.language.domain

import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage

interface LanguageRepository {
    fun availableLanguages(): List<AppLanguage>

    fun currentLanguageCode(): String

    fun persistLanguage(code: String)

    fun markStartLanguageShown()
}
