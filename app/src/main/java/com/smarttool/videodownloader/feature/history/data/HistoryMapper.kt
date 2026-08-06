package com.smarttool.videodownloader.feature.history.data

import com.smarttool.videodownloader.data.network.entity.HistoryItem
import com.smarttool.videodownloader.feature.history.domain.model.HistoryEntry

fun HistoryItem.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    title = title.orEmpty(),
    url = url,
    datetime = datetime,
    isBookmark = isBookmark,
    favicon = favicon,
)

fun HistoryEntry.toEntity(): HistoryItem = HistoryItem(
    id = id,
    title = title,
    url = url,
    datetime = datetime,
    isBookmark = isBookmark,
    favicon = favicon,
)
