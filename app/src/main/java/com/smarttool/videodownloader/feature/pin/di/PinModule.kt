package com.smarttool.videodownloader.feature.pin.di

import com.smarttool.videodownloader.feature.pin.data.PinRepositoryImpl
import com.smarttool.videodownloader.feature.pin.domain.PinRepository
import com.smarttool.videodownloader.feature.pin.domain.usecase.ResetPinUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.SavePinUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.SaveSecurityQuestionUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.VerifyPinUseCase
import com.smarttool.videodownloader.feature.pin.domain.usecase.VerifySecurityAnswerUseCase
import com.smarttool.videodownloader.feature.pin.presentation.PinViewModel
import com.smarttool.videodownloader.feature.pin.presentation.SecurityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val pinModule = module {
    single<PinRepository> { PinRepositoryImpl(get()) }
    factory { VerifyPinUseCase(get()) }
    factory { SavePinUseCase(get()) }
    factory { VerifySecurityAnswerUseCase(get()) }
    factory { SaveSecurityQuestionUseCase(get()) }
    factory { ResetPinUseCase(get()) }
    viewModel { PinViewModel(get(), get(), get()) }
    viewModel { SecurityViewModel(get(), get(), get()) }
}
