package com.smarttool.videodownloader.feature.browser.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.smarttool.videodownloader.feature.browser.domain.FaviconUtils
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.feature.browser.domain.usecase.DownloadImageUseCase
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.domain.usecase.SaveHistoryEntryUseCase
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel
import com.smarttool.videodownloader.feature.tab.domain.usecase.CreateTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.GetSelectedTabUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.ObserveTabsUseCase
import com.smarttool.videodownloader.feature.tab.domain.usecase.OpenTabUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import timber.log.Timber

/**
 * Owns the browser tab's whole business state: navigation, history, the tab list count,
 * fullscreen flag, and image downloads. [WebTabViewHost] only holds the raw `WebView` and
 * forwards every callback here — nothing is decided outside this class.
 */
class WebTabViewModel(
    private val adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource,
    observeTabs: ObserveTabsUseCase,
    private val createTab: CreateTabUseCase,
    private val getSelectedTab: GetSelectedTabUseCase,
    private val openTab: OpenTabUseCase,
    private val saveHistoryEntry: SaveHistoryEntryUseCase,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val downloadImage: DownloadImageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebTabPipelineContract.State())
    val uiState: StateFlow<WebTabPipelineContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<WebTabPipelineContract.Effect>(Channel.BUFFERED)
    val effect: Flow<WebTabPipelineContract.Effect> = _effect.receiveAsFlow()

    private var historyItemCurrent: HistoryItem? = null
    private var lastSavedHistoryUrl: String = ""
    private var lastSavedTitleHistory: String = ""

    init {
        viewModelScope.launch {
            observeTabs().collect { tabs ->
                _uiState.update { it.copy(tabCount = tabs.size) }
            }
        }
    }

    fun isAd(url: String): Boolean = adBlockHostsRemoteDataSource.isAds(url)

    /** One-time session bootstrap: make sure a tab exists to select. Not a repeatable UI action. */
    suspend fun ensureSelectedTab(url: String) {
        try {
            if (getSelectedTab() == null) {
                createTab(TabModel(url = url, isSelected = true))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert initial tab model")
        }
    }

    fun onEvent(event: WebTabPipelineContract.Event) {
        when (event) {
            WebTabPipelineContract.Event.SetListHost -> viewModelScope.launch {
                adBlockHostsRemoteDataSource.setListHost()
            }

            is WebTabPipelineContract.Event.FinishPage -> {
                setTabTextInput(event.url, true)
                _uiState.update { it.copy(isShowProgress = false) }
            }

            is WebTabPipelineContract.Event.StartPage -> applyStartPage(event.url, event.title)

            is WebTabPipelineContract.Event.UpdateVisitedHistory -> applyUpdateVisitedHistory(event.url)

            is WebTabPipelineContract.Event.ChangeTabFocus -> changeTabFocus(event.focused)

            is WebTabPipelineContract.Event.SetDownloadDialogShown ->
                _uiState.update { it.copy(isDownloadDialogShown = event.shown) }

            is WebTabPipelineContract.Event.OpenPage -> {
                if (event.input.isNotEmpty()) {
                    changeTabFocus(false)
                    val tab = WebTabFactory.createWebTabFromInput(event.input)
                    _effect.trySend(WebTabPipelineContract.Effect.OpenPage(tab))
                }
            }

            is WebTabPipelineContract.Event.LoadPage -> {
                if (event.input.isNotEmpty()) {
                    changeTabFocus(false)
                    val tab = WebTabFactory.createWebTabFromInput(event.input)
                    setTabTextInput(tab.getUrl())
                    _effect.trySend(WebTabPipelineContract.Effect.LoadPage(tab))
                }
            }

            is WebTabPipelineContract.Event.SetTabTextInput ->
                setTabTextInput(event.input, event.force)

            is WebTabPipelineContract.Event.CloseTab -> Unit

            is WebTabPipelineContract.Event.PageReload -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView?.reload()
            }

            is WebTabPipelineContract.Event.PageStop -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = false) }
                event.webView?.stopLoading()
            }

            is WebTabPipelineContract.Event.GoBack -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView.goBack()
            }

            is WebTabPipelineContract.Event.GoForward -> {
                changeTabFocus(false)
                _uiState.update { it.copy(isShowProgress = true) }
                event.webView.goForward()
            }

            is WebTabPipelineContract.Event.SetProgress ->
                // Every caller pairs this with "show progress while not yet at 100%";
                // folded in here since nothing ever sets progress without it.
                _uiState.update {
                    it.copy(progress = event.progress, isShowProgress = event.progress != 100)
                }

            is WebTabPipelineContract.Event.NotifyPageOpened ->
                _effect.trySend(WebTabPipelineContract.Effect.OpenPage(event.tab))

            is WebTabPipelineContract.Event.UrlChange ->
                _uiState.update { it.copy(tabUrl = event.url) }

            is WebTabPipelineContract.Event.RefreshNavState ->
                _uiState.update {
                    it.copy(canGoBack = event.canGoBack, canGoForward = event.canGoForward)
                }

            is WebTabPipelineContract.Event.SetFullscreen ->
                _uiState.update { it.copy(isFullscreen = event.isFullscreen) }

            is WebTabPipelineContract.Event.OnPageStarted -> onPageStarted(event)

            is WebTabPipelineContract.Event.PageVisited ->
                onPageVisited(event.url, event.viewTitle, event.userAgent)

            WebTabPipelineContract.Event.SaveUrlToHistoryBookmark -> saveUrlToHistoryBookmark()

            is WebTabPipelineContract.Event.DownloadImage -> onDownloadImage(event.imageUrl)
        }
    }

    private fun changeTabFocus(focused: Boolean) {
        _uiState.update { it.copy(isTabInputFocused = focused) }
        _effect.trySend(WebTabPipelineContract.Effect.ChangeTabFocus(focused))
    }

    private fun setTabTextInput(input: String?, force: Boolean = false) {
        if (input.isNullOrEmpty()) return
        if (!_uiState.value.isTabInputFocused || force) {
            _uiState.update { it.copy(tabUrl = input) }
        }
    }

    private fun applyStartPage(url: String, title: String?) {
        setTabTextInput(url)
        _uiState.update { it.copy(isShowProgress = true, currentTitle = title, tabUrl = url) }
    }

    private fun applyUpdateVisitedHistory(url: String) {
        if (url.startsWith("http")) {
            setTabTextInput(url)
            _uiState.update { it.copy(isShowProgress = true, tabUrl = url) }
        }
    }

    private fun onPageStarted(event: WebTabPipelineContract.Event.OnPageStarted) {
        applyStartPage(event.url, event.viewTitle)

        val faviconBytes = FaviconUtils.bitmapToBytes(event.favicon)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                upsertSelectedTab(event.url, faviconBytes)
            } catch (e: Exception) {
                Timber.w(e, "Failed to update selected tab favicon")
            }
        }
    }

    /**
     * Mirrors the pre-Contract `doUpdateVisitedHistory`: the outer dedup check here (and
     * the inner one inside [saveUrlToHistory]) both key off [lastSavedHistoryUrl] — same
     * field, checked twice, exactly as it was before this moved out of the Controller.
     */
    private fun onPageVisited(url: String, viewTitle: String?, userAgent: String?) {
        if (lastSavedHistoryUrl == url) return

        val title = _uiState.value.currentTitle

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val icon = try {
                    FaviconUtils.getEncodedFaviconFromUrl(okHttpProxyClient.getProxyOkHttpClient(), url)
                } catch (e: Throwable) {
                    null
                }
                val outputFavicon = FaviconUtils.bitmapToBytes(icon)

                saveUrlToHistory(url, icon, viewTitle ?: title)

                _effect.trySend(
                    WebTabPipelineContract.Effect.NotifyPageStarted(url, userAgent.orEmpty()),
                )
                applyUpdateVisitedHistory(url)

                upsertSelectedTab(url, outputFavicon)
            } catch (e: Exception) {
                Timber.w(e, "Failed to update tab favicon")
            }
        }
    }

    private suspend fun upsertSelectedTab(url: String, favicon: ByteArray?) {
        val selected = getSelectedTab()
        if (selected == null) {
            createTab(TabModel(url = url, isSelected = true, favicon = favicon))
        } else {
            selected.url = url
            selected.favicon = favicon
            openTab(selected)
        }
    }

    private suspend fun saveUrlToHistory(url: String, favicon: Bitmap?, title: String?) {
        val isTitleEmpty = title?.trim()?.isEmpty() == true

        val shouldSave = !isTitleEmpty &&
            lastSavedTitleHistory != title &&
            lastSavedHistoryUrl != url &&
            url.isNotEmpty() &&
            !url.contains("about:blank")

        if (!shouldSave) return

        lastSavedHistoryUrl = url
        lastSavedTitleHistory = title.orEmpty()

        yield()

        val item = HistoryItem(
            url = url,
            favicon = FaviconUtils.bitmapToBytes(favicon),
            title = title,
        )

        historyItemCurrent = item
        saveHistoryEntry(item.toEntry())
    }

    private fun saveUrlToHistoryBookmark() {
        val item = historyItemCurrent ?: return

        item.isBookmark = true
        viewModelScope.launch(Dispatchers.IO) {
            saveHistoryEntry(item.toEntry())
            _effect.trySend(WebTabPipelineContract.Effect.BookmarkSaved)
        }
    }

    private fun onDownloadImage(imageUrl: String) {
        viewModelScope.launch {
            val file = downloadImage(imageUrl)
            _effect.trySend(
                if (file != null) {
                    WebTabPipelineContract.Effect.ImageDownloaded(file.absolutePath)
                } else {
                    WebTabPipelineContract.Effect.ImageDownloadFailed
                },
            )
        }
    }

    private fun HistoryItem.toEntry() = HistoryEntry(
        id = id,
        title = title.orEmpty(),
        url = url,
        datetime = datetime,
        isBookmark = isBookmark,
        favicon = favicon,
    )
}
