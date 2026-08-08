package com.smarttool.videodownloader.feature.browser.presentation

import android.webkit.WebView
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.SingleLiveEvent
import com.smarttool.videodownloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory

class WebTabViewModel(
    private val adBlockHostsRemoteDataSource: AdBlockHostsRemoteDataSource,
) : ViewModel() {
    val isTabInputFocused = ObservableBoolean(false)
    val changeTabFocusEvent = SingleLiveEvent<Boolean>()
    val isDownloadDialogShown = ObservableBoolean(false)
    val isShowProgress = ObservableBoolean(true)
    val progress = ObservableInt(0)
    val progressIcon = ObservableInt(R.drawable.ic_reload)

    val currentTitle = ObservableField("")
    var userAgent = ObservableField("")

    // This events from BrowserFragment
    val openPageEvent = SingleLiveEvent<WebTab>()
    val closePageEvent = SingleLiveEvent<WebTab>()

    val loadPageEvent = SingleLiveEvent<WebTab>()

    val tabUrl = ObservableField("")

    suspend fun setListHost() {
        adBlockHostsRemoteDataSource.setListHost()
    }

    fun isAd(url: String): Boolean {
        return adBlockHostsRemoteDataSource.isAds(url)
    }

    fun finishPage(url: String) {
        setTabTextInput(url, true)
        isShowProgress.set(false)
    }

    fun onStartPage(url: String, title: String?) {
        setTabTextInput(url)
        isShowProgress.set(true)
        currentTitle.set(title)
        tabUrl.set(url)
    }

    fun onUpdateVisitedHistory(url: String, title: String?, userAgent: String?) {
        if (url.startsWith("http")) {
            setTabTextInput(url)
            isShowProgress.set(true)
            tabUrl.set(url)
        }
    }

    fun changeTabFocus(isFocus: Boolean) {
        this.isTabInputFocused.set(isFocus)
        changeTabFocusEvent.value = isFocus
    }


    fun openPage(input: String) {
        if (input.isNotEmpty()) {
            changeTabFocus(false)
            openPageEvent.value = WebTabFactory.createWebTabFromInput(input)
        }
    }

    fun loadPage(input: String) {
        if (input.isNotEmpty()) {
            changeTabFocus(false)
            val tab = WebTabFactory.createWebTabFromInput(input)
            setTabTextInput(tab.getUrl())

            loadPageEvent.value = tab
        }
    }

    fun setTabTextInput(input: String?, isForce: Boolean = false) {
        if (input.isNullOrEmpty()) {
            return
        }

        if (!isTabInputFocused.get() || isForce) {
            tabUrl.set(input)
        }
    }

    fun getTabTextInput(): ObservableField<String> {
        return tabUrl
    }

    fun closeTab(webTab: WebTab) {
        closePageEvent.value = webTab
    }

    fun onPageReload(urlLoader: WebView?) {
        changeTabFocus(false)
        isShowProgress.set(true)

        urlLoader?.reload()
    }

    fun onPageStop(urlLoader: WebView?) {
        changeTabFocus(false)
        isShowProgress.set(false)
        urlLoader?.stopLoading()
    }

    fun onGoBack(webView: WebView) {
        changeTabFocus(false)
        isShowProgress.set(true)
        webView.goBack()
    }

    fun onGoForward(webView: WebView) {
        changeTabFocus(false)
        isShowProgress.set(true)
        webView.goForward()
    }

    fun setProgress(newProgress: Int) {
        progress.set(newProgress)
    }
}
