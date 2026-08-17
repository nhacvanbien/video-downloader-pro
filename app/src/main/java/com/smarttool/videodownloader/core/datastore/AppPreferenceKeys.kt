package com.smarttool.videodownloader.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** Every preference the app persists. Renaming any of these resets it on upgrade. */
object AppPreferenceKeys {
    val CURRENT_LANGUAGE = stringPreferencesKey("pref_current_language")
    val SHOWED_START_LANGUAGE = booleanPreferencesKey("pref_showed_start_language")
    val SHOWED_ONBOARDING = booleanPreferencesKey("pref_showed_onboarding")
    val SHOW_PERMISSION = booleanPreferencesKey("show_permission")
    val RATING_PROMPT_SHOWN = booleanPreferencesKey("rate")
    val RATING_SUCCESSFUL_DOWNLOAD_COUNT = intPreferencesKey("rating_successful_download_count")
    val RATING_LAST_PROMPTED_DOWNLOAD_COUNT = intPreferencesKey("rating_last_prompted_download_count")

    val PROXY_IP_PORT = stringPreferencesKey("PROXY_IP_PORT")
    val IS_PROXY_TURN_ON = booleanPreferencesKey("IS_PROXY_TURN_ON")

    val HOSTS_POPULATED = booleanPreferencesKey("HOSTS_POPULATED")
    val IS_EXTERNAL_USE = booleanPreferencesKey("IS_EXTERNAL_USE")
    val IS_APP_DIR_USE = booleanPreferencesKey("IS_APP_DIR_USE")

    val REGULAR_THREAD_COUNT = intPreferencesKey("REGULAR_THREAD_COUNT")
    val M3U8_THREAD_COUNT = intPreferencesKey("M3U8_THREAD_COUNT")
    val VIDEO_DETECTION_THRESHOLD = intPreferencesKey("VIDEO_DETECTION_TRESHOLD")

    val IS_LOCK_PORTRAIT = booleanPreferencesKey("IS_LOCK_PORTRAIT")
    val IS_CHECK_IF_IN_LIST = booleanPreferencesKey("IS_CHECK_IF_IN_LIST")
    val IS_SHOW_VIDEO_ALERT = booleanPreferencesKey("IS_SHOW_VIDEO_ALERT")

    val PLAYBACK_SPEED = floatPreferencesKey("SPEED")
    val PLAYBACK_LOOP = booleanPreferencesKey("LOOP_MEDIA")
    val PLAYBACK_FILL = booleanPreferencesKey("FILL_MEDIA")

    val IS_SETUP_PIN_CODE = booleanPreferencesKey("IS_SETUP_PIN_CODE")
    val PIN_CODE = stringPreferencesKey("PIN_CODE")
    val NUM_SECURITY_QUESTION = intPreferencesKey("NUM_SECURITY_QUESTION")
    val SECURITY_ANSWER = stringPreferencesKey("SECURITY_ANSWER")

    val TYPE_SORT = intPreferencesKey("TYPE_SORT")
    val COUNT_EXIT = intPreferencesKey("count_exit")

    val WIFI_ONLY = booleanPreferencesKey("wifi_only")
    val SEARCH_ENGINE_ID = stringPreferencesKey("search_engine_id")
    val DOWNLOAD_LOCATION_SUBFOLDER = stringPreferencesKey("download_location_subfolder")
}
