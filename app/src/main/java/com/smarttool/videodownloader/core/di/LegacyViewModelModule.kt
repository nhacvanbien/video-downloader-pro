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
 * The WebView video-detection pipeline and its collaborators, carried over from the
 * pre-migration `ui/` packages.
 *
 * They are resolved through a private [ScopedViewModelStore] per host rather than the
 * Activity store: the browser and the processing screen each make themselves the
 * pipeline's owner, so one shared instance would cross-wire them.
 */
val legacyViewModelModule = module {
    viewModel { TabModelViewModel(get()) }
    viewModel { BrowserSettingsViewModel(get()) }
    viewModel { WebTabViewModel(get()) }
    viewModel { VideoDetectionAlgVModel(get(), get()) }
    viewModel { PrivateVideoViewModel(get(), get()) }
    viewModel { DetectedVideosTabViewModel(get(), get(), get(), get()) }
}
