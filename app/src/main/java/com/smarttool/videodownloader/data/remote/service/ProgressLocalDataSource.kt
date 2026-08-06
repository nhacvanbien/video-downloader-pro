package com.smarttool.videodownloader.data.remote.service

import com.smarttool.videodownloader.data.dao.ProgressDao
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.repository.ProgressRepository
import io.reactivex.rxjava3.core.Flowable
class ProgressLocalDataSource  constructor(
    private val progressDao: ProgressDao
) : ProgressRepository {

    override fun getProgressInfos(): Flowable<List<ProgressInfo>> {
        return progressDao.getProgressInfos()
    }

    override fun saveProgressInfo(progressInfo: ProgressInfo) {
        progressDao.insertProgressInfo(progressInfo)
    }

    override fun deleteProgressInfo(progressInfo: ProgressInfo) {
        progressDao.deleteProgressInfo(progressInfo)
    }
}