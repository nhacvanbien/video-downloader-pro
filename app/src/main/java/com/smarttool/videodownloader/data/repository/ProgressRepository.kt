package com.smarttool.videodownloader.data.repository

import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import io.reactivex.rxjava3.core.Flowable

interface ProgressRepository {

    fun getProgressInfos(): Flowable<List<ProgressInfo>>

    fun saveProgressInfo(progressInfo: ProgressInfo)

    fun deleteProgressInfo(progressInfo: ProgressInfo)
}
class ProgressRepositoryImpl  constructor(
    private val localDataSource: ProgressRepository
) : ProgressRepository {
    private var lastSavedInfo: ProgressInfo? = null

    private var lastDeletedInfo: ProgressInfo? = null


    override fun getProgressInfos(): Flowable<List<ProgressInfo>> {
        return localDataSource.getProgressInfos()
    }

    override fun saveProgressInfo(progressInfo: ProgressInfo) {
        if (progressInfo.hashCode() != lastSavedInfo.hashCode()) {
            lastSavedInfo = progressInfo
            localDataSource.saveProgressInfo(progressInfo)
        }
    }

    override fun deleteProgressInfo(progressInfo: ProgressInfo) {
        if (progressInfo.hashCode() != lastDeletedInfo.hashCode()) {
            lastDeletedInfo = progressInfo
            localDataSource.deleteProgressInfo(progressInfo)
        }
    }
}