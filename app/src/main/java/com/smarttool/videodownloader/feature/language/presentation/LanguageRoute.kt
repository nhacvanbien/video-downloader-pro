package com.smarttool.videodownloader.feature.language.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.SystemUtil
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ui.components.findComponentActivity
import com.smarttool.videodownloader.extensions.setLocale
import com.smarttool.videodownloader.feature.language.domain.model.AppLanguage
import com.smarttool.videodownloader.feature.language.domain.usecase.GetLocalizedStringUseCase
import com.smarttool.videodownloader.feature.language.domain.usecase.GetSystemLanguageUseCase
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Language picker, reached both from Settings and as the first onboarding step.
 *
 * [fromSplash] is what separates the two: the onboarding variant preselects nothing,
 * preloads the full-screen native ads, shows a brief loading overlay, and renders its
 * header in whichever language the user is hovering.
 */
@Composable
fun LanguageRoute(
    fromSplash: Boolean,
    onApplied: (AppLanguage) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: LanguageViewModel = koinViewModel()
    val systemLanguage: GetSystemLanguageUseCase = koinInject()
    val localizedString: GetLocalizedStringUseCase = koinInject()

    val context = LocalContext.current
    val activity = context.findComponentActivity()

    val titleRes = if (fromSplash && AdsConstant.newUILfo) {
        R.string.string_select_languages
    } else {
        R.string.string_languages
    }

    val initialCode = remember {
        if (fromSplash) systemLanguage() else SystemUtil.getPreLanguage(context)
    }

    var headerTitle by remember { mutableStateOf(localizedString(initialCode, titleRes)) }
    var showLoadingOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.load(initialCode, preselect = !fromSplash)

        if (!fromSplash) return@LaunchedEffect

        AdsConstant.requestNativeFullscreen1(activity)
        AdsConstant.requestNativeFullscreen2(activity)

        showLoadingOverlay = true
        delay(LOADING_OVERLAY_MILLIS)
        showLoadingOverlay = false
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val mode = when {
        !fromSplash -> LanguageMode.Settings
        AdsConstant.newUILfo -> LanguageMode.FirstOpenNew
        else -> LanguageMode.FirstOpen
    }

    LanguageScreen(
        state = state,
        mode = mode,
        showLoadingOverlay = showLoadingOverlay,
        headerTitle = headerTitle,
        onSelect = { code ->
            viewModel.select(code)
            if (fromSplash) headerTitle = localizedString(code, titleRes)
        },
        onConfirm = {
            val selected = viewModel.confirm(markStartShown = fromSplash)

            if (selected == null) {
                Toast.makeText(
                    context,
                    context.getString(R.string.string_please_select_language),
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                activity.setLocale(selected.code)
                SystemUtil.setPreLanguage(context, selected.code)

                if (fromSplash) onApplied(selected) else onBack()
            }
        },
        onBack = onBack,
    )
}

private const val LOADING_OVERLAY_MILLIS = 2000L
