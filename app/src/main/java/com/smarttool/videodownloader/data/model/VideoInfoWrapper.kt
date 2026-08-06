package com.smarttool.videodownloader.data.model

import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class VideoInfoWrapper(
    @SerializedName("info")
    @Expose
    var videoInfo: VideoInfo?
)