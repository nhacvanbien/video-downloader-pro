package com.smarttool.videodownloader.feature.language.data

import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.language.domain.LanguageRepository
import com.smarttool.videodownloader.feature.language.domain.model.SUPPORTED_LANGUAGES
import com.smarttool.videodownloader.helper.PreferenceHelper

class LanguageRepositoryImpl(
    private val preferenceHelper: PreferenceHelper,
) : LanguageRepository {

    override fun availableLanguages(): List<AppLanguage> = SUPPORTED_LANGUAGES

    override fun currentLanguageCode(): String =
        preferenceHelper.getString(PreferenceHelper.PREF_CURRENT_LANGUAGE).orEmpty()

    override fun persistLanguage(code: String) {
        preferenceHelper.setString(PreferenceHelper.PREF_CURRENT_LANGUAGE, code)
    }

    override fun markStartLanguageShown() {
        preferenceHelper.setBoolean(PreferenceHelper.PREF_SHOWED_START_LANGUAGE, true)
    }
}
