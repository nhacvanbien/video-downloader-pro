package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.feature.library.domain.model.SortState

private val SORT_OPTION_ORDER = listOf(
    SortState.DATE_DESC,
    SortState.DATE,
    SortState.NAME,
    SortState.NAME_DESC,
    SortState.SIZE_DESC,
    SortState.SIZE,
)

/** Icon that stands in for [sort] anywhere it needs a compact, at-a-glance glyph (e.g. the list meta row). */
fun sortStateIconRes(sort: SortState): Int = when (sort) {
    SortState.DATE_DESC -> R.drawable.ic_sort_date_desc
    SortState.DATE -> R.drawable.ic_sort_date_asc
    SortState.NAME, SortState.NAME_DESC -> R.drawable.ic_sort_name
    SortState.SIZE_DESC, SortState.SIZE -> R.drawable.ic_sort_size
}

/**
 * Trimmed variant of [sortStateLabelRes] for the inline "Sort by: X" control, where the full
 * "Newest first"/"Largest first" phrasing would wrap.
 */
fun sortStateShortLabelRes(sort: SortState): Int = when (sort) {
    SortState.DATE_DESC -> R.string.string_sort_short_newest
    SortState.DATE -> R.string.string_sort_short_oldest
    SortState.NAME -> R.string.string_sort_short_name_az
    SortState.NAME_DESC -> R.string.string_sort_short_name_za
    SortState.SIZE_DESC -> R.string.string_sort_short_largest
    SortState.SIZE -> R.string.string_sort_short_smallest
}

private fun sortStateLabelRes(sort: SortState): Int = when (sort) {
    SortState.DATE_DESC -> R.string.string_sort_newest_first
    SortState.DATE -> R.string.string_sort_oldest_first
    SortState.NAME -> R.string.string_sort_name_az
    SortState.NAME_DESC -> R.string.string_sort_name_za
    SortState.SIZE_DESC -> R.string.string_sort_largest_first
    SortState.SIZE -> R.string.string_sort_smallest_first
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortSheet(
    selected: SortState,
    onSelect: (SortState) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        // The sheet composes in its own window, whose AbstractComposeView re-provides
        // LocalContext from the Activity — discarding the override installed around the
        // main composition. Without this the sheet keeps the launch-time language.
        AppLocaleProvider {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                Text(
                    text = stringResource(R.string.string_sort_by),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextColor,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                SORT_OPTION_ORDER.forEach { sort ->
                    val isSelected = sort == selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(ShapeMd)
                            .border(1.dp, if (isSelected) Pri else Border, ShapeMd)
                            .clickable { onSelect(sort) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(sortStateIconRes(sort)),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (isSelected) Pri else Muted),
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            text = stringResource(sortStateLabelRes(sort)),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                            color = if (isSelected) TextColor else Muted,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )

                        if (isSelected) {
                            Row(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clip(ShapePill)
                                    .background(Pri),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = PriInk,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
