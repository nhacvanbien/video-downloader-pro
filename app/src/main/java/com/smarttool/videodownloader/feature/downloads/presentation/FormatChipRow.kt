package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.feature.downloads.domain.model.VideoFormatOption

private val UnselectedText = Color(0xFF404040)
private val UnselectedBorder = Color(0xFFE3E5E8)

/** Horizontal quality picker, replacing the old CandidatesList RecyclerView. */
@Composable
fun FormatChipRow(
    formats: List<VideoFormatOption>,
    selectedFormat: String?,
    onSelect: (VideoFormatOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(formats, key = { it.format }) { option ->
            val selected = option.format == selectedFormat

            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                color = if (selected) Primary else UnselectedText,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Primary.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) Primary else UnselectedBorder,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
