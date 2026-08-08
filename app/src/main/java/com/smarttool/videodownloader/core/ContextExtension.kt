package com.smarttool.videodownloader.core

import android.content.Context
import java.util.Locale

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