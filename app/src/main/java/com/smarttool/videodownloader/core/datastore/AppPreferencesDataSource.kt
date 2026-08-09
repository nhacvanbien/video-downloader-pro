package com.smarttool.videodownloader.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.smarttool.videodownloader.core.coroutines.AppScope
import com.smarttool.videodownloader.core.network.TikTokExtractionSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException

/**
 * The single place the app's preferences are stored, backed by Preferences DataStore.
 *
 * Reads are [Flow]s and writes are `suspend` — screens go through a repository and a
 * ViewModel to reach either. The `…Blocking` accessors at the bottom exist only for the
 * handful of pre-existing non-suspending call sites in the download workers and the
 * proxy/WebView plumbing, which cannot be made suspending without rewriting the
 * downloader; they read the in-memory snapshot the collector below keeps warm, so they
 * only ever touch disk once, at startup.
 */
class AppPreferencesDataSource(
    context: Context,
    appScope: AppScope,
) {
    private val scope: CoroutineScope = appScope + Dispatchers.IO

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) },
    )

    /** A corrupt or unreadable file must not crash the app; it falls back to defaults. */
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable !is IOException) throw throwable
            Timber.w(throwable, "Reading preferences failed; falling back to defaults")
            emit(emptyPreferences())
        }

    private val snapshot = MutableStateFlow<Preferences?>(null)

    init {
        scope.launch { preferences.collect { snapshot.value = it } }
    }

    // --- Onboarding -------------------------------------------------------------

    val showedStartLanguage: Flow<Boolean> = read(AppPreferenceKeys.SHOWED_START_LANGUAGE, false)

    val showedOnboarding: Flow<Boolean> = read(AppPreferenceKeys.SHOWED_ONBOARDING, false)

    val showPermissionStep: Flow<Boolean> = read(AppPreferenceKeys.SHOW_PERMISSION, true)

    suspend fun setShowedStartLanguage(showed: Boolean) =
        write(AppPreferenceKeys.SHOWED_START_LANGUAGE, showed)

    suspend fun setShowedOnboarding(showed: Boolean) =
        write(AppPreferenceKeys.SHOWED_ONBOARDING, showed)

    suspend fun setShowPermissionStep(show: Boolean) =
        write(AppPreferenceKeys.SHOW_PERMISSION, show)

    // --- Language ---------------------------------------------------------------

    val currentLanguage: Flow<String> = read(AppPreferenceKeys.CURRENT_LANGUAGE, "")

    suspend fun setCurrentLanguage(code: String) =
        write(AppPreferenceKeys.CURRENT_LANGUAGE, code)

    /** Synchronous read for `Activity.attachBaseContext`, which cannot suspend. */
    fun currentLanguageBlocking(): String = latest()[AppPreferenceKeys.CURRENT_LANGUAGE] ?: ""

    // --- Settings ---------------------------------------------------------------

    val rated: Flow<Boolean> = read(AppPreferenceKeys.RATED, false)

    suspend fun setRated(rated: Boolean) = write(AppPreferenceKeys.RATED, rated)

    // --- Storage ----------------------------------------------------------------

    /** @param defaultExternal the app falls back to whatever the device supports. */
    fun isExternalStorageUsed(defaultExternal: Boolean): Flow<Boolean> =
        read(AppPreferenceKeys.IS_EXTERNAL_USE, defaultExternal)

    val isAppDirUsed: Flow<Boolean> = read(AppPreferenceKeys.IS_APP_DIR_USE, false)

    // --- Ad blocker -------------------------------------------------------------

    val adHostsPopulated: Flow<Boolean> = read(AppPreferenceKeys.HOSTS_POPULATED, false)

    suspend fun setAdHostsPopulated(populated: Boolean) =
        write(AppPreferenceKeys.HOSTS_POPULATED, populated)

    // --- Proxy ------------------------------------------------------------------

    val proxyJson: Flow<String> = read(AppPreferenceKeys.PROXY_IP_PORT, EMPTY_JSON)

    val isProxyOn: Flow<Boolean> = read(AppPreferenceKeys.IS_PROXY_TURN_ON, false)

    // --- Browser ----------------------------------------------------------------

    val isLockPortrait: Flow<Boolean> = read(AppPreferenceKeys.IS_LOCK_PORTRAIT, false)

    val isShowVideoAlert: Flow<Boolean> = read(AppPreferenceKeys.IS_SHOW_VIDEO_ALERT, true)

    val videoDetectionThreshold: Flow<Int> =
        read(AppPreferenceKeys.VIDEO_DETECTION_THRESHOLD, DEFAULT_DETECTION_THRESHOLD)

    suspend fun setShowVideoAlert(show: Boolean) =
        write(AppPreferenceKeys.IS_SHOW_VIDEO_ALERT, show)

    // --- Media player -----------------------------------------------------------

    val playbackSpeed: Flow<Float> = read(AppPreferenceKeys.PLAYBACK_SPEED, 1f)

    val playbackLooping: Flow<Boolean> = read(AppPreferenceKeys.PLAYBACK_LOOP, false)

    val playbackFillMode: Flow<Boolean> = read(AppPreferenceKeys.PLAYBACK_FILL, false)

    suspend fun setPlaybackSpeed(speed: Float) = write(AppPreferenceKeys.PLAYBACK_SPEED, speed)

    suspend fun setPlaybackLooping(looping: Boolean) =
        write(AppPreferenceKeys.PLAYBACK_LOOP, looping)

    suspend fun setPlaybackFillMode(fill: Boolean) = write(AppPreferenceKeys.PLAYBACK_FILL, fill)

    // --- PIN --------------------------------------------------------------------

    val isPinConfigured: Flow<Boolean> = read(AppPreferenceKeys.IS_SETUP_PIN_CODE, false)

    val pinCode: Flow<String> = read(AppPreferenceKeys.PIN_CODE, "")

    val securityQuestionIndex: Flow<Int> = read(AppPreferenceKeys.NUM_SECURITY_QUESTION, 1)

    val securityAnswer: Flow<String> = read(AppPreferenceKeys.SECURITY_ANSWER, "")

    suspend fun savePin(pin: String) = dataStore.editSafely {
        it[AppPreferenceKeys.IS_SETUP_PIN_CODE] = true
        it[AppPreferenceKeys.PIN_CODE] = pin
    }

    suspend fun saveSecurityQuestion(index: Int, answer: String) = dataStore.editSafely {
        it[AppPreferenceKeys.NUM_SECURITY_QUESTION] = index
        it[AppPreferenceKeys.SECURITY_ANSWER] = answer
    }

    suspend fun clearPin() = dataStore.editSafely {
        it[AppPreferenceKeys.IS_SETUP_PIN_CODE] = false
        it[AppPreferenceKeys.PIN_CODE] = ""
        it[AppPreferenceKeys.NUM_SECURITY_QUESTION] = 1
        it[AppPreferenceKeys.SECURITY_ANSWER] = ""
    }

    // --- Library ----------------------------------------------------------------

    val sortType: Flow<Int> = read(AppPreferenceKeys.TYPE_SORT, 1)

    suspend fun setSortType(type: Int) = write(AppPreferenceKeys.TYPE_SORT, type)

    // --- App usage --------------------------------------------------------------

    suspend fun incrementExitCount() = dataStore.editSafely {
        it[AppPreferenceKeys.COUNT_EXIT] = (it[AppPreferenceKeys.COUNT_EXIT] ?: 1) + 1
    }

    // --- Blocking accessors for the legacy downloader / WebView plumbing ---------

    fun isExternalStorageUsedBlocking(defaultExternal: Boolean): Boolean =
        latest()[AppPreferenceKeys.IS_EXTERNAL_USE] ?: defaultExternal

    fun isAppDirUsedBlocking(): Boolean = latest()[AppPreferenceKeys.IS_APP_DIR_USE] ?: false

    fun proxyJsonBlocking(): String = latest()[AppPreferenceKeys.PROXY_IP_PORT] ?: EMPTY_JSON

    fun isProxyOnBlocking(): Boolean = latest()[AppPreferenceKeys.IS_PROXY_TURN_ON] ?: false

    fun isCheckHostByListBlocking(): Boolean =
        latest()[AppPreferenceKeys.IS_CHECK_IF_IN_LIST] ?: false

    fun regularThreadCountBlocking(): Int = latest()[AppPreferenceKeys.REGULAR_THREAD_COUNT] ?: 1

    fun m3u8ThreadCountBlocking(): Int = latest()[AppPreferenceKeys.M3U8_THREAD_COUNT] ?: 3

    fun videoDetectionThresholdBlocking(): Int =
        latest()[AppPreferenceKeys.VIDEO_DETECTION_THRESHOLD] ?: DEFAULT_DETECTION_THRESHOLD

    fun setProxyJsonBlocking(json: String) = writeBlocking(AppPreferenceKeys.PROXY_IP_PORT, json)

    fun setProxyOnBlocking(isOn: Boolean) =
        writeBlocking(AppPreferenceKeys.IS_PROXY_TURN_ON, isOn)

    /**
     * Stable per-install id for TikTok's mobile-API extractor-arg (see
     * [com.smarttool.videodownloader.core.network.TikTokExtractionSupport]). Generated once
     * and persisted — TikTok is more likely to treat a device id that stays the same across
     * requests as genuine than one that changes every call.
     */
    fun tikTokDeviceIdBlocking(): String {
        latest()[AppPreferenceKeys.TIKTOK_DEVICE_ID]?.let { return it }
        val generated = TikTokExtractionSupport.generateDeviceId()
        writeBlocking(AppPreferenceKeys.TIKTOK_DEVICE_ID, generated)
        return generated
    }

    // --- Internals --------------------------------------------------------------

    private fun <T> read(key: Preferences.Key<T>, default: T): Flow<T> =
        preferences.map { it[key] ?: default }.distinctUntilChanged()

    private suspend fun <T> write(key: Preferences.Key<T>, value: T) {
        dataStore.editSafely { it[key] = value }
    }

    /** Fire-and-forget write for the non-suspend callers listed above. */
    private fun <T> writeBlocking(key: Preferences.Key<T>, value: T) {
        scope.launch { write(key, value) }
    }

    /**
     * The last emission the collector saw, or a one-off read if it has not arrived yet —
     * which can only happen for a call made before the first emission at startup.
     */
    private fun latest(): Preferences =
        snapshot.value ?: runBlocking { preferences.first().also { snapshot.value = it } }

    private suspend fun DataStore<Preferences>.editSafely(
        transform: (MutablePreferences) -> Unit,
    ) {
        try {
            edit(transform)
        } catch (e: IOException) {
            Timber.w(e, "Writing preferences failed")
        }
    }

    private companion object {
        const val DATA_STORE_NAME = "app_preferences"

        const val EMPTY_JSON = "{}"
        const val DEFAULT_DETECTION_THRESHOLD = 5 * 1024 * 1024
    }
}
