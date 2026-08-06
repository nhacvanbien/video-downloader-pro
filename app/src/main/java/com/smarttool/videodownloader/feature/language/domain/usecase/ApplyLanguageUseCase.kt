package com.smarttool.videodownloader.feature.language.domain.usecase

import com.smarttool.videodownloader.feature.language.domain.LanguageRepository

class ApplyLanguageUseCase(private val repository: LanguageRepository) {
    operator fun invoke(code: String, markStartShown: Boolean) {
        repository.persistLanguage(code)
        if (markStartShown) {
            repository.markStartLanguageShown()
        }
    }
}
