package com.smarttool.videodownloader.feature.language.presentation

/** Which chrome the language screen wears — the three variants the View version toggled between. */
enum class LanguageMode {
    /** First-open flow, legacy look: gradient background, "Save" text button. */
    FirstOpen,

    /** First-open flow, new look: flat grey background, check-icon confirm. */
    FirstOpenNew,

    /** Reached from Settings: back arrow plus "Save". */
    Settings,
}
