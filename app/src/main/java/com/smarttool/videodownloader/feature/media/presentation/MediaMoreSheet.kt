package com.smarttool.videodownloader.feature.media.presentation

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

/**
 * The player's "..." overflow menu. Only offered for downloaded items ([MediaContract.State.showMoreAction]) —
 * the same Share/Rename/Delete actions [com.smarttool.videodownloader.feature.library.presentation.MediaActionSheet]
 * exposes from the library row, reachable here too so the button in the header does something.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaMoreSheet(
    fileName: String,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
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
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextColor,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )

                MoreActionRow(R.drawable.ic_edit, R.string.string_rename, TextColor, onRename)
                MoreActionRow(R.drawable.ic_share, R.string.string_share, TextColor, onShare)
                MoreActionRow(R.drawable.ic_delete, R.string.string_delete, Error, onDelete)
            }
        }
    }
}

@Composable
private fun MoreActionRow(iconRes: Int, labelRes: Int, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            colorFilter = ColorFilter.tint(tint),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
