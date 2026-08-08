package com.smarttool.videodownloader.feature.downloads.presentation

import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.data.network.entity.ProgressInfo
import com.smarttool.videodownloader.data.network.entity.VideoInfo

interface ProcessingContract {
    sealed interface Event : UiEvent {
        data class Start(val videoInfo: VideoInfo) : Event
        data class Pause(val progressInfo: ProgressInfo) : Event
        data class Resume(val progressInfo: ProgressInfo) : Event
        data class Cancel(val progressInfo: ProgressInfo, val removeFile: Boolean) : Event
        data class StopAndSave(val progressInfo: ProgressInfo) : Event
    }
}
