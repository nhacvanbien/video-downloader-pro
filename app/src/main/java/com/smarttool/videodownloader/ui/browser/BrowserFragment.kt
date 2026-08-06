package com.smarttool.videodownloader.ui.browser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeScreen
import com.smarttool.videodownloader.feature.browser.presentation.BrowserHomeViewModel
import com.smarttool.videodownloader.feature.browser.domain.model.WebTab
import com.smarttool.videodownloader.feature.browser.domain.model.WebTabFactory
import com.smarttool.videodownloader.ui.guide.GuideActivity
import com.smarttool.videodownloader.ui.history.HistoryActivity
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.ads.showInterAll
import com.vimalcvs.materialrating.DialogManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.smarttool.videodownloader.ui.browser.webTab.WebTabActivity
import com.smarttool.videodownloader.ui.tab.TabsActivity

class BrowserFragment : Fragment() {

    private val viewModel: BrowserHomeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AppTheme {
                BrowserHomeScreen(
                    state = state,
                    sites = viewModel.sites,
                    onQueryChange = viewModel::onQueryChange,
                    onSubmitQuery = {
                        if (state.query.isNotEmpty()) {
                            showInterAll {
                                newTab(state.query)
                                viewModel.clearQuery()
                            }
                        }
                    },
                    onOpenSite = { site -> showInterAll { newTab(site.url) } },
                    onOpenGuide = ::openGuide,
                    onOpenTabs = ::openTabs,
                    onOpenBookmarks = { openHistory(OPEN_BOOKMARK) },
                    onOpenHistory = { openHistory(OPEN_HISTORY) },
                )
            }
        }
    }

    private fun openGuide() {
        showInterAll {
            startActivityResult.launch(
                GuideActivity.newIntent(requireContext()).apply {
                    putExtra(EXTRA_OPEN, OPEN_HOME)
                },
            )
        }
    }

    private fun openTabs() {
        showInterAll {
            startActivityResult.launch(
                Intent(requireContext(), TabsActivity::class.java).apply {
                    putExtra(EXTRA_OPEN, OPEN_HOME)
                },
            )
        }
    }

    private fun openHistory(mode: String) {
        showInterAll {
            startActivityResult.launch(
                HistoryActivity.newIntent(requireContext(), mode).apply {
                    putExtra(EXTRA_START, OPEN_HOME)
                },
            )
        }
    }

    private fun newTab(input: String) {
        viewModel.openTab(WebTabFactory.createTabModelFromInput(input)) {
            openWebTab(WebTabFactory.createWebTabFromInput(input))
        }
    }

    private fun openWebTab(webTab: WebTab) {
        val intent = Intent(requireContext(), WebTabActivity::class.java)
        intent.putExtras(Bundle().apply { putSerializable(EXTRA_WEBTAB, webTab) })
        intent.putExtra(EXTRA_OPEN, OPEN_HOME)
        startActivityResult.launch(intent)
    }

    /** Returning from any of these flows is a natural moment to ask for a rating. */
    private val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        activity?.let { DialogManager.showRatingAfterDoFunction(it, AppConstant.FEEDBACK_EMAIL) }
    }

    private companion object {
        const val EXTRA_OPEN = "open"
        const val EXTRA_START = "start"
        const val EXTRA_WEBTAB = "webtab"
        const val OPEN_HOME = "home"
        const val OPEN_BOOKMARK = "bookmark"
        const val OPEN_HISTORY = "history"
    }
}
