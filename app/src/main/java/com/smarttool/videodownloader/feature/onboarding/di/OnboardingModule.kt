package com.smarttool.videodownloader.feature.onboarding.di

import com.smarttool.videodownloader.feature.intro.presentation.IntroViewModel
import com.smarttool.videodownloader.feature.onboarding.data.OnboardingRepositoryImpl
import com.smarttool.videodownloader.feature.onboarding.domain.OnboardingRepository
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.CompletePermissionStepUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.GetAppEntryPointUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.IsStartLanguageShownUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.MarkOnboardingShownUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.ShouldShowPermissionStepUseCase
import com.smarttool.videodownloader.feature.permission.presentation.PermissionViewModel
import com.smarttool.videodownloader.feature.splash.presentation.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** First-run state, plus the three screens that read or write it. */
val onboardingModule = module {
    single<OnboardingRepository> { OnboardingRepositoryImpl(get()) }
    factory { GetAppEntryPointUseCase(get()) }
    factory { IsStartLanguageShownUseCase(get()) }
    factory { MarkOnboardingShownUseCase(get()) }
    factory { ShouldShowPermissionStepUseCase(get()) }
    factory { CompletePermissionStepUseCase(get()) }

    viewModel { SplashViewModel(get(), get()) }
    viewModel { IntroViewModel(get(), get()) }
    viewModel { PermissionViewModel(get(), get()) }
}
