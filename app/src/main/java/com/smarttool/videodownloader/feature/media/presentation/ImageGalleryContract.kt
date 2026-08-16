package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem

interface ImageGalleryContract {
    data class State(
        val isLoading: Boolean = true,
        /** Every image sharing the tapped item's private/public scope, sorted newest first. */
        val images: List<VideoTaskItem> = emptyList(),
        val startIndex: Int = 0,
        val currentIndex: Int = 0,
        /** Only downloaded items expose the detail/more action. */
        val showMoreAction: Boolean = false,
        val moreMenuVisible: Boolean = false,
        /** Fallback title shown while [images] is still loading. */
        val initialTitle: String = "",
    ) : UiState {
        val currentItem: VideoTaskItem? get() = images.getOrNull(currentIndex)
    }

    sealed interface Event : UiEvent {
        data class Load(val title: String, val showMoreAction: Boolean, val filePath: String) : Event

        data class PageChanged(val index: Int) : Event

        data object ShowMoreMenu : Event

        data object HideMoreMenu : Event

        data class Rename(val context: Context, val newName: String) : Event

        data object Delete : Event
    }

    sealed interface Effect : UiEffect {
        data object RenameFailed : Effect

        /** The gallery ran out of images (last one just got deleted) — nothing left to view. */
        data object AllImagesDeleted : Effect

        data class ScrollToPage(val index: Int) : Effect
    }
}
