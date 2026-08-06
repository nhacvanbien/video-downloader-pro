package com.smarttool.videodownloader.data.remote.service

import com.smarttool.videodownloader.data.network.entity.VideoInfo
import com.smarttool.videodownloader.data.repository.VideoRepository
import okhttp3.Request
class VideoRemoteDataSource  constructor(
    private val videoService: VideoService
) : VideoRepository {

    override fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        return videoService.getVideoInfo(url, isM3u8OrMpd)?.videoInfo
    }

    override fun saveVideoInfo(videoInfo: VideoInfo) {
    }
}