package com.smarttool.videodownloader.model

import android.net.Uri

data class LocalVideo(
    var id: Long,
    var uri: Uri,
    var name: String,
    var isChecked: Boolean = false,
    var isEditable: Boolean = false,
) {

    var size: String = ""
    var duration: String = ""
    var date: String = ""

    val thumbnailPath: Uri
        get() = uri

}