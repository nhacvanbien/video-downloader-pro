package com.smarttool.videodownloader.feature.media.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskItem
import com.smarttool.videodownloader.data.repository.VideoTaskItemRepository
import com.smarttool.videodownloader.feature.library.domain.usecase.DeleteMediaUseCase
import com.smarttool.videodownloader.feature.library.domain.usecase.RenameMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backs the swipeable image gallery. Rather than threading the tapped-from list through
 * Navigation-Compose (there is no bridge for passing complex objects between
 * destinations in this app — every other player screen re-resolves its item from
 * [VideoTaskItemRepository] by file path, see [MediaViewModel.load]), this re-derives the
 * sibling list itself: every image row sharing the tapped item's private/public scope
 * ([VideoTaskItem.isSecurity]), so a public gallery can never swipe into the private one.
 */
class ImageGalleryViewModel(
    private val videoTaskItemRepository: VideoTaskItemRepository,
    private val deleteMedia: DeleteMediaUseCase,
    private val renameMedia: RenameMediaUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageGalleryContract.State())
    val uiState: StateFlow<ImageGalleryContract.State> = _uiState.asStateFlow()

    private val _effect = Channel<ImageGalleryContract.Effect>(Channel.BUFFERED)
    val effect: Flow<ImageGalleryContract.Effect> = _effect.receiveAsFlow()

    fun onEvent(event: ImageGalleryContract.Event) {
        when (event) {
            is ImageGalleryContract.Event.Load -> load(event.title, event.showMoreAction, event.filePath)
            is ImageGalleryContract.Event.PageChanged -> {
                _uiState.update { it.copy(currentIndex = event.index) }
            }
            ImageGalleryContract.Event.ShowMoreMenu -> _uiState.update { it.copy(moreMenuVisible = true) }
            ImageGalleryContract.Event.HideMoreMenu -> _uiState.update { it.copy(moreMenuVisible = false) }
            is ImageGalleryContract.Event.Rename -> rename(event.context, event.newName)
            ImageGalleryContract.Event.Delete -> delete()
        }
    }

    private fun load(title: String, showMoreAction: Boolean, filePath: String) {
        _uiState.value = ImageGalleryContract.State(initialTitle = title, showMoreAction = showMoreAction)

        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) { videoTaskItemRepository.getAllVideoTaskItems() }
            val tapped = all.firstOrNull { it.filePath == filePath }

            val images = if (tapped != null) {
                all.filter { it.mimeType.startsWith("image") && it.isSecurity == tapped.isSecurity }
                    .sortedByDescending { it.fileDate }
            } else {
                emptyList()
            }

            val startIndex = images.indexOfFirst { it.filePath == filePath }.coerceAtLeast(0)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    images = images,
                    startIndex = startIndex,
                    currentIndex = startIndex,
                )
            }
        }
    }

    private fun rename(context: Context, newName: String) {
        val state = _uiState.value
        val item = state.currentItem ?: return

        viewModelScope.launch {
            val success = renameMedia(context, item, newName, isImage = true)

            if (success) {
                val renamed = item.copy(fileName = newName, title = newName)
                _uiState.update {
                    it.copy(
                        moreMenuVisible = false,
                        images = it.images.replaceAt(it.currentIndex, renamed),
                    )
                }
            } else {
                _effect.trySend(ImageGalleryContract.Effect.RenameFailed)
            }
        }
    }

    private fun delete() {
        val state = _uiState.value
        val item = state.currentItem ?: return

        viewModelScope.launch {
            deleteMedia(item)

            val remaining = state.images.filterNot { it.mId == item.mId }

            if (remaining.isEmpty()) {
                _uiState.update { it.copy(moreMenuVisible = false, images = emptyList()) }
                _effect.trySend(ImageGalleryContract.Effect.AllImagesDeleted)
                return@launch
            }

            val nextIndex = state.currentIndex.coerceAtMost(remaining.size - 1)
            _uiState.update {
                it.copy(moreMenuVisible = false, images = remaining, currentIndex = nextIndex)
            }
            _effect.trySend(ImageGalleryContract.Effect.ScrollToPage(nextIndex))
        }
    }
}

private fun List<VideoTaskItem>.replaceAt(index: Int, value: VideoTaskItem): List<VideoTaskItem> =
    toMutableList().also { it[index] = value }
