package com.smarttool.videodownloader.feature.browser.di

import com.smarttool.videodownloader.feature.browser.data.BrowserPreferencesRepositoryImpl
import com.smarttool.videodownloader.feature.browser.data.SearchEngineRepositoryImpl
import com.smarttool.videodownloader.feature.browser.domain.BrowserPreferencesRepository
import com.smarttool.videodownloader.feature.browser.domain.SearchEngineRepository
import com.smarttool.videodownloader.feature.browser.domain.usecase.DownloadImageUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetBrowserSettingsUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetPopularSitesUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetSearchEngineUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetVideoDetectionThresholdUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.ObserveSearchEngineUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetSearchEngineUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetShowVideoAlertUseCase
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val browserModule = module {
    single<BrowserPreferencesRepository> { BrowserPreferencesRepositoryImpl(get()) }
    single<SearchEngineRepository> { SearchEngineRepositoryImpl(get()) }
    factory { GetBrowserSettingsUseCase(get()) }
    factory { SetShowVideoAlertUseCase(get()) }
    factory { GetVideoDetectionThresholdUseCase(get()) }
    factory { DownloadImageUseCase(androidContext(), get()) }
    factory { ObserveSearchEngineUseCase(get()) }
    factory { GetSearchEngineUseCase(get()) }
    factory { SetSearchEngineUseCase(get()) }

    factory { GetPopularSitesUseCase() }
    viewModel { BrowserHomeViewModel(get(), get(), get(), get(), get()) }
}
