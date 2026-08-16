package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.downloads.domain.model.VideoFormatOption

private val UnselectedText = TextColor
private val UnselectedBorder = Border

/** Wrapping quality picker, replacing the old CandidatesList RecyclerView. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormatChipRow(
    formats: List<VideoFormatOption>,
    selectedFormat: String?,
    onSelect: (VideoFormatOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        formats.forEach { option ->
            val selected = option.format == selectedFormat
            val color = if (selected) Pri else UnselectedText

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(ShapePill)
                    .background(Color.Transparent)
                    .border(
                        width = if (selected) 1.dp else 1.dp,
                        color = if (selected) Pri else UnselectedBorder,
                        shape = ShapePill,
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
            ) {
                if (option.isAudio) {
                    Image(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }

                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                    ),
                    color = color,
                )
            }
        }
    }
}
