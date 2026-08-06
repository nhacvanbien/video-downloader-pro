package com.smarttool.videodownloader.ui.tab

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.tab.presentation.TabsScreen
import com.smarttool.videodownloader.feature.tab.presentation.TabsViewModel
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.core.ads.showInterAll
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.smarttool.videodownloader.ui.browser.webTab.WebTabActivity

class TabsActivity : BaseComposeActivity() {

    private val viewModel: TabsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val tabs by viewModel.tabs.collectAsStateWithLifecycle()

                TabsScreen(
                    tabs = tabs,
                    onOpenTab = { tab ->
                        showInterAll {
                            if (tab.url.isEmpty()) return@showInterAll
                            viewModel.onOpenTab(tab) {
                                openWebTab(WebTabFactory.createWebTabFromInput(tab.url))
                            }
                        }
                    },
                    onDeleteTab = viewModel::onDeleteTab,
                    onCloseAll = viewModel::onCloseAll,
                    onNewTab = ::newTab,
                )
            }
        }
    }

    private fun newTab() {
        val input = DEFAULT_TAB_URL
        viewModel.onCreateTab(WebTabFactory.createTabModelFromInput(input)) {
            openWebTab(WebTabFactory.createWebTabFromInput(input))
        }
    }

    private fun openWebTab(webTab: WebTab) {
        val intent = Intent(this, WebTabActivity::class.java)
        intent.putExtra("open", "tab")
        intent.putExtras(Bundle().apply { putSerializable("webtab", webTab) })
        startActivity(intent)
    }

    companion object {
        private const val DEFAULT_TAB_URL = "https://www.google.com"
    }
}
