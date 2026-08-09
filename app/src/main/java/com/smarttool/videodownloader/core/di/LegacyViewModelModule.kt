package com.smarttool.videodownloader.core.di

import com.smarttool.videodownloader.feature.browser.presentation.BrowserSettingsViewModel
import com.smarttool.videodownloader.feature.browser.presentation.DetectedVideosTabViewModel
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewModel
import org.koin.android.ext.koin.androidContext
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
    viewModel { BrowserSettingsViewModel(get(), get()) }
    viewModel { WebTabViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DetectedVideosTabViewModel(get(), get(), get(), get(), androidContext()) }
}
