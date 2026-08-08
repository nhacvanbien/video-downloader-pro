package com.smarttool.videodownloader.feature.library.domain

/** The one preference the library screens remember between visits: sort order. */
interface LibraryPreferencesRepository {
    suspend fun sortType(): Int

    suspend fun setSortType(type: Int)
}
