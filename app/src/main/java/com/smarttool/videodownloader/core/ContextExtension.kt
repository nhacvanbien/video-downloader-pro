package com.smarttool.videodownloader.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Returns [this] context reconfigured for the language the user picked, falling back to
 * the system one when nothing is saved.
 *
 * Activities apply it in `attachBaseContext` so every resource they resolve is already in
 * that language, instead of mutating the shared `Resources` afterwards the way the
 * deprecated `updateConfiguration` path did.
 */
fun Context.withAppLocale(): Context {
    val language = SystemUtil.getPreLanguage(this)
    val locale = if (language.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(language)

    Locale.setDefault(locale)

    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)

    return createConfigurationContext(configuration)
}

fun Context.setLocale(language: String?) {
    val configuration = resources.configuration
    val locale = if (language.isNullOrEmpty()) {
        Locale.getDefault()
    } else {
        Locale(language)
    }
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    createConfigurationContext(configuration)
}