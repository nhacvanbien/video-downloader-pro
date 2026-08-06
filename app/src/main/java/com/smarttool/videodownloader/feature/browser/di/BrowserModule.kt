package com.smarttool.videodownloader.feature.browser.di

import com.smarttool.videodownloader.feature.browser.domain.usecase.GetPopularSitesUseCase
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val browserModule = module {
    factory { GetPopularSitesUseCase() }
    viewModel { BrowserHomeViewModel(get(), get(), get()) }
}
