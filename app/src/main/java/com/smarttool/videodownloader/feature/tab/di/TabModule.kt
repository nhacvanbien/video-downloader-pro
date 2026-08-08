package com.smarttool.videodownloader.feature.tab.di

import com.smarttool.videodownloader.feature.tab.data.TabsRepositoryImpl
import com.smarttool.videodownloader.feature.tab.domain.TabsRepository
import com.smarttool.videodownloader.feature.tab.domain.usecase.ClearTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.DeleteTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.OpenTabUseCase
import com.smarttool.videodownloader.feature.tab.presentation.TabsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val tabModule = module {
    single<TabsRepository> { TabsRepositoryImpl(get()) }
    factory { ObserveTabsUseCase(get()) }
    factory { DeleteTabUseCase(get()) }
    factory { ClearTabsUseCase(get()) }
    factory { OpenTabUseCase(get()) }
    factory { CreateTabUseCase(get()) }
    viewModel { TabsViewModel(get(), get(), get(), get(), get()) }
}
