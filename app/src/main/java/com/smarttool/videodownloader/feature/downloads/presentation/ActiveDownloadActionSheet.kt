package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
 * Overflow menu for a running download. The row itself only has space for one action button, so
 * cancelling lives here rather than as a second button beside pause/resume.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDownloadActionSheet(
    isPaused: Boolean,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        AppLocaleProvider {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                ActionRow(
                    iconRes = if (isPaused) R.drawable.ic_play_glyph else R.drawable.ic_pause_glyph,
                    labelRes = if (isPaused) {
                        R.string.string_resume_action
                    } else {
                        R.string.string_pause_action
                    },
                    tint = TextColor,
                    onClick = onPauseResume,
                )

                ActionRow(
                    iconRes = R.drawable.ic_delete,
                    labelRes = R.string.string_cancel_download,
                    tint = Error,
                    onClick = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(iconRes: Int, labelRes: Int, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes == R.drawable.ic_delete) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = TextColor,
                modifier = Modifier.size(20.dp),
            )
        }

        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
