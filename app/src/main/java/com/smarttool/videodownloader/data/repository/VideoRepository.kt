package com.smarttool.videodownloader.data.repository

import com.smarttool.videodownloader.data.network.entity.VideoInfo
import okhttp3.Request
import timber.log.Timber

interface VideoRepository {

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean = false): VideoInfo?

    fun saveVideoInfo(videoInfo: VideoInfo)
}
class VideoRepositoryImpl  constructor() {

    private val cachedVideos: MutableMap<String, VideoInfo> = mutableMapOf()

    fun getVideoInfo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        Timber.d("getVideoInfo: url=${url.url} cached=${cachedVideos.containsKey(url.url.toString())}")
        cachedVideos[url.url.toString()]?.let { return it }

        return getAndCacheRemoteVideo(url, isM3u8OrMpd)
    }

    fun saveVideoInfo(videoInfo: VideoInfo) {
        cachedVideos[videoInfo.originalUrl] = videoInfo
    }

    private fun getAndCacheRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo? {
        // Logic lấy video từ nguồn từ xa
        val videoInfo = fetchRemoteVideo(url, isM3u8OrMpd)
        if (videoInfo != null) {
            videoInfo.originalUrl = url.url.toString()
            cachedVideos[videoInfo.originalUrl] = videoInfo
        }
        return videoInfo
    }

    private fun fetchRemoteVideo(url: Request, isM3u8OrMpd: Boolean): VideoInfo {
        // Giả lập fetch video từ remote
        return VideoInfo(originalUrl = url.url.toString())
    }

}