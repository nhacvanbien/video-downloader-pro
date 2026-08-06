package com.smarttool.videodownloader.feature.downloads.domain

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo

/**
 * The domain boundary over the download engines in `util/downloaders`.
 *
 * Before the migration, ViewModels and Activities reached straight into
 * `CustomRegularDownloader` / `YoutubeDlDownloader` and had to know which engine a
 * given item belonged to. Everything above the data layer now depends on this
 * interface instead, and the engine choice lives in the single implementation.
 */
interface DownloaderGateway {
    fun start(videoInfo: VideoInfo)

    fun pause(progressInfo: ProgressInfo)

    fun resume(progressInfo: ProgressInfo)

    fun cancel(progressInfo: ProgressInfo, removeFile: Boolean)

    /** Ends the download but keeps whatever has been written so far. */
    fun stopAndSave(progressInfo: ProgressInfo)
}
