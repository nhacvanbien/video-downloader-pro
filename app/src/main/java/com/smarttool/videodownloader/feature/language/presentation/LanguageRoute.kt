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
import com.smarttool.videodownloader.feature.language.domain.LanguageRepository
import com.smarttool.videodownloader.feature.language.domain.NEW_UI_LFO
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
 * shows a brief loading overlay, and renders its header in whichever language the user
 * is hovering.
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
    val languageRepository: LanguageRepository = koinInject()

    val context = LocalContext.current

    val titleRes = if (fromSplash && NEW_UI_LFO) {
        R.string.string_select_languages
    } else {
        R.string.string_languages
    }

    val initialCode = remember {
        if (fromSplash) {
            systemLanguage()
        } else {
            languageRepository.currentLanguageCodeBlocking().ifEmpty { systemLanguage() }
        }
    }

    var headerTitle by remember { mutableStateOf(localizedString(initialCode, titleRes)) }
    var showLoadingOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.onEvent(LanguageContract.Event.Load(initialCode, preselect = !fromSplash))

        if (!fromSplash) return@LaunchedEffect

        showLoadingOverlay = true
        delay(LOADING_OVERLAY_MILLIS)
        showLoadingOverlay = false
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LanguageContract.Effect.Confirmed -> {
                    // The ViewModel already persisted the choice; AppLocaleProvider (wrapping
                    // the NavHost) picks that up and recomposes in place, so there's no
                    // Activity recreation and no jump back to the graph's start destination.
                    if (fromSplash) onApplied(effect.language) else onBack()
                }

                LanguageContract.Effect.ConfirmFailed -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.string_please_select_language),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val mode = when {
        !fromSplash -> LanguageMode.Settings
        NEW_UI_LFO -> LanguageMode.FirstOpenNew
        else -> LanguageMode.FirstOpen
    }

    LanguageScreen(
        state = state,
        mode = mode,
        showLoadingOverlay = showLoadingOverlay,
        headerTitle = headerTitle,
        onSelect = { code ->
            viewModel.onEvent(LanguageContract.Event.Select(code))
            if (fromSplash) headerTitle = localizedString(code, titleRes)
        },
        onSearch = { query -> viewModel.onEvent(LanguageContract.Event.Search(query)) },
        onConfirm = {
            viewModel.onEvent(LanguageContract.Event.Confirm(markStartShown = fromSplash))
        },
        onBack = onBack,
    )
}

private const val LOADING_OVERLAY_MILLIS = 2000L
