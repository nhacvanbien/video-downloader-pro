package com.smarttool.videodownloader.feature.library.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.core.file.FileUtil
import com.smarttool.videodownloader.core.SingleLiveEvent
import com.smarttool.videodownloader.feature.library.domain.model.SortState
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PrivateVideoViewModel constructor(
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val fileUtil: FileUtil,
) :
    ViewModel() {

    val sortStateObservable: MutableLiveData<SortState> = MutableLiveData(SortState.NAME)

    val fileTabLiveData: MutableLiveData<Int> = MutableLiveData(0)

    companion object {
        const val FILE_EXIST_ERROR_CODE = 1
        const val FILE_INVALID_ERROR_CODE = 2
    }

    val renameErrorEvent = SingleLiveEvent<Int>()
    val shareEvent = SingleLiveEvent<Uri>()

    val searchCharObservable: MutableLiveData<String> = MutableLiveData("")
    val fileType: MutableLiveData<String> = MutableLiveData("")

    suspend fun queryVideoTaskItem(): LiveData<List<VideoTaskItem>> {
        return sortStateObservable.switchMap { sortState ->
            return@switchMap searchCharObservable.switchMap { query ->
                return@switchMap fileType.switchMap { type ->
                    videoTaskItemRepository.queryVideoTaskItem(
                        type == "all",
                        type,
                        query,
                        sortState.value
                    )
                }

            }
        }
    }

    private fun isFileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    suspend fun matchAndRemoveDeletedFiles() {
        withContext(Dispatchers.IO) {
            val allVideos = videoTaskItemRepository.getAllVideoTaskItems()
            val deletedItems = mutableListOf<VideoTaskItem>()

            for (item in allVideos) {
                if (!isFileExists(item.filePath)) {
                    deletedItems.add(item)
                }
            }

            if (deletedItems.isNotEmpty()) {
                videoTaskItemRepository.deleteVideoTaskItems(deletedItems)
            }
        }
    }

    suspend fun queryVideoTaskItemSecurity(): LiveData<List<VideoTaskItem>> {
        return sortStateObservable.switchMap { sortState ->
            return@switchMap searchCharObservable.switchMap { query ->
                return@switchMap fileType.switchMap { type ->
                    videoTaskItemRepository.queryVideoTaskItemSecurity(
                        type == "all",
                        type,
                        query,
                        sortState.value
                    )
                }

            }
        }
    }

    suspend fun insertVideoTaskItem(videoTaskItem: VideoTaskItem) {
        videoTaskItemRepository.insertVideoTaskItem(videoTaskItem)
    }


    suspend fun updateIsCheckSecurity(id: String, isSecurity: Boolean) {
        videoTaskItemRepository.updateIsCheckSecurity(id, isSecurity)
    }

    suspend fun renameVideo(context: Context, id: String, filePath: String, newName: String) {
        if (newName.isNotEmpty() && !videoTaskItemRepository.isFileNameVideoExists(newName)) {
//            val exists = fileUtil.isUriExists(context, uri)
//            if (exists) {
//                val isFileWithNameNotExists =
//                    fileUtil.isFileWithNameNotExists(context, uri, newName)
//                if (isFileWithNameNotExists) {
//                    val newMediaNameUri = fileUtil.renameMedia(context, uri, newName)
//                    if (newMediaNameUri != null) {
//                        newMediaNameUri.second.path?.let {
//                            videoTaskItemRepository.updateNameVideoTaskItem(
//                                id, newMediaNameUri.first,
//                                it
//                            )
//                        }
//                        return
//                    }
//                }
//
//                renameErrorEvent.value = FILE_EXIST_ERROR_CODE
//                return
//            }

            val newMediaNameUri = fileUtil.renameMedia(context, filePath, newName)
            if (newMediaNameUri != null) {
                newMediaNameUri.second.let {
                    videoTaskItemRepository.updateNameVideoTaskItem(
                        id, newMediaNameUri.first,
                        it
                    )
                }

                return

                renameErrorEvent.value = FILE_EXIST_ERROR_CODE

            }
        }

        renameErrorEvent.value = FILE_INVALID_ERROR_CODE
    }

    suspend fun renameImage(context: Context, id: String, filePath: String, newName: String) {
        if (newName.isNotEmpty() && !videoTaskItemRepository.isFileNameImageExists(newName)) {

            val newMediaNameUri = fileUtil.renameImage(context, filePath, newName)
            if (newMediaNameUri != null) {
                newMediaNameUri.second.let {
                    videoTaskItemRepository.updateNameVideoTaskItem(
                        id, newMediaNameUri.first,
                        it
                    )
                }

                return

                renameErrorEvent.value = FILE_EXIST_ERROR_CODE

            }
        }

        renameErrorEvent.value = FILE_INVALID_ERROR_CODE
    }

    suspend fun deleteVideoTaskItem(videoTaskItem: VideoTaskItem) {

        videoTaskItemRepository.deleteVideoTaskItem(videoTaskItem)

    }


    fun queryVideoSecurityTaskItem(): LiveData<List<VideoTaskItem>> {
        return videoTaskItemRepository.getAllVideoSecurityTaskItem()
    }

    fun findVideoTaskItemByName(name: String): VideoTaskItem {
        return videoTaskItemRepository.findVideoTaskItemByName(name)
    }

    suspend fun resetSecurityFlag() {
        videoTaskItemRepository.resetSecurityFlag()
    }

    suspend fun findVideoByName(downloadFilename: String?): VideoTaskItem? =
        withContext(Dispatchers.IO) {
            downloadFilename?.let { findVideoTaskItemByName(it) }
        }
}