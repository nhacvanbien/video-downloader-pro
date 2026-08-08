package com.smarttool.videodownloader.feature.browser.di

import com.smarttool.videodownloader.feature.browser.data.BrowserPreferencesRepositoryImpl
import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository
import com.smarttool.videodownloader.feature.browser.domain.usecase.DownloadImageUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetBrowserSettingsUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetPopularSitesUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetVideoDetectionThresholdUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetShowVideoAlertUseCase
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val browserModule = module {
    single<BrowserPreferencesRepository> { BrowserPreferencesRepositoryImpl(get()) }
    factory { GetBrowserSettingsUseCase(get()) }
    factory { SetShowVideoAlertUseCase(get()) }
    factory { GetVideoDetectionThresholdUseCase(get()) }
    factory { DownloadImageUseCase(androidContext(), get()) }

    factory { GetPopularSitesUseCase() }
    viewModel { BrowserHomeViewModel(get(), get(), get()) }
}
