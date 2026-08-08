package com.smarttool.videodownloader.core

import android.content.Context
import android.content.res.Configuration
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import java.util.Locale

/**
 * Returns [this] context reconfigured for the language the user picked, falling back to
 * the system one when nothing is saved.
 *
 * Activities apply it in `attachBaseContext` so legacy, non-Compose screens (which never
 * recompose) still resolve resources in the right language. Compose screens get the same
 * language live, without an Activity recreation, via `AppLocaleProvider`.
 */
fun Context.withAppLocale(preferences: AppPreferencesDataSource): Context {
    val language = preferences.currentLanguageBlocking()
    val locale = if (language.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(language)

    Locale.setDefault(locale)

    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)

    return createConfigurationContext(configuration)
}
