package com.smarttool.videodownloader.feature.browser.presentation

import android.webkit.WebView
import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab

/**
 * Named `Pipeline` rather than `WebTabContract` to keep it distinct from
 * [WebTabController]'s own `WebTabUiState` — this is the tab-loading/navigation
 * pipeline [WebTabViewModel] drives, not the chrome the controller renders.
 */
interface WebTabPipelineContract {
    data class State(
        val isTabInputFocused: Boolean = false,
        val isDownloadDialogShown: Boolean = false,
        val isShowProgress: Boolean = true,
        val progress: Int = 0,
        val currentTitle: String? = "",
        val userAgent: String = "",
        val tabUrl: String = "",
    ) : UiState

    sealed interface Event : UiEvent {
        data object SetListHost : Event
        data class FinishPage(val url: String) : Event
        data class StartPage(val url: String, val title: String?) : Event
        data class UpdateVisitedHistory(
            val url: String,
            val title: String?,
            val userAgent: String?,
        ) : Event

        data class ChangeTabFocus(val focused: Boolean) : Event
        data class SetDownloadDialogShown(val shown: Boolean) : Event
        data class OpenPage(val input: String) : Event
        data class LoadPage(val input: String) : Event
        data class SetTabTextInput(val input: String?, val force: Boolean = false) : Event

        /** No-op today — mirrors `closeTab`'s pre-Contract `closePageEvent`, which had no observer either. */
        data class CloseTab(val tab: WebTab) : Event

        data class PageReload(val webView: WebView?) : Event
        data class PageStop(val webView: WebView?) : Event
        data class GoBack(val webView: WebView) : Event
        data class GoForward(val webView: WebView) : Event
        data class SetProgress(val progress: Int) : Event

        /** A tab built by the host (e.g. from `window.open()`) that just needs broadcasting. */
        data class NotifyPageOpened(val tab: WebTab) : Event
    }

    sealed interface Effect : UiEffect {
        data class ChangeTabFocus(val focused: Boolean) : Effect
        data class OpenPage(val tab: WebTab) : Effect
        data class LoadPage(val tab: WebTab) : Effect
    }
}
