package com.smarttool.videodownloader.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.VideoDownloaderApplication
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.settings.presentation.SettingsScreen
import com.smarttool.videodownloader.feature.settings.presentation.SettingsViewModel
import com.smarttool.videodownloader.ui.language.LanguageActivity
import com.smarttool.videodownloader.core.AppConstant
import com.vimalcvs.materialrating.DialogManager
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModel()

    private var isShareDialogOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            AppTheme {
                SettingsScreen(
                    state = state,
                    onLanguageClick = {
                        startActivity(LanguageActivity.newIntent(requireContext(), false))
                    },
                    onRateClick = ::showDialogRate,
                    onShareClick = ::onClickLayoutShare,
                    onPolicyClick = ::onClickLayoutPrivatePolicy,
                )
            }
        }
    }

    private fun showDialogRate() {
        DialogManager.showRating(requireActivity(), AppConstant.FEEDBACK_EMAIL)
    }

    private fun onClickLayoutShare() {
        try {
            if (!isShareDialogOpen) {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                var shareMessage =
                    "${getString(R.string.app_name)} ${getString(R.string.string_let_me_recommend_you_this_application)} ".trimIndent()
                shareMessage =
                    "${shareMessage}https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}".trimIndent()
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)

                activity?.window!!.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()

                startActivityForResult(
                    Intent.createChooser(
                        shareIntent,
                        getString(R.string.string_choose_one)
                    ), 1900
                )

                isShareDialogOpen = true
            }
        } catch (e: Exception) {
            Log.d("ntt", "onClickLayoutShare: ${e.printStackTrace()}")
        }
    }

    private fun onClickLayoutPrivatePolicy() {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://smartweb-technology.netlify.app/policy"))
        VideoDownloaderApplication.instance.appResumeAdHelper.setDisableAppResumeOnScreen()
        startActivity(browserIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1900) {
            isShareDialogOpen = false
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        VideoDownloaderApplication.instance.appResumeAdHelper.setEnableAppResumeOnScreen()
    }
}
