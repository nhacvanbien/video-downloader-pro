package com.smarttool.videodownloader.core.di

import com.smarttool.videodownloader.feature.browser.presentation.BrowserSettingsViewModel
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.VideoDetectionAlgVModel
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import com.smarttool.videodownloader.feature.library.presentation.PrivateVideoViewModel
import com.smarttool.videodownloader.feature.tab.presentation.TabModelViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModels that still belong to the pre-migration `ui/` packages — the WebView
 * video-detection pipeline and its collaborators. They are Koin-wired so Hilt can be
 * removed; splitting them into `feature/` packages is a separate refactor.
 */
val legacyViewModelModule = module {
    viewModel { TabModelViewModel(get()) }
    viewModel { BrowserSettingsViewModel(get()) }
    viewModel { WebTabViewModel(get(), get()) }
    viewModel { VideoDetectionAlgVModel(get(), get()) }
    viewModel { PrivateVideoViewModel(get(), get()) }
    viewModel { DetectedVideosTabViewModel(get(), get(), get(), get()) }
}
