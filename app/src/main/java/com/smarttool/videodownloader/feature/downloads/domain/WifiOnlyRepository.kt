package com.smarttool.videodownloader.feature.downloads.domain

import kotlinx.coroutines.flow.Flow

/** Whether new downloads must wait for Wi-Fi — set from Settings, read before every start. */
interface WifiOnlyRepository {
    val wifiOnly: Flow<Boolean>

    suspend fun setWifiOnly(enabled: Boolean)
}
