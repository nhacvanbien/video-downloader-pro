package com.vimalcvs.materialrating

import android.content.Context
import androidx.core.content.edit

class RatingCache private constructor(context: Context) {

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isRatingShown(): Boolean = sharedPreferences.getBoolean(RATING_SHOWN_KEY, false)

    fun setRatingShown() {
        sharedPreferences.edit { putBoolean(RATING_SHOWN_KEY, true) }
    }

    /** Successful downloads are the signal the user is getting value out of the app. */
    fun incrementSuccessfulDownloadCount(): Int {
        val count = sharedPreferences.getInt(SUCCESSFUL_DOWNLOAD_COUNT_KEY, 0) + 1
        sharedPreferences.edit { putInt(SUCCESSFUL_DOWNLOAD_COUNT_KEY, count) }
        return count
    }

    fun getLastPromptedDownloadCount(): Int = sharedPreferences.getInt(LAST_PROMPTED_DOWNLOAD_COUNT_KEY, 0)

    fun setLastPromptedDownloadCount(count: Int) {
        sharedPreferences.edit { putInt(LAST_PROMPTED_DOWNLOAD_COUNT_KEY, count) }
    }

    companion object {
        private const val PREF_NAME = "rating_cache"
        private const val RATING_SHOWN_KEY = "rating_shown_key"
        private const val SUCCESSFUL_DOWNLOAD_COUNT_KEY = "successful_download_count_key"
        private const val LAST_PROMPTED_DOWNLOAD_COUNT_KEY = "last_prompted_download_count_key"

        @Volatile
        private var instance: RatingCache? = null

        fun getInstance(context: Context): RatingCache = instance ?: synchronized(this) {
            instance ?: RatingCache(context).also { instance = it }
        }
    }
}
