package com.smarttool.videodownloader.feature.rating.di

import com.smarttool.videodownloader.feature.rating.data.RatingPromptRepositoryImpl
import com.smarttool.videodownloader.feature.rating.domain.repository.RatingPromptRepository
import com.smarttool.videodownloader.feature.rating.domain.usecase.MarkRatingPromptShownUseCase
import com.smarttool.videodownloader.feature.rating.domain.usecase.ShouldPromptRatingAfterDownloadUseCase
import com.smarttool.videodownloader.feature.rating.presentation.RatingPromptController
import org.koin.dsl.module

val ratingModule = module {
    single<RatingPromptRepository> { RatingPromptRepositoryImpl(get()) }

    factory { ShouldPromptRatingAfterDownloadUseCase(get()) }
    factory { MarkRatingPromptShownUseCase(get()) }

    single { RatingPromptController(get(), get()) }
}
