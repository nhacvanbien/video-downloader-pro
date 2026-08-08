package com.smarttool.videodownloader.feature.language.di

import com.smarttool.videodownloader.feature.language.data.LanguageRepositoryImpl
import com.smarttool.videodownloader.feature.language.domain.LanguageRepository
import com.smarttool.videodownloader.feature.language.domain.usecase.ApplyLanguageUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetLanguagesUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetLocalizedStringUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetSystemLanguageUseCase
import com.smarttool.videodownloader.feature.language.presentation.LanguageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val languageModule = module {
    single<LanguageRepository> { LanguageRepositoryImpl(get()) }
    factory { GetLanguagesUseCase(get()) }
    factory { ApplyLanguageUseCase(get()) }
    factory { GetSystemLanguageUseCase() }
    factory { GetLocalizedStringUseCase(androidContext()) }
    viewModel { LanguageViewModel(get(), get()) }
}
