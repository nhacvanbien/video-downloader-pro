
package com.smarttool.videodownloader

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.work.Configuration
import androidx.work.WorkManager
import com.ads.admob.admob.TevoAdmobFactory
import com.ads.admob.config.EventConfig
import com.ads.admob.config.NetworkProvider
import com.ads.admob.config.TevoAdjustConfig
import com.ads.admob.config.TevoAdsConfig
import com.ads.admob.helper.appoppen.AppResumeAdConfig
import com.ads.admob.helper.appoppen.AppResumeAdHelper
import com.google.android.gms.ads.AdActivity
import com.google.firebase.FirebaseApp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.core.di.appModule
import com.smarttool.videodownloader.core.di.legacyViewModelModule
import com.smarttool.videodownloader.feature.browser.di.browserModule
import com.smarttool.videodownloader.feature.history.di.historyModule
import com.smarttool.videodownloader.feature.language.di.languageModule
import com.smarttool.videodownloader.feature.library.di.libraryModule
import com.smarttool.videodownloader.feature.media.di.mediaModule
import com.smarttool.videodownloader.feature.pin.di.pinModule
import com.smarttool.videodownloader.feature.tab.di.tabModule
import com.smarttool.videodownloader.feature.settings.di.settingsModule
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.feature.splash.presentation.SplashActivity
import com.smarttool.videodownloader.feature.tab.presentation.TabViewModel
import com.smarttool.videodownloader.core.ads.AdsConstant
import com.smarttool.videodownloader.core.ContextUtils
import com.smarttool.videodownloader.core.logging.CrashlyticsTree
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.SystemUtil
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.File
import kotlin.jvm.java
import androidx.work.WorkerFactory
import org.koin.android.ext.android.inject
import timber.log.Timber

class VideoDownloaderApplication : Application() {

    @SuppressLint("StaticFieldLeak")
    companion object {
        lateinit var instance: VideoDownloaderApplication
    }

    private val sharedPrefHelper: PreferenceHelper by inject()

    private val workerFactory: WorkerFactory by inject()

    private val fileUtil: FileUtil by inject()

    val globalViewModel by lazy {
        ViewModelProvider(
            ViewModelStore(),
            ViewModelProvider.AndroidViewModelFactory.getInstance(this)
        ).get(TabViewModel::class.java)
    }

    override fun onCreate() {
        super.onCreate()

        // Planted first so everything below (startup, DI, youtube-dl init) is captured.
        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else CrashlyticsTree())
        Timber.i("App starting: versionName=${BuildConfig.VERSION_NAME} versionCode=${BuildConfig.VERSION_CODE}")

        ContextUtils.initApplicationContext(applicationContext)
        instance = this
        SystemUtil.setLocale(this)

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
            )
        }

        FirebaseApp.initializeApp(this)

        initializeFileUtils()

        val file: File = fileUtil.folderDir

        WorkManager.initialize(
            applicationContext,
            Configuration.Builder()
                .setWorkerFactory(workerFactory).build()
        )

        RxJavaPlugins.setErrorHandler { error: Throwable? ->
            Timber.e(error, "Unhandled RxJava error")
        }

        CoroutineScope(Dispatchers.Default).launch {
            if (!file.exists()) {
                file.mkdirs()
            }

            initializeYoutubeDl()

            updateYoutubeDL()
        }

        initTevoAdLib()
    }

    private fun initializeFileUtils() {
        val isExternal = sharedPrefHelper.getIsExternalUse()
        val isAppDir = sharedPrefHelper.getIsAppDirUse()

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
                .updateYoutubeDL(applicationContext, YoutubeDL.UpdateChannel.MASTER)
            Timber.i("youtube-dl update (MASTER) status=$status")
        } catch (e: Throwable) {
            Timber.e(e, "youtube-dl update failed")
        }
    }

    private fun initTevoAdLib() {
        val tevoAdjustConfig =
            TevoAdjustConfig.Build("tcv1br2lo1s0", true).build()
        val tevoAdsConfig = TevoAdsConfig.Builder(mexaAdjustConfig = tevoAdjustConfig)
            .intervalBetweenInterstitial(15000L)
            .buildVariantProduce(false)
            .mediationProvider(NetworkProvider.ADMOB)
            .eventConfig(EventConfig(exchangeRate = 26000, exchangeCurrency = "VND"))
            .listTestDevices(arrayListOf("CFCDA023561C957CF4C6255116677DB6", "D000EACD8F96E69652B309A4A3037BAD", "533EC017154FECA5D81ADAB06C87416B"))
            .build()
        TevoAdmobFactory.initAdmob(this, tevoAdsConfig)
    }
    val appResumeAdHelper by lazy { initAppOpenAd() }
    private fun initAppOpenAd(): AppResumeAdHelper {
        // The browser used to be listed here as WebTabActivity. It is a destination in
        // MainActivity's graph now, and blocking by class would suppress app-resume ads
        // on every screen; the browser destination calls
        // setDisableAppResumeOnScreen/setEnableAppResumeOnScreen for itself instead.
        val listClassInValid = mutableListOf<Class<*>>()
        listClassInValid.add(AdActivity::class.java)
        listClassInValid.add(SplashActivity::class.java)
        val config = AppResumeAdConfig(
            idAds = BuildConfig.OPEN_RESUME,
            listClassInValid = listClassInValid,
            canShowAds = AdsConstant.showOpenResume,
            adPlacement = "open_resume",
        )
        return AppResumeAdHelper(
            application = this,
            lifecycleOwner = ProcessLifecycleOwner.get(),
            config = config,
        )
    }
}
