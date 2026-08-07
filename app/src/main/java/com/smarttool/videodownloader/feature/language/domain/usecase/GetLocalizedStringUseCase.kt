package com.smarttool.videodownloader.feature.language.domain.usecase

import android.content.Context
import java.util.Locale

/**
 * Resolves a string resource in an arbitrary language rather than the app's active
 * locale — the first-open picker renders its header in whichever language the user is
 * currently hovering, so the title updates live as they browse the list.
 */
class GetLocalizedStringUseCase(private val context: Context) {

    operator fun invoke(languageCode: String, stringRes: Int): String {
        if (languageCode.isEmpty()) return context.getString(stringRes)

        val locale = if (languageCode.contains("-")) {
            val parts = languageCode.split("-")
            Locale(parts[0], parts[1])
        } else {
            Locale(languageCode)
        }

        val config = context.resources.configuration
        config.setLocale(locale)

        return context.createConfigurationContext(config).getString(stringRes)
    }
}
