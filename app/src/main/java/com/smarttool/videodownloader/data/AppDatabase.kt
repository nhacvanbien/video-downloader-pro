package com.smarttool.videodownloader.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smarttool.videodownloader.data.dao.AdHostDao
import com.smarttool.videodownloader.data.dao.HistoryDao
import com.smarttool.videodownloader.data.dao.ProgressDao
import com.smarttool.videodownloader.data.dao.TabModelDao
import com.smarttool.videodownloader.data.dao.VideoTaskItemDao
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.network.entity.AdHost
import com.smarttool.videodownloader.data.network.entity.DownloadUrlsConverter
import com.smarttool.videodownloader.data.network.entity.FormatsConverter
import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.feature.tab.domain.model.TabModel


const val DB_VERSION = 1

@Database(
    entities = [HistoryItem::class, AdHost::class, ProgressInfo::class, VideoTaskItem::class, TabModel::class],
    version = DB_VERSION,
)
@TypeConverters(FormatsConverter::class, DownloadUrlsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    abstract fun tabModelDao(): TabModelDao

    abstract fun adHostDao(): AdHostDao

    abstract fun progressDao(): ProgressDao

    abstract fun videoTaskItemDao(): VideoTaskItemDao

}