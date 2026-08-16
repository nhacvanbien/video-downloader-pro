package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.feature.library.domain.model.MediaFilter

private val FilterOptions = listOf(
    MediaFilter.All to R.string.string_all,
    MediaFilter.Video to R.string.string_video,
    MediaFilter.Audio to R.string.string_audio,
    MediaFilter.Image to R.string.string_image,
)

/**
 * Chip-style selector for [MediaFilter], shared by the Downloads tab
 * ([com.smarttool.videodownloader.feature.downloads.presentation.DownloadsScreen]) and the
 * library screens (Downloaded/Private/SelectVideo, all backed by [LibraryScreen]).
 */
@Composable
fun MediaFilterChipRow(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterOptions.forEach { (filter, labelRes) ->
            val active = filter == selected

            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = if (active) Pri else Muted,
                modifier = Modifier
                    .clip(ShapePill)
                    .background(if (active) PriSoft else Surface)
                    .border(1.dp, if (active) PriSoft else Border, ShapePill)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
