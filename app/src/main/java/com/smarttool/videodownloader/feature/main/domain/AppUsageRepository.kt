package com.smarttool.videodownloader.feature.main.domain

/** Counters the app keeps about how it is used; currently only the exit count. */
interface AppUsageRepository {
    suspend fun recordAppExit()
}
