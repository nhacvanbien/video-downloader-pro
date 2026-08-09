package com.smarttool.videodownloader.feature.media.di

import com.smarttool.videodownloader.feature.media.data.MediaPreferencesRepositoryImpl
import com.smarttool.videodownloader.feature.media.domain.MediaPreferencesRepository
import com.smarttool.videodownloader.feature.media.domain.usecase.GetPlaybackSettingsUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetFillModeUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetLoopingUseCase
import com.smarttool.videodownloader.feature.media.domain.usecase.SetPlaybackSpeedUseCase
import com.smarttool.videodownloader.feature.media.presentation.MediaViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val mediaModule = module {
    single<MediaPreferencesRepository> { MediaPreferencesRepositoryImpl(get()) }
    factory { GetPlaybackSettingsUseCase(get()) }
    factory { SetPlaybackSpeedUseCase(get()) }
    factory { SetLoopingUseCase(get()) }
    factory { SetFillModeUseCase(get()) }
    viewModel { MediaViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
