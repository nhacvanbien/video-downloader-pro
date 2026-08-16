package com.smarttool.videodownloader.feature.settings.domain.model

import com.smarttool.videodownloader.feature.browser.domain.model.SearchEngine

data class SettingsInfo(
    val downloadLocation: String,
    val isRated: Boolean,
    val wifiOnly: Boolean,
    val searchEngine: SearchEngine,
    val downloadLocationSubfolder: String,
)
