package com.smarttool.videodownloader.feature.library.di

import com.smarttool.videodownloader.feature.downloads.data.AndroidDownloaderGateway
import com.smarttool.videodownloader.feature.downloads.data.DownloadsRepositoryImpl
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.feature.downloads.domain.DownloadsRepository
import com.smarttool.videodownloader.feature.downloads.domain.usecase.CancelDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.GetVideoFormatOptionsUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ObserveActiveDownloadsUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.PauseDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.ResumeDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.SanitizeFileNameUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StartDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.domain.usecase.StopAndSaveDownloadUseCase
import com.smarttool.videodownloader.feature.downloads.presentation.DetectedVideoUiMapper
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingViewModel
import com.smarttool.videodownloader.feature.library.data.LibraryPreferencesRepositoryImpl
import com.smarttool.videodownloader.feature.library.data.MediaLibraryRepositoryImpl
import com.smarttool.videodownloader.feature.library.domain.LibraryPreferencesRepository
import com.smarttool.videodownloader.feature.library.domain.MediaLibraryRepository
import com.smarttool.videodownloader.feature.library.domain.usecase.DeleteMediaUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.GetSortTypeUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.ObserveLibraryUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.ObservePrivateLibraryUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.PruneMissingFilesUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.RenameMediaUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.SetMediaPrivateUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.SetSortTypeUseCase
import com.smarttool.videodownloader.feature.library.presentation.LibraryViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Distinguishes the normal library ViewModel from the PIN-protected one. */
val PrivateLibrary = named("privateLibrary")

val libraryModule = module {
    single<MediaLibraryRepository> { MediaLibraryRepositoryImpl(get(), get()) }
    single<LibraryPreferencesRepository> { LibraryPreferencesRepositoryImpl(get()) }
    factory { ObserveLibraryUseCase(get()) }
    factory { ObservePrivateLibraryUseCase(get()) }
    factory { DeleteMediaUseCase(get()) }
    factory { RenameMediaUseCase(get()) }
    factory { SetMediaPrivateUseCase(get()) }
    factory { PruneMissingFilesUseCase(get()) }
    factory { GetSortTypeUseCase(get()) }
    factory { SetSortTypeUseCase(get()) }

    single<DownloaderGateway> { AndroidDownloaderGateway(androidContext()) }
    single<DownloadsRepository> { DownloadsRepositoryImpl(get(), get()) }
    factory { ObserveActiveDownloadsUseCase(get()) }
    factory { StartDownloadUseCase(get(), get()) }
    factory { PauseDownloadUseCase(get(), get()) }
    factory { ResumeDownloadUseCase(get(), get()) }
    factory { CancelDownloadUseCase(get(), get()) }
    factory { StopAndSaveDownloadUseCase(get()) }
    factory { GetVideoFormatOptionsUseCase() }
    factory { SanitizeFileNameUseCase() }
    factory { DetectedVideoUiMapper(get()) }

    viewModel { ProcessingViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { LibraryViewModel(false, get(), get(), get(), get(), get(), get(), get()) }
    viewModel(PrivateLibrary) {
        LibraryViewModel(true, get(), get(), get(), get(), get(), get(), get())
    }
}
