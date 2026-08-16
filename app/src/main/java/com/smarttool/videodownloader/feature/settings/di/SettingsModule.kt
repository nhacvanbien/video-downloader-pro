package com.smarttool.videodownloader.feature.settings.di

import com.smarttool.videodownloader.feature.settings.data.SettingsRepositoryImpl
import com.smarttool.videodownloader.feature.settings.domain.SettingsRepository
import com.smarttool.videodownloader.feature.settings.domain.usecase.GetSettingsUseCase
import com.smarttool.videodownloader.feature.settings.domain.usecase.SetDownloadLocationSubfolderUseCase
import com.smarttool.videodownloader.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get()) }
    factory { GetSettingsUseCase(get()) }
    factory { SetDownloadLocationSubfolderUseCase(get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
