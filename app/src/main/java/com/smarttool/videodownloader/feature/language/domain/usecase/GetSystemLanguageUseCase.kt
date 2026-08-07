package com.smarttool.videodownloader.feature.language.domain.usecase

import android.content.res.Resources
import android.os.Build
import com.smarttool.videodownloader.core.SystemUtil

/**
 * The device language, narrowed to one the app actually ships translations for.
 * Anything unsupported falls back to English, which is what the first-open language
 * picker preselects.
 */
class GetSystemLanguageUseCase {

    operator fun invoke(): String {
        val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Resources.getSystem().configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            Resources.getSystem().configuration.locale.language
        }

        return if (!SystemUtil.getLanguageApp().contains(language)) "en" else language
    }
}
