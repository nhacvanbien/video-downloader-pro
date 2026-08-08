package com.smarttool.videodownloader

import android.annotation.SuppressLint
import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.google.firebase.FirebaseApp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.di.appModule
import com.smarttool.videodownloader.core.di.legacyViewModelModule
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.logging.CrashlyticsTree
import com.smarttool.videodownloader.feature.browser.di.browserModule
import com.smarttool.videodownloader.feature.history.di.historyModule
import com.smarttool.videodownloader.feature.language.di.languageModule
import com.smarttool.videodownloader.feature.library.di.libraryModule
import com.smarttool.videodownloader.feature.main.di.mainModule
import com.smarttool.videodownloader.feature.media.di.mediaModule
import com.smarttool.videodownloader.feature.onboarding.di.onboardingModule
import com.smarttool.videodownloader.feature.pin.di.pinModule
import com.smarttool.videodownloader.feature.settings.di.settingsModule
import com.smarttool.videodownloader.feature.tab.di.tabModule
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import java.io.File

class VideoDownloaderApplication : Application() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        lateinit var instance: VideoDownloaderApplication
    }

    private val preferences: AppPreferencesDataSource by inject()

    private val workerFactory: WorkerFactory by inject()

    private val fileUtil: FileUtil by inject()

    override fun onCreate() {
        super.onCreate()

        // Planted first so everything below (startup, DI, youtube-dl init) is captured.
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashlyticsTree())
        Timber.i("App starting: versionName=${BuildConfig.VERSION_NAME} versionCode=${BuildConfig.VERSION_CODE}")

        instance = this

        startKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@VideoDownloaderApplication)
            modules(
                appModule,
                historyModule,
                settingsModule,
                languageModule,
                pinModule,
                tabModule,
                libraryModule,
                mediaModule,
                browserModule,
                legacyViewModelModule,
                onboardingModule,
                mainModule,
            )
        }

        FirebaseApp.initializeApp(this)

        initializeFileUtils()

        WorkManager.initialize(
            applicationContext,
            Configuration.Builder()
                .setWorkerFactory(workerFactory).build()
        )

        // Off the main thread: creating the download folder hits the filesystem, and the
        // youtube-dl init and update both unpack and download.
        CoroutineScope(Dispatchers.Default).launch {
            val downloadDir: File = fileUtil.folderDir
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            initializeYoutubeDl()

            updateYoutubeDL()
        }
    }

    private fun initializeFileUtils() {
        val isExternal = preferences.isExternalStorageUsedBlocking(FileUtil.isExternalStorageWritable())
        val isAppDir = preferences.isAppDirUsedBlocking()

        Timber.d("initializeFileUtils: isExternal=$isExternal isAppDir=$isAppDir")

        FileUtil.IS_EXTERNAL_STORAGE_USE = isExternal
        FileUtil.IS_APP_DATA_DIR_USE = isAppDir
        FileUtil.INITIIALIZED = true
    }

    private fun initializeYoutubeDl() {
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
//            Aria2c.getInstance().init(this)
        } catch (e: YoutubeDLException) {
            Timber.e(e, "Failed to initialize youtubedl-android")
        }
    }

    private fun updateYoutubeDL() {
        try {
            val status = YoutubeDL.getInstance()
                .updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.NIGHTLY)
            Timber.i("youtube-dl update (NIGHTLY) status=$status")
        } catch (e: Throwable) {
            Timber.e(e, "youtube-dl update failed")
        }
    }
}
