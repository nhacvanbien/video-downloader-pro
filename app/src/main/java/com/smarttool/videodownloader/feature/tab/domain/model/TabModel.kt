package com.smarttool.videodownloader.feature.tab.domain.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "TabModel")
data class TabModel(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "_url")
    var url: String,

//    @ColumnInfo(name = "_title")
//    var title: String?,

//    private var iconBytes: Bitmap? = null,
    @ColumnInfo(name = "_is_selected")
    var isSelected: Boolean = true,

    @ColumnInfo(name = "_favicon", typeAffinity = ColumnInfo.BLOB)
    var favicon: ByteArray? = null
) {
    fun faviconBitmap(): Bitmap? {
        if (favicon == null) return null
        return BitmapFactory.decodeByteArray(favicon, 0, favicon?.size ?: 0)
    }

    /**
     * Identity is id + url; the favicon blob is deliberately excluded so a re-fetched
     * icon does not read as a different tab.
     *
     * The cast used to be to `HistoryItem`, which threw `ClassCastException` for every
     * comparison of two non-identical tabs — including the equality check a `StateFlow`
     * runs before emitting the tab list.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TabModel

        if (id != other.id) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }
}