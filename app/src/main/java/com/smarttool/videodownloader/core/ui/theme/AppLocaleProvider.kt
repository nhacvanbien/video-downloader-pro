package com.smarttool.videodownloader.core.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import java.util.Locale
import org.koin.compose.koinInject

/**
 * Reconfigures [LocalContext]/[LocalConfiguration] for the language the user picked, so
 * every `stringResource()` under [content] recomposes into the new language the moment it
 * changes in [AppPreferencesDataSource.currentLanguage].
 *
 * This intentionally avoids `AppCompatDelegate.setApplicationLocales` / `recreate()`: those
 * tear the Activity down and rebuild the nav graph from its start destination, which is
 * exactly the "flicker back to home" behavior a mid-session language change must not cause.
 */
@Composable
fun AppLocaleProvider(content: @Composable () -> Unit) {
    val preferences: AppPreferencesDataSource = koinInject()
    val languageCode by preferences.currentLanguage.collectAsStateWithLifecycle(
        initialValue = preferences.currentLanguageBlocking(),
    )

    val context = LocalContext.current
    val localizedContext = remember(context, languageCode) {
        if (languageCode.isEmpty()) {
            context
        } else {
            val locale = Locale.forLanguageTag(languageCode)
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
            context.createConfigurationContext(configuration)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content,
    )
}
