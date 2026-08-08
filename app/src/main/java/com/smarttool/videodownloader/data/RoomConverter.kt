package com.smarttool.videodownloader.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.smarttool.videodownloader.data.network.entity.VideoInfo

class RoomConverter {

    @TypeConverter
    fun convertJsonToVideo(json: String): VideoInfo {
        return Gson().fromJson(json, VideoInfo::class.java)
    }

    @TypeConverter
    fun convertListVideosToJson(video: VideoInfo): String {
        return Gson().toJson(video)
    }
}