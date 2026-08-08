package com.smarttool.videodownloader.data.downloader.generic_downloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.smarttool.videodownloader.data.repository.ProgressRepository
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.notification.NotificationsHelper
import com.smarttool.videodownloader.data.downloader.generic_downloader.workers.GenericDownloadWorker
import com.smarttool.videodownloader.data.downloader.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient

class DaggerWorkerFactory  constructor(
    private val progress: ProgressRepository,
    private val fileUtil: FileUtil,
    private val notificationsHelper: NotificationsHelper,
    private val proxyController: CustomProxyController,
    private val okHttpProxyClient: OkHttpProxyClient,
    private val preferences: AppPreferencesDataSource,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context, workerClassName: String, workerParameters: WorkerParameters
    ): CoroutineWorker? {
        val workerKlass = try {
            Class.forName(workerClassName)
        } catch (e: ClassNotFoundException) {
            // The class doesn't exist, let the default factory handle it.
            return null
        }

        // Check if the requested worker is one of ours.
        if (!GenericDownloadWorker::class.java.isAssignableFrom(workerKlass)) {
            // It's not our worker (e.g., OfflinePingSender), so let the default factory handle it.
            return null
        }

        // Now it's safe to cast and proceed with your custom instantiation logic.
        val targetKlass = workerKlass.asSubclass(GenericDownloadWorker::class.java)

        val constructor =
            targetKlass.getDeclaredConstructor(Context::class.java, WorkerParameters::class.java)
        val instance = constructor.newInstance(appContext, workerParameters)

        when (instance) {
            is GenericDownloadWorkerWrapper -> {
                instance.preferences = preferences
                instance.progressRepository = progress
                instance.fileUtil = fileUtil
                instance.notificationsHelper = notificationsHelper
                instance.proxyController = proxyController
                instance.proxyOkHttpClient = okHttpProxyClient
            }
        }

        return instance
    }
}