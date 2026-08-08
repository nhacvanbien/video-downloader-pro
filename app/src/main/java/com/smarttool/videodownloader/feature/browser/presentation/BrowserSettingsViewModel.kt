package com.smarttool.videodownloader.feature.browser.presentation

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.browser.domain.usecase.GetBrowserSettingsUseCase
import com.smarttool.videodownloader.feature.browser.domain.usecase.SetShowVideoAlertUseCase
import kotlinx.coroutines.launch

/**
 * The two browser preferences the browser itself still reads: whether to alert on a
 * detected video, and whether to lock the page to portrait.
 *
 * Everything else this used to hold — thread counts, storage folder, desktop mode,
 * ad-blocker and dark-mode toggles — backed a settings screen that no longer exists;
 * the live settings screen is `feature/settings`. Those members were removed rather
 * than left as an API nothing calls.
 */
class BrowserSettingsViewModel(
    private val getBrowserSettings: GetBrowserSettingsUseCase,
    private val setShowVideoAlert: SetShowVideoAlertUseCase,
) : ViewModel() {

    val isLockPortrait = ObservableBoolean(false)

    private val isShowVideoAlert = ObservableBoolean(true)

    init {
        viewModelScope.launch {
            val settings = getBrowserSettings()
            isShowVideoAlert.set(settings.showVideoAlert)
            isLockPortrait.set(settings.lockPortrait)
        }
    }

    fun getVideoAlertState(): ObservableBoolean = isShowVideoAlert

    fun setShowVideoAlertOff() {
        isShowVideoAlert.set(false)
        viewModelScope.launch { setShowVideoAlert(false) }
    }
}
