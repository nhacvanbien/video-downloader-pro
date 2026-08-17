package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.MediaKind
import com.smarttool.videodownloader.core.ui.components.MediaThumbnail
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub
import com.smarttool.videodownloader.feature.downloads.domain.model.VideoFormatOption

/**
 * Picker for the videos detected on the current page — the Compose replacement for
 * the `VideoInfoAdapter` bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectedVideosSheet(
    videos: List<DetectedVideoUi>,
    onSelectFormat: (DetectedVideoUi, VideoFormatOption) -> Unit,
    onRename: (DetectedVideoUi) -> Unit,
    onPreview: (DetectedVideoUi) -> Unit,
    onDownload: (DetectedVideoUi, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppWhite,
    ) {
        // The sheet composes in its own window, whose AbstractComposeView re-provides
        // LocalContext from the Activity — discarding the override installed around the
        // main composition. Without this the sheet keeps the launch-time language.
        AppLocaleProvider {
            LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                itemsIndexed(videos, key = { _, v -> v.id }) { _, video ->
                    DetectedVideoRow(
                        video = video,
                        onSelectFormat = { onSelectFormat(video, it) },
                        onRename = { onRename(video) },
                        onPreview = { onPreview(video) },
                        onDownload = { isPrivate -> onDownload(video, isPrivate) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedVideoRow(
    video: DetectedVideoUi,
    onSelectFormat: (VideoFormatOption) -> Unit,
    onRename: () -> Unit,
    onPreview: () -> Unit,
    onDownload: (Boolean) -> Unit,
) {
    var isPrivate by rememberSaveable(video.id) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MediaThumbnail(
                filePath = video.thumbnailUrl.orEmpty(),
                mediaType = MediaKind.VIDEO,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPreview),
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (video.typeLabel.isNotEmpty()) {
                    Text(
                        text = video.typeLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSub,
                    )
                }

                video.readableSize?.let {
                    Text(
                        text = stringResource(R.string.string_download_size, it),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextSub,
                    )
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                modifier = Modifier.size(22.dp).clickable(onClick = onRename),
            )
        }

        val videoFormats = video.formats.filterNot { it.isAudio }
        val audioFormats = video.formats.filter { it.isAudio }

        if (videoFormats.isNotEmpty()) {
            Text(
                text = stringResource(R.string.string_video),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = TextSub,
                modifier = Modifier.padding(top = 10.dp),
            )
            FormatChipRow(
                formats = videoFormats,
                selectedFormat = video.selectedFormat,
                onSelect = onSelectFormat,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }

        if (audioFormats.isNotEmpty()) {
            Text(
                text = stringResource(R.string.string_audio),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = TextSub,
                modifier = Modifier.padding(top = 10.dp),
            )
            FormatChipRow(
                formats = audioFormats,
                selectedFormat = video.selectedFormat,
                onSelect = onSelectFormat,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable { isPrivate = !isPrivate },
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPrivate) Primary else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isPrivate) Primary else Border,
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isPrivate) {
                    Image(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(AppWhite),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.string_save_to_private_folder),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        val selectedIsAudio = video.formats.find { it.format == video.selectedFormat }?.isAudio == true
        val downloadLabel = if (selectedIsAudio) {
            R.string.string_download_audio
        } else {
            R.string.string_download_video
        }

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(120.dp))
                .background(Primary)
                .clickable { onDownload(isPrivate) }
                .height(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(downloadLabel),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                color = AppWhite,
                textAlign = TextAlign.Center,
            )
        }
    }
}
