package com.smarttool.videodownloader.feature.downloads.data

import android.content.Context
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.feature.downloads.domain.DownloaderGateway
import com.smarttool.videodownloader.data.downloader.custom_downloader.CustomRegularDownloader
import com.smarttool.videodownloader.data.downloader.youtubedl_downloader.YoutubeDlDownloader

/**
 * Routes each operation to the engine that owns the item: regular HTTP downloads go
 * to [CustomRegularDownloader], everything else (m3u8/extractor-backed) to
 * [YoutubeDlDownloader]. This is the only place that distinction is made.
 */
class AndroidDownloaderGateway(
    private val context: Context,
) : DownloaderGateway {

    override fun start(videoInfo: VideoInfo) {
        if (videoInfo.isRegularDownload) {
            CustomRegularDownloader.addDownload(context, videoInfo)
        } else {
            YoutubeDlDownloader.startDownload(context, videoInfo)
        }
    }

    override fun pause(progressInfo: ProgressInfo) {
        if (progressInfo.videoInfo.isRegularDownload) {
            CustomRegularDownloader.pauseDownload(context, progressInfo)
        } else {
            YoutubeDlDownloader.pauseDownload(context, progressInfo)
        }
    }

    override fun resume(progressInfo: ProgressInfo) {
        if (progressInfo.videoInfo.isRegularDownload) {
            CustomRegularDownloader.resumeDownload(context, progressInfo)
        } else {
            YoutubeDlDownloader.resumeDownload(context, progressInfo)
        }
    }

    override fun cancel(progressInfo: ProgressInfo, removeFile: Boolean) {
        if (progressInfo.videoInfo.isRegularDownload) {
            CustomRegularDownloader.cancelDownload(context, progressInfo, removeFile)
        } else {
            YoutubeDlDownloader.cancelDownload(context, progressInfo, removeFile)
        }
    }

    override fun stopAndSave(progressInfo: ProgressInfo) {
        // Only the youtube-dl engine supports keeping a partial file; regular
        // downloads have no equivalent and are left running.
        if (!progressInfo.videoInfo.isRegularDownload) {
            YoutubeDlDownloader.stopAndSaveDownload(context, progressInfo)
        }
    }
}
