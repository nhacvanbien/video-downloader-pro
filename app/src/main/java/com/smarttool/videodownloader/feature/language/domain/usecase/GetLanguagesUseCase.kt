package com.smarttool.videodownloader.feature.language.domain.usecase

import com.smarttool.videodownloader.feature.language.domain.LanguageRepository
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage

/**
 * Returns the language list with the currently selected entry hoisted to index 3,
 * matching the ordering the screen has always used to surface the likely choice.
 */
class GetLanguagesUseCase(private val repository: LanguageRepository) {
    operator fun invoke(selectedCode: String): List<AppLanguage> {
        val languages = repository.availableLanguages().toMutableList()
        val selected = languages.firstOrNull { it.code == selectedCode }
            ?: languages.firstOrNull { it.code == DEFAULT_CODE }

        selected?.let {
            languages.remove(it)
            languages.add(HOISTED_INDEX, it)
        }

        return languages
    }

    private companion object {
        const val DEFAULT_CODE = "en"
        const val HOISTED_INDEX = 3
    }
}
