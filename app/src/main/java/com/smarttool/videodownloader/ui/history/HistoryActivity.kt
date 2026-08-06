package com.smarttool.videodownloader.ui.history

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.NativeAdContainer
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.data.repository.TabModelRepository
import com.smarttool.videodownloader.dialog.DialogAddBookmark
import com.smarttool.videodownloader.dialog.DialogConfirmDelete
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry
import com.smarttool.videodownloader.feature.history.presentation.HistoryMode
import com.smarttool.videodownloader.feature.history.presentation.HistoryScreen
import com.smarttool.videodownloader.feature.history.presentation.HistoryViewModel
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.SystemUtil
import com.smarttool.videodownloader.core.ads.showInterAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import com.smarttool.videodownloader.ui.browser.webTab.WebTabActivity

class HistoryActivity : AppCompatActivity() {

    private val mode: HistoryMode by lazy {
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_BOOKMARK) {
            HistoryMode.BOOKMARK
        } else {
            HistoryMode.HISTORY
        }
    }

    private val viewModel: HistoryViewModel by viewModel { parametersOf(mode) }

    private val tabModelRepository: TabModelRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        SystemUtil.setLocale(this)
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AppTheme {
                HistoryScreen(
                    state = state,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onSearchActiveChange = viewModel::onSearchActiveChange,
                    onEntryClick = { openInNewTab(it.url) },
                    onDeleteEntry = { showDeleteConfirmation(it) },
                    onClearHistory = viewModel::onClearHistory,
                    onAddBookmark = { showAddBookmarkDialog() },
                    adSlot = {
                        NativeAdContainer(
                            adUnitId = BuildConfig.NATIVE_SMALL_ALL,
                            layoutRes = R.layout.layout_native_ad_small_bottom,
                            adPlacement = "native_history",
                            canShowAds = AdsConstant.showNativeSmallAll,
                        )
                    },
                )
            }
        }
    }

    private fun showDeleteConfirmation(entry: HistoryEntry) {
        DialogConfirmDelete(this) { viewModel.onDeleteEntry(entry) }.show()
    }

    private fun showAddBookmarkDialog() {
        DialogAddBookmark(this) { name, url -> viewModel.onAddBookmark(name, url) }.show()
    }

    private fun openInNewTab(url: String) {
        val webTab = WebTabFactory.createWebTabFromInput(url)
        val tabModel = WebTabFactory.createTabModelFromInput(url)

        lifecycleScope.launch(Dispatchers.IO) {
            tabModelRepository.insertTabModel(tabModel)

            withContext(Dispatchers.Main) {
                val intent = Intent(this@HistoryActivity, WebTabActivity::class.java)
                intent.putExtras(Bundle().apply { putSerializable("webtab", webTab) })
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        showInterAll {
            super.onBackPressed()
        }
    }

    companion object {
        private const val EXTRA_OPEN = "open"
        private const val OPEN_BOOKMARK = "bookmark"

        fun newIntent(context: Context, open: String = ""): Intent {
            val intent = Intent(context, HistoryActivity::class.java)
            intent.putExtra(EXTRA_OPEN, open)
            return intent
        }
    }
}
