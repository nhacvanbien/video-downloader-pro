package com.smarttool.videodownloader.feature.settings.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.AppConstant
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import com.vimalcvs.materialrating.DialogManager
import org.koin.androidx.compose.koinViewModel

/** The Settings tab. */
@Composable
fun SettingsRoute(onOpenLanguage: () -> Unit) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val appName = stringResource(R.string.app_name)
    val recommendText = stringResource(R.string.string_let_me_recommend_you_this_application)
    val chooseOne = stringResource(R.string.string_choose_one)

    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(SettingsContract.Event.Refresh)
        onPauseOrDispose { }
    }

    SettingsScreen(
        state = state,
        onLanguageClick = onOpenLanguage,
        onWifiOnlyToggle = {
            viewModel.onEvent(SettingsContract.Event.SetWifiOnly(!state.wifiOnly))
        },
        onSearchEngineClick = {
            viewModel.onEvent(SettingsContract.Event.SetSearchEngineSheetVisible(true))
        },
        onDownloadLocationClick = {
            viewModel.onEvent(SettingsContract.Event.SetDownloadLocationEditorVisible(true))
        },
        onRateClick = {
            DialogManager.showRating(
                context.findComponentActivity() as FragmentActivity,
                AppConstant.FEEDBACK_EMAIL,
            )
        },
        onShareClick = {
            val storeUrl =
                "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, appName)
                putExtra(Intent.EXTRA_TEXT, "$appName $recommendText $storeUrl")
            }

            context.startActivity(Intent.createChooser(shareIntent, chooseOne))
        },
        onPolicyClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, POLICY_URL.toUri()),
            )
        },
    )

    if (state.searchEngineSheetVisible) {
        SearchEngineSheet(
            selected = state.searchEngine,
            onSelect = { viewModel.onEvent(SettingsContract.Event.SetSearchEngine(it)) },
            onDismiss = {
                viewModel.onEvent(SettingsContract.Event.SetSearchEngineSheetVisible(false))
            },
        )
    }

    if (state.downloadLocationEditorVisible) {
        DownloadLocationDialog(
            currentSubfolder = state.downloadLocationSubfolder,
            onSave = { viewModel.onEvent(SettingsContract.Event.SetDownloadLocationSubfolder(it)) },
            onDismiss = {
                viewModel.onEvent(SettingsContract.Event.SetDownloadLocationEditorVisible(false))
            },
        )
    }
}

private const val POLICY_URL = "https://smartweb-technology.netlify.app/policy"
