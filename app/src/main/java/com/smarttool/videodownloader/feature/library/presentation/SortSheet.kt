package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub
import com.smarttool.videodownloader.feature.library.domain.model.SortState

/** Each option shows a category plus the direction, mirroring the original sheet. */
private data class SortOption(
    val sort: SortState,
    val categoryRes: Int,
    val directionRes: Int,
)

private val SORT_OPTIONS = listOf(
    SortOption(SortState.NAME, R.string.string_name, R.string.string_a_z),
    SortOption(SortState.NAME_DESC, R.string.string_name, R.string.string_z_a),
    SortOption(SortState.DATE_DESC, R.string.string_last_modified, R.string.string_new_to_old),
    SortOption(SortState.DATE, R.string.string_last_modified, R.string.string_old_to_new),
    SortOption(SortState.SIZE_DESC, R.string.string_file_size, R.string.string_large_to_small),
    SortOption(SortState.SIZE, R.string.string_file_size, R.string.string_small_to_large),
)

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
        containerColor = AppWhite,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.string_sort_by),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            SORT_OPTIONS.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.sort) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(option.categoryRes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                        )

                        Text(
                            text = stringResource(option.directionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSub,
                        )
                    }

                    Image(
                        painter = painterResource(
                            if (option.sort == selected) {
                                R.drawable.ic_dot_selected
                            } else {
                                R.drawable.ic_dot_normal
                            },
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
