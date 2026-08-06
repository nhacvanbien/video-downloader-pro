package com.smarttool.videodownloader.feature.history.domain.model

data class HistoryEntry(
    val id: String,
    val title: String,
    val url: String,
    val datetime: Long,
    val isBookmark: Boolean,
    val favicon: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistoryEntry) return false
        return id == other.id && url == other.url && datetime == other.datetime
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + datetime.hashCode()
        return result
    }
}
