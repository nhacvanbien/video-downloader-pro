package com.smarttool.videodownloader.feature.history.di

import com.smarttool.videodownloader.feature.history.data.HistoryRepositoryImpl
import com.smarttool.videodownloader.feature.history.domain.repository.HistoryRepository
import com.smarttool.videodownloader.feature.history.domain.usecase.AddBookmarkUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ClearHistoryUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.DeleteHistoryEntryUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ObserveBookmarksUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.ObserveHistoryUseCase
import com.smarttool.videodownloader.feature.history.domain.usecase.SaveHistoryEntryUseCase
import com.smarttool.videodownloader.feature.history.presentation.HistoryMode
import com.smarttool.videodownloader.feature.history.presentation.HistoryViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val historyModule = module {
    single<HistoryRepository> { HistoryRepositoryImpl(get(), get(), Dispatchers.IO) }

    factory { ObserveHistoryUseCase(get()) }
    factory { ObserveBookmarksUseCase(get()) }
    factory { AddBookmarkUseCase(get()) }
    factory { DeleteHistoryEntryUseCase(get()) }
    factory { ClearHistoryUseCase(get()) }
    factory { SaveHistoryEntryUseCase(get()) }

    viewModel { (mode: HistoryMode) ->
        HistoryViewModel(
            mode = mode,
            observeHistory = get(),
            observeBookmarks = get(),
            addBookmark = get(),
            deleteEntry = get(),
            clearHistory = get(),
        )
    }
}
