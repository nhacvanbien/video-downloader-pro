package com.smarttool.videodownloader.feature.downloads.presentation

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

/**
 * Sheet for a row parked in
 * [com.smarttool.videodownloader.data.downloader.generic_downloader.models.VideoTaskState.WAITING_FOR_WIFI]
 * — "Download now" (start on the current connection anyway) or "Delete".
 *
 * The overflow button used to call cancel straight away, so tapping the three dots silently
 * dropped the queued item with no menu and no way back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingForWifiActionSheet(
    onDownloadNow: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        // Same reason as FailedDownloadActionSheet: the sheet composes in its own window and
        // would otherwise fall back to the launch-time language.
        AppLocaleProvider {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ActionRow(R.drawable.ic_download, R.string.string_download_now, TextColor, onDownloadNow)
                ActionRow(R.drawable.ic_delete, R.string.string_delete, Error, onDelete)
            }
        }
    }
}

@Composable
private fun ActionRow(
    iconRes: Int,
    labelRes: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
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
