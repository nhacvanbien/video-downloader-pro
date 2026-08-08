package com.smarttool.videodownloader.feature.browser.presentation

import android.graphics.Bitmap
import android.webkit.WebView
import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab

/**
 * Named `Pipeline` rather than `WebTabContract` to keep it distinct from the browser
 * chrome concept living in the same package — this is the tab-loading/navigation/history
 * pipeline [WebTabViewModel] drives end to end; [WebTabViewHost] only holds the raw
 * `WebView` and forwards every callback here.
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
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val tabCount: Int = 0,
        /** True while a page element is playing fullscreen; the browser chrome hides. */
        val isFullscreen: Boolean = false,
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

        /** Typed directly into the address bar; distinct from navigation-driven [State.tabUrl] updates. */
        data class UrlChange(val url: String) : Event

        /** After a nav action or page load, so [State.canGoBack]/[State.canGoForward] stay live. */
        data class RefreshNavState(val canGoBack: Boolean, val canGoForward: Boolean) : Event

        data class SetFullscreen(val isFullscreen: Boolean) : Event

        /** A page just started loading — upserts the selected tab model's favicon/url. */
        data class OnPageStarted(val url: String, val viewTitle: String?, val favicon: Bitmap?) : Event

        /** History-worthy navigation, deduped internally against the last URL/title saved. */
        data class PageVisited(val url: String, val viewTitle: String?, val userAgent: String) : Event

        data object SaveUrlToHistoryBookmark : Event
        data class DownloadImage(val imageUrl: String) : Event
    }

    sealed interface Effect : UiEffect {
        data class ChangeTabFocus(val focused: Boolean) : Effect
        data class OpenPage(val tab: WebTab) : Effect
        data class LoadPage(val tab: WebTab) : Effect

        /** [Event.PageVisited] passed its dedup check — the detector should start checking too. */
        data class NotifyPageStarted(val url: String, val userAgent: String) : Effect

        data object BookmarkSaved : Effect
        data class ImageDownloaded(val path: String) : Effect
        data object ImageDownloadFailed : Effect
    }
}
