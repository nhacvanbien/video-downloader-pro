package com.smarttool.videodownloader.feature.main.di

import com.smarttool.videodownloader.feature.main.data.AppUsageRepositoryImpl
import com.smarttool.videodownloader.feature.main.domain.AppUsageRepository
import com.smarttool.videodownloader.feature.main.domain.usecase.RecordAppExitUseCase
import com.smarttool.videodownloader.feature.main.presentation.MainViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mainModule = module {
    single<AppUsageRepository> { AppUsageRepositoryImpl(get()) }
    factory { RecordAppExitUseCase(get()) }
    viewModel { MainViewModel(get()) }
}
