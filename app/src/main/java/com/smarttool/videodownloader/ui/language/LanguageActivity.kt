package com.smarttool.videodownloader.ui.language

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.extensions.setLocale
import com.smarttool.videodownloader.feature.language.presentation.LanguageMode
import com.smarttool.videodownloader.feature.language.presentation.LanguageScreen
import com.smarttool.videodownloader.feature.language.presentation.LanguageViewModel
import com.smarttool.videodownloader.ui.intro.IntroActivity
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.SystemUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class LanguageActivity : BaseComposeActivity() {

    private val viewModel: LanguageViewModel by viewModel()

    private var fromSplash = false
    private var showLoadingOverlay by mutableStateOf(false)

    /**
     * Rendered in the language the user is currently hovering, so the header updates
     * live as they browse the list during first-open.
     */
    private var headerTitle by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fromSplash = intent.getBooleanExtra(FROM_SPLASH, false)

        val currentCode = if (fromSplash) systemLanguage() else SystemUtil.getPreLanguage(this)
        viewModel.load(currentCode, preselect = !fromSplash)
        headerTitle = titleFor(currentCode)

        val mode = when {
            !fromSplash -> LanguageMode.Settings
            AdsConstant.newUILfo -> LanguageMode.FirstOpenNew
            else -> LanguageMode.FirstOpen
        }

        if (fromSplash) {
            AdsConstant.requestNativeFullscreen1(this)
            AdsConstant.requestNativeFullscreen2(this)

            lifecycleScope.launch {
                showLoadingOverlay = true
                delay(LOADING_OVERLAY_MILLIS)
                showLoadingOverlay = false
            }
        }

        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LanguageScreen(
                    state = state,
                    mode = mode,
                    showLoadingOverlay = showLoadingOverlay,
                    headerTitle = headerTitle,
                    onSelect = { code ->
                        viewModel.select(code)
                        if (fromSplash) headerTitle = titleFor(code)
                    },
                    onConfirm = ::onConfirm,
                    onBack = { finish() },
                )
            }
        }
    }

    private fun onConfirm() {
        val selected = viewModel.confirm(markStartShown = fromSplash)

        if (selected == null) {
            Toast.makeText(
                this,
                getString(R.string.string_please_select_language),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        setLocale(selected.code)
        SystemUtil.setPreLanguage(this, selected.code)

        if (!fromSplash) {
            finish()
            return
        }

        startActivity(
            LanguageAppliedActivity.newIntent(this, selected.name, selected.iconRes),
        )
    }

    /** Title string resolved in [languageCode] rather than the app's active locale. */
    private fun titleFor(languageCode: String): String {
        if (languageCode.isEmpty()) return getString(R.string.string_languages)

        val locale = if (languageCode.contains("-")) {
            val split = languageCode.split("-")
            Locale(split[0], split[1])
        } else {
            Locale(languageCode)
        }

        val config = resources.configuration
        config.setLocale(locale)

        val titleRes = if (fromSplash && AdsConstant.newUILfo) {
            R.string.string_select_languages
        } else {
            R.string.string_languages
        }

        return createConfigurationContext(config).getString(titleRes)
    }

    private fun systemLanguage(): String {
        val lang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale.language
        }

        return if (!SystemUtil.getLanguageApp().contains(lang)) "en" else lang
    }

    companion object {
        const val FROM_SPLASH = "from_splash"
        private const val LOADING_OVERLAY_MILLIS = 2000L

        fun newIntent(context: Context, fromSplash: Boolean = false): Intent {
            val intent = Intent(context, LanguageActivity::class.java)
            intent.putExtra(FROM_SPLASH, fromSplash)
            if (fromSplash) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            return intent
        }
    }
}
