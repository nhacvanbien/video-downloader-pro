package com.smarttool.videodownloader.data.downloader.generic_downloader.workers

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.smarttool.videodownloader.data.repository.ProgressRepository
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.notification.NotificationsHelper
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import com.smarttool.videodownloader.core.network.CustomProxyController
import com.smarttool.videodownloader.core.network.OkHttpProxyClient
import com.smarttool.videodownloader.data.downloader.generic_downloader.GenericDownloader
import com.smarttool.videodownloader.data.downloader.generic_downloader.NotificationReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import timber.log.Timber

open class GenericDownloadWorkerWrapper  constructor(
    appContext: Context, workerParams: WorkerParameters
) : GenericDownloadWorker(appContext, workerParams) {
    lateinit var progressRepository: ProgressRepository
    lateinit var fileUtil: FileUtil
    lateinit var notificationsHelper: NotificationsHelper
    lateinit var proxyController: CustomProxyController
    lateinit var proxyOkHttpClient: OkHttpProxyClient
    lateinit var preferences: AppPreferencesDataSource

    override fun onDownloadPrepare(item: VideoTaskItem?) {
        super.onDownloadPrepare(item)

        GenericDownloader.downloadListener?.onDownloadPrepare(item)
    }

    override fun onDownloadProgress(item: VideoTaskItem?) {
        if (item != null) {
            if (item.taskState == VideoTaskState.SUCCESS) {
                return
            }
        }
        super.onDownloadProgress(item)

        GenericDownloader.downloadListener?.onDownloadProgress(item)
    }

    override fun onDownloadPause(item: VideoTaskItem?) {
        Timber.i("Download paused: $item")

        if (item?.taskState == VideoTaskState.SUCCESS) {
            return
        }

        super.onDownloadPause(item)

        GenericDownloader.downloadListener?.onDownloadPause(item)
    }

    override fun onDownloadError(item: VideoTaskItem?) {
        Timber.w("Download failed: $item")
        super.onDownloadError(item)

        GenericDownloader.downloadListener?.onDownloadError(item)
    }

    override fun onDownloadSuccess(item: VideoTaskItem?) {
        super.onDownloadSuccess(item)
        Timber.i("Download succeeded: $item")

        val infos = runBlocking { progressRepository.getProgressInfos().first() }
        if (item != null) {
            infos.find { it.id == item.url }
                ?.let { it1 -> progressRepository.deleteProgressInfo(it1) }
        }

        val builderPair = item?.let { notificationsHelper.createNotificationBuilder(it) }

        if (builderPair != null) {
            applicationContext.sendBroadcast(
                Intent(applicationContext, NotificationReceiver::class.java).setAction(
                    NotificationReceiver.ACTION_SEND_NOTIFICATION
                ).putExtra(NotificationReceiver.EXTRA_ID, item?.url.hashCode() + 1)
                    .putExtra(NotificationReceiver.EXTRA_NOTIFICATION, builderPair.second.build())
            )
        }

        GenericDownloader.downloadListener?.onDownloadSuccess(item)

        finishWork(item)
    }

    override fun createForegroundInfo(task: VideoTaskItem): ForegroundInfo {
        val pairData = notificationsHelper.createNotificationBuilder(task)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                pairData.first,
                pairData.second.build(),
                FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(pairData.first, pairData.second.build())
        }
    }

    fun showNotification(id: Int, notification: NotificationCompat.Builder) {
        notificationsHelper.showNotification(Pair(id, notification))
    }

    fun showNotificationAsync(id: Int, notification: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForegroundAsync(
                ForegroundInfo(
                    id, // taskId.hashcode()
                    notification.build(),
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            )
        } else {
            setForegroundAsync(
                ForegroundInfo(
                    id, // taskId.hashcode()
                    notification.build()
                )
            )
        }
    }

    override fun handleAction(
        action: String,
        task: VideoTaskItem,
        headers: Map<String, String>,
        isFileRemove: Boolean
    ) {
        when (action) {
            GenericDownloader.DownloaderActions.DOWNLOAD -> {
                throw Error("Unimplemented")
            }

            GenericDownloader.DownloaderActions.CANCEL -> {
                notificationsHelper.hideNotification(task.url.hashCode())
                getContinuation().resume(Result.success())
            }

            GenericDownloader.DownloaderActions.PAUSE -> {
                getContinuation().resume(Result.success())
            }

            GenericDownloader.DownloaderActions.RESUME -> {
                throw Error("Unimplemented")
            }
        }
    }

    override fun finishWork(item: VideoTaskItem?) {
        setDone()
    }
}
