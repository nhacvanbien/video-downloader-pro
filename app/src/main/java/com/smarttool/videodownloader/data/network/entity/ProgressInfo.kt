package com.smarttool.videodownloader.data.network.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.data.RoomConverter
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState
import java.util.UUID

@Entity(tableName = "ProgressInfo")
@TypeConverters(RoomConverter::class)
data class ProgressInfo(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),

    var downloadId: Long = 0,

    @TypeConverters(RoomConverter::class)
    var videoInfo: VideoInfo,

    @Suppress("DEPRECATION")
    @Deprecated("bytesDownloaded deprecated use progressDownloaded instead")
    var bytesDownloaded: Int = 0,

    @Suppress("DEPRECATION")
    @Deprecated("bytesTotal deprecated use progressTotal instead")
    var bytesTotal: Int = 0,

    @ColumnInfo(defaultValue = "0")
    var progressDownloaded: Long = 0,

    @ColumnInfo(defaultValue = "0")
    var progressTotal: Long = 0,

    @ColumnInfo(defaultValue = "0")
    var speedBytesPerSecond: Long = 0,

    var downloadStatus: Int = -1,

    var isLive: Boolean = false,

    var isM3u8: Boolean = false,

    var fragmentsDownloaded: Int = 0,

    var fragmentsTotal: Int = 1,

    @ColumnInfo(name = "infoLine")
    var infoLine: String = ""
) {
    var progress: Int = 0
        get() {
            if (progressTotal <= 0) return 0
            // Workers write progressDownloaded/progressTotal from two independently-sourced
            // async ticks (e.g. yt-dlp switching from a video-format download to the smaller
            // audio-format one mid-merge); coerce here so a transient downloaded > total never
            // leaks into the UI as a percentage above 100.
            return (progressDownloaded * 100f / progressTotal).toInt().coerceIn(0, 100)
        }

    var progressSize: String = ""
        get() {
            return FileUtil.getFileSizeReadable(progressDownloaded.toDouble()) + "/" + FileUtil.getFileSizeReadable(
                progressTotal.toDouble()
            ) + " - $downloadStatusFormatted"
        }

    var speedFormatted: String = ""
        get() = if (downloadStatus == VideoTaskState.DOWNLOADING && speedBytesPerSecond > 0) {
            "${FileUtil.getFileSizeReadable(speedBytesPerSecond.toDouble())}/s"
        } else {
            ""
        }

    // PENDING/PREPARE cover the queueing + yt-dlp metadata-extraction phase before any byte
    // count exists; a DOWNLOADING row with progressTotal still 0 is the same gap for the
    // regular HTTP downloader (Content-Length not known yet) and for yt-dlp between emitting
    // PREPARE and its first real progress line. In all of these the % text would otherwise
    // read a stale/undefined "0%" while nothing visibly happens.
    val isFetchingInfo: Boolean
        get() = downloadStatus == VideoTaskState.PENDING ||
            downloadStatus == VideoTaskState.PREPARE ||
            (downloadStatus == VideoTaskState.DOWNLOADING && progressTotal <= 0)

    var downloadStatusFormatted: String = ""
        get() = when (downloadStatus) {
            VideoTaskState.DOWNLOADING -> "downloading"
            VideoTaskState.SUCCESS -> "success"
            VideoTaskState.PAUSE -> "pause"
            VideoTaskState.PENDING -> "pending"
            VideoTaskState.PREPARE -> "prepare"
            VideoTaskState.ENOSPC -> "failed"
            VideoTaskState.ERROR -> "failed"
            VideoTaskState.WAITING_FOR_WIFI -> "waiting_for_wifi"
            else -> "undefined"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProgressInfo

        if (id != other.id) return false
        if (downloadId != other.downloadId) return false
        if (videoInfo != other.videoInfo) return false
        if (progressDownloaded != other.progressDownloaded) return false
        if (progressTotal != other.progressTotal) return false
        if (speedBytesPerSecond != other.speedBytesPerSecond) return false
        if (downloadStatus != other.downloadStatus) return false
        if (isM3u8 != other.isM3u8) return false
        if (fragmentsDownloaded != other.fragmentsDownloaded) return false
        if (fragmentsTotal != other.fragmentsTotal) return false
        if (infoLine != other.infoLine) return false
        if (progress != other.progress) return false
        if (progressSize != other.progressSize) return false
        return downloadStatusFormatted == other.downloadStatusFormatted
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + downloadId.hashCode()
        result = 31 * result + videoInfo.hashCode()
        result = 31 * result + progressDownloaded.hashCode()
        result = 31 * result + progressTotal.hashCode()
        result = 31 * result + speedBytesPerSecond.hashCode()
        result = 31 * result + downloadStatus
        result = 31 * result + isM3u8.hashCode()
        result = 31 * result + fragmentsDownloaded
        result = 31 * result + fragmentsTotal
        result = 31 * result + infoLine.hashCode()
        return result
    }
}