package com.smarttool.videodownloader.data.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.smarttool.videodownloader.data.network.entity.VideoInfo

data class VideoInfoWrapper(
    @SerializedName("info")
    @Expose
    var videoInfo: VideoInfo?
)