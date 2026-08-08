package com.smarttool.videodownloader.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.feature.browser.presentation.WebTabViewHost
import com.smarttool.videodownloader.feature.browser.presentation.WebTabRoute
import com.smarttool.videodownloader.feature.disclaimers.presentation.DisclaimersRoute
import com.smarttool.videodownloader.feature.downloads.presentation.ProcessingWebViewHost
import com.smarttool.videodownloader.feature.guide.presentation.GuideRoute
import com.smarttool.videodownloader.feature.history.presentation.HistoryMode
import com.smarttool.videodownloader.feature.history.presentation.HistoryRoute
import com.smarttool.videodownloader.feature.intro.presentation.IntroRoute
import com.smarttool.videodownloader.feature.language.presentation.LanguageAppliedRoute
import com.smarttool.videodownloader.feature.language.presentation.LanguageRoute
import com.smarttool.videodownloader.feature.library.presentation.LibraryRoute
import com.smarttool.videodownloader.feature.library.presentation.SelectVideoRoute
import com.smarttool.videodownloader.feature.main.presentation.GUIDE_FROM_HOME
import com.smarttool.videodownloader.feature.main.presentation.MainRoute
import com.smarttool.videodownloader.feature.main.presentation.MainTab
import com.smarttool.videodownloader.feature.media.presentation.MediaRoute
import com.smarttool.videodownloader.feature.permission.presentation.PermissionRoute
import com.smarttool.videodownloader.feature.pin.presentation.PinRoute
import com.smarttool.videodownloader.feature.pin.presentation.SecurityRoute
import com.smarttool.videodownloader.feature.tab.presentation.TabsRoute

/** PIN screen opened to change an existing code rather than unlock. */
const val PIN_ACTION_CHANGE = "changePinCode"

/** Security screen opened from "forgot PIN" rather than from choosing one. */
const val SECURITY_STATUS_FORGOT = "forgot"

private const val HISTORY_MODE_BOOKMARK = "bookmark"

/**
 * The whole app graph. Everything except the splash screen is a destination here.
 *
 * Navigation used to be `startActivity` plus `FLAG_ACTIVITY_CLEAR_TASK`; those flags
 * map onto `popUpTo(..., inclusive = true)`, which is why the onboarding chain pops
 * each step as it advances — backing out of onboarding must not walk it in reverse.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    selectedTab: MainTab,
    processingHost: ProcessingWebViewHost,
    webTabHost: WebTabViewHost,
    onSelectTab: (MainTab) -> Unit,
    onExitRequested: () -> Unit,
) {
    val openWebTab: (String) -> Unit = { url ->
        navController.navigate(AppRoute.webTab(url)) {
            // Opening a page always replaces the browser rather than stacking a second
            // WebView on top of it, so the tab list and history do not pile up behind.
            popUpTo(AppRoute.MAIN)
        }
    }

    val openMedia: (VideoTaskItem) -> Unit = { item ->
        navController.navigate(
            AppRoute.playMedia(
                url = item.filePath,
                title = item.title,
                type = if (item.mimeType.startsWith("image")) "image" else "video",
                isDownloaded = true,
            ),
        )
    }

    val previewMedia: (String, String, String) -> Unit = { url, title, headers ->
        navController.navigate(
            AppRoute.playMedia(url = url, title = title, headers = headers),
        )
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(AppRoute.MAIN) {
            MainRoute(
                selectedTab = selectedTab,
                processingHost = processingHost,
                onSelectTab = onSelectTab,
                onOpenUrl = openWebTab,
                onOpenGuide = { from -> navController.navigate(AppRoute.guide(from)) },
                onOpenTabs = { navController.navigate(AppRoute.TABS) },
                onOpenBookmarks = {
                    navController.navigate(AppRoute.history(HISTORY_MODE_BOOKMARK))
                },
                onOpenHistory = { navController.navigate(AppRoute.history("history")) },
                onOpenPrivateArea = { navController.navigate(AppRoute.pin()) },
                onOpenLanguage = { navController.navigate(AppRoute.language(false)) },
                onOpenMedia = openMedia,
                onPreviewMedia = previewMedia,
                onExitRequested = onExitRequested,
            )
        }

        composable(
            route = AppRoute.GUIDE,
            arguments = listOf(navArgument(AppRoute.ARG_OPEN) { type = NavType.StringType }),
        ) { entry ->
            val open = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_OPEN))

            GuideRoute(
                isHomeGuide = open == GUIDE_FROM_HOME,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = AppRoute.HISTORY,
            arguments = listOf(navArgument(AppRoute.ARG_MODE) { type = NavType.StringType }),
        ) { entry ->
            val mode = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_MODE))

            HistoryRoute(
                mode = if (mode == HISTORY_MODE_BOOKMARK) {
                    HistoryMode.BOOKMARK
                } else {
                    HistoryMode.HISTORY
                },
                onOpenUrl = openWebTab,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoute.TABS) {
            TabsRoute(
                onOpenUrl = openWebTab,
            )
        }

        composable(
            route = AppRoute.WEB_TAB,
            arguments = listOf(navArgument(AppRoute.ARG_URL) { type = NavType.StringType }),
        ) { entry ->
            val url = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_URL))

            WebTabRoute(
                host = webTabHost,
                url = url,
                onOpenTabs = { navController.navigate(AppRoute.TABS) },
                onPreviewMedia = previewMedia,
                onBack = {
                    webTabHost.release()
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = AppRoute.PLAY_MEDIA,
            arguments = listOf(
                navArgument(AppRoute.ARG_URL) { type = NavType.StringType },
                navArgument(AppRoute.ARG_TITLE) {
                    type = NavType.StringType
                    defaultValue = AppRoute.EMPTY
                },
                navArgument(AppRoute.ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = "video"
                },
                navArgument(AppRoute.ARG_DOWNLOADED) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(AppRoute.ARG_HEADERS) {
                    type = NavType.StringType
                    defaultValue = AppRoute.EMPTY
                },
            ),
        ) { entry ->
            val args = entry.arguments

            MediaRoute(
                url = AppRoute.decode(args?.getString(AppRoute.ARG_URL)),
                title = AppRoute.decode(args?.getString(AppRoute.ARG_TITLE)),
                isDownloaded = args?.getBoolean(AppRoute.ARG_DOWNLOADED) == true,
                headersJson = AppRoute.decode(args?.getString(AppRoute.ARG_HEADERS)),
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = AppRoute.PIN,
            arguments = listOf(navArgument(AppRoute.ARG_ACTION) { type = NavType.StringType }),
        ) { entry ->
            val action = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_ACTION))

            PinRoute(
                isChangingPin = action == PIN_ACTION_CHANGE,
                onUnlocked = {
                    navController.navigate(AppRoute.PRIVATE_VIDEO) {
                        popUpTo(AppRoute.PIN) { inclusive = true }
                    }
                },
                onPinChosen = { pin ->
                    navController.navigate(AppRoute.security(pass = pin)) {
                        popUpTo(AppRoute.PIN) { inclusive = true }
                    }
                },
                onOpenRecovery = {
                    navController.navigate(AppRoute.security(status = SECURITY_STATUS_FORGOT)) {
                        popUpTo(AppRoute.PIN) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = AppRoute.SECURITY,
            arguments = listOf(
                navArgument(AppRoute.ARG_STATUS) { type = NavType.StringType },
                navArgument(AppRoute.ARG_PASS) { type = NavType.StringType },
            ),
        ) { entry ->
            val status = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_STATUS))
            val pass = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_PASS))

            SecurityRoute(
                isRecovery = status == SECURITY_STATUS_FORGOT,
                pendingPin = pass,
                onRecovered = {
                    navController.navigate(AppRoute.pin()) {
                        popUpTo(AppRoute.SECURITY) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoute.PRIVATE_VIDEO) {
            LibraryRoute(
                isPrivate = true,
                trailingIconRes = R.drawable.ic_add,
                onTrailingAction = { navController.navigate(AppRoute.SELECT_VIDEO) },
                onOpenMedia = openMedia,
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoute.SELECT_VIDEO) {
            SelectVideoRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = AppRoute.LANGUAGE,
            arguments = listOf(
                navArgument(AppRoute.ARG_FROM_SPLASH) {
                    type = NavType.BoolType
                    // Only consulted when the graph starts here, which is the splash flow.
                    defaultValue = true
                },
            ),
        ) { entry ->
            val fromSplash = entry.arguments?.getBoolean(AppRoute.ARG_FROM_SPLASH) == true

            LanguageRoute(
                fromSplash = fromSplash,
                onApplied = { language ->
                    navController.navigate(
                        AppRoute.languageApplied(language.name, language.iconRes),
                    ) {
                        popUpTo(AppRoute.LANGUAGE) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = AppRoute.LANGUAGE_APPLIED,
            arguments = listOf(
                navArgument(AppRoute.ARG_NAME) { type = NavType.StringType },
                navArgument(AppRoute.ARG_ICON) { type = NavType.IntType },
            ),
        ) { entry ->
            LanguageAppliedRoute(
                languageName = AppRoute.decode(entry.arguments?.getString(AppRoute.ARG_NAME)),
                languageIconRes = entry.arguments?.getInt(AppRoute.ARG_ICON) ?: 0,
                onContinue = {
                    navController.navigate(AppRoute.INTRO) {
                        popUpTo(AppRoute.LANGUAGE_APPLIED) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.INTRO) {
            IntroRoute(
                onFinish = { showPermission ->
                    val next = if (showPermission) AppRoute.PERMISSION else AppRoute.MAIN
                    navController.navigate(next) {
                        popUpTo(AppRoute.INTRO) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.PERMISSION) {
            PermissionRoute(
                onContinue = {
                    navController.navigate(AppRoute.DISCLAIMERS) {
                        popUpTo(AppRoute.PERMISSION) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoute.DISCLAIMERS) {
            DisclaimersRoute(
                onAgree = {
                    navController.navigate(AppRoute.MAIN) {
                        popUpTo(AppRoute.DISCLAIMERS) { inclusive = true }
                    }
                },
            )
        }
    }
}
