package com.smarttool.videodownloader.data.network.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "VideoFormat")
data class VideoFormatEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "asr")
    @SerializedName("asr")
    @Expose
    val asr: Int = 0,

    @ColumnInfo(name = "tbr")
    @SerializedName("tbr")
    @Expose
    val tbr: Int = 0,

    @ColumnInfo(name = "abr")
    @SerializedName("abr")
    @Expose
    val abr: Int = 0,

    @ColumnInfo(name = "format")
    @SerializedName("format")
    @Expose
    val format: String? = null,

    @ColumnInfo(name = "formatId")
    @SerializedName("formatId")
    @Expose
    val formatId: String? = null,

    @ColumnInfo(name = "formatNote")
    @SerializedName("formatNote")
    @Expose
    val formatNote: String? = null,

    @ColumnInfo(name = "ext")
    @SerializedName("ext")
    @Expose
    val ext: String? = null,

    @ColumnInfo(name = "preference")
    @SerializedName("preference")
    @Expose
    val preference: Int = 0,

    @ColumnInfo(name = "vcodec")
    @SerializedName("vcodec")
    @Expose
    val vcodec: String? = null,

    @ColumnInfo(name = "acodec")
    @SerializedName("acodec")
    @Expose
    val acodec: String? = null,

    @ColumnInfo(name = "width")
    @SerializedName("width")
    @Expose
    val width: Int = 0,

    @ColumnInfo(name = "height")
    @SerializedName("height")
    @Expose
    val height: Int = 0,

    @ColumnInfo(name = "fileSize")
    @SerializedName("fileSize")
    @Expose
    val fileSize: Long = 0,

    @ColumnInfo(name = "fileSizeApproximate")
    @SerializedName("fileSizeApproximate")
    @Expose
    val fileSizeApproximate: Long = 0,

    @ColumnInfo(name = "fps")
    @SerializedName("fps")
    @Expose
    val fps: Int = 0,

    @ColumnInfo(name = "url")
    @SerializedName("url")
    @Expose
    val url: String? = null,

    @ColumnInfo(name = "manifestUrl")
    @SerializedName("manifestUrl")
    @Expose
    val manifestUrl: String? = null,

    @ColumnInfo(name = "httpHeaders")
    @SerializedName("httpHeaders")
    @Expose
    val httpHeaders: Map<String, String>? = null
) {
    /**
     * False when yt-dlp returned this entry with formatId "0" and nothing else usable
     * (no resolution, no codec) — seen when it parses a single-quality HLS media
     * playlist (e.g. `.../240p/index-v1-a1.m3u8`) directly instead of the master
     * playlist that actually carries the `#EXT-X-STREAM-INF` resolution/codec tags.
     * That leaves a format entry that's real (a working stream URL) but has nothing
     * meaningful to label or pick by.
     */
    val isUsable: Boolean
        get() = formatId != "0" || width > 0 || height > 0 ||
            !vcodec.isNullOrBlank() || !acodec.isNullOrBlank()
}

data class VideFormatEntityList(
    val formats: List<VideoFormatEntity>
)