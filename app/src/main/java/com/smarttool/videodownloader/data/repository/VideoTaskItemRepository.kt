package com.smarttool.videodownloader.data.repository

import androidx.lifecycle.LiveData
import com.smarttool.videodownloader.data.dao.VideoTaskItemDao
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

class VideoTaskItemRepository  constructor(
    private val videoTaskItemDao: VideoTaskItemDao
) {

    fun getAllVideoTaskItem(): LiveData<List<VideoTaskItem>> {
        return videoTaskItemDao.getVideoTaskItem()
    }

    fun getAllVideoTaskItems(): List<VideoTaskItem> {
        return videoTaskItemDao.getAllVideoTaskItems()
    }

    fun deleteVideoTaskItems(videoTaskItems: List<VideoTaskItem>) {
        videoTaskItemDao.deleteVideoTaskItems(videoTaskItems)
    }

    fun queryVideoTaskItem(
        isAll: Boolean,
        typeItem: String?,
        textSearch: String,
        typeSort: Int
    ): LiveData<List<VideoTaskItem>> {
        return videoTaskItemDao
            .getLiveDataVideoTaskItemByTextSearch(
                if (isAll) 1 else 0,
                typeItem,
                "%" + textSearch.trim() + "%",
                typeSort
            )
    }

    fun queryVideoTaskItemSecurity(
        isAll: Boolean?,
        typeItem: String?,
        textSearch: String,
        typeSort: Int
    ): LiveData<List<VideoTaskItem>> {
        return videoTaskItemDao
            .getLiveDataVideoTaskItemSecurityByTextSearch(
                isAll, typeItem,
                "%" + textSearch.trim() + "%",
                typeSort
            )
    }

    fun getAllVideoSecurityTaskItem(): LiveData<List<VideoTaskItem>> {
        return videoTaskItemDao.getVideoSecurityTaskItem()
    }

    suspend fun insertVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemDao.insertVideoTaskItem(videoTaskItem)
    }

    suspend fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemDao.deleteVideoTaskItem(videoTaskItem)
    }

    suspend fun updateIsCheckSecurity(id: String, isSecurity: Boolean) {
        videoTaskItemDao.updateIsSecurity(id, isSecurity)
    }

    suspend fun updateNameVideoTaskItem(id: String, newName: String, newPath: String) {
        videoTaskItemDao.updateNameVideoTaskItem(id, newName, newPath)
    }

    suspend fun isFileNameImageExists(newName: String): Boolean {
        return videoTaskItemDao.isFileNameImageExists(newName) > 0
    }

    suspend fun isFileNameVideoExists(newName: String): Boolean {
        return videoTaskItemDao.isFileNameVideoExists(newName) > 0
    }

    fun findVideoTaskItemByName(name: String): VideoTaskItem {
        return videoTaskItemDao.findVideoTaskItemByName(name)
    }

    suspend fun resetSecurityFlag() {
        videoTaskItemDao.resetSecurityFlag()
    }
}