package com.smarttool.videodownloader.core.di

import android.app.Application
import androidx.room.Room
import androidx.work.WorkerFactory
import com.smarttool.videodownloader.data.AppDatabase
import com.smarttool.videodownloader.data.dao.AdHostDao
import com.smarttool.videodownloader.data.dao.HistoryDao
import com.smarttool.videodownloader.data.dao.ProgressDao
import com.smarttool.videodownloader.data.dao.TabModelDao
import com.smarttool.videodownloader.data.dao.VideoTaskItemDao
import com.smarttool.videodownloader.data.remote.service.AdBlockHostsRemoteDataSource
import com.smarttool.videodownloader.data.remote.service.ConfigService
import com.smarttool.videodownloader.data.remote.service.ProgressLocalDataSource
import com.smarttool.videodownloader.data.remote.service.VideoServiceLocal
import com.smarttool.videodownloader.data.remote.service.YoutubedlHelper
import com.smarttool.videodownloader.data.repository.ProgressRepository
import com.smarttool.videodownloader.data.repository.ProgressRepositoryImpl
import com.smarttool.videodownloader.data.repository.TabModelRepository
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.helper.PreferenceHelper
import com.smarttool.videodownloader.core.ui.SystemUiController
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.file.IntentUtil
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.core.network.Memory
import com.smarttool.videodownloader.core.notification.NotificationsHelper
import com.smarttool.videodownloader.data.downloader.generic_downloader.DaggerWorkerFactory
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.core.scheduler.BaseSchedulers
import com.smarttool.videodownloader.core.scheduler.BaseSchedulersImpl
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private const val DATA_URL = "https://some-url.com/youtube-dl/"

/** Distinguishes the DAO-backed progress store from the caching wrapper over it. */
val LocalProgressSource = named("localProgressSource")

/**
 * Application-wide singletons, ported from the Hilt modules that used to declare
 * them (`DatabaseModule`, `NetworkModule`, `UtilModule`, `SchedulerModule`,
 * `WorkerModule`, `ProgressModule`).
 */
val appModule = module {
    // --- Database ---
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "videoDownloader")
            .fallbackToDestructiveMigration()
            .build()
    }
    single<HistoryDao> { get<AppDatabase>().historyDao() }
    single<TabModelDao> { get<AppDatabase>().tabModelDao() }
    single<AdHostDao> { get<AppDatabase>().adHostDao() }
    single<ProgressDao> { get<AppDatabase>().progressDao() }
    single<VideoTaskItemDao> { get<AppDatabase>().videoTaskItemDao() }

    // --- Network ---
    single {
        val application: Application = androidApplication()
        OkHttpClient.Builder()
            .connectTimeout(10L, TimeUnit.SECONDS)
            .writeTimeout(10L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .cache(
                Cache(
                    File(application.cacheDir, "YoutubeDLCache"),
                    Memory.calcCacheSize(application, .25f),
                ),
            )
            .build()
    }
    single<ConfigService> {
        Retrofit.Builder()
            .baseUrl(DATA_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
            .create(ConfigService::class.java)
    }
    single { YoutubedlHelper(get(), get()) }
    single { VideoServiceLocal(get(), get()) }
    single { AdBlockHostsRemoteDataSource(get(), get(), get()) }

    // --- Helpers ---
    single { PreferenceHelper(androidContext()) }
    single { FileUtil() }
    single { SystemUiController() }
    single { IntentUtil(get()) }
    single { MediaPermissionChecker(androidContext()) }
    single { NotificationsHelper(androidContext(), get()) }
    single<BaseSchedulers> { BaseSchedulersImpl() }
    single { CustomProxyController(get(), get()) }
    single { OkHttpProxyClient(get(), get()) }

    // --- Repositories ---
    single { TabModelRepository(get()) }
    single { VideoTaskItemRepository(get()) }
    single<ProgressRepository>(LocalProgressSource) { ProgressLocalDataSource(get()) }
    single<ProgressRepository> { ProgressRepositoryImpl(get(LocalProgressSource)) }

    // --- WorkManager ---
    single<WorkerFactory> { DaggerWorkerFactory(get(), get(), get(), get(), get(), get()) }
}
