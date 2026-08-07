package com.smarttool.videodownloader.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Release tree: nothing is written to logcat, but INFO and above are kept as Crashlytics
 * breadcrumbs so a crash report carries the trail that led to it, and WARN/ERROR throwables
 * are recorded as non-fatals.
 */
class CrashlyticsTree : Timber.Tree() {

    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log("${priorityChar(priority)}/${tag ?: DEFAULT_TAG}: $message")

        if (t != null && priority >= Log.WARN) {
            crashlytics.recordException(t)
        }
    }

    private fun priorityChar(priority: Int): Char = when (priority) {
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> 'D'
    }

    private companion object {
        const val DEFAULT_TAG = "VideoDownloader"
    }
}
