package com.smarttool.videodownloader.feature.library.presentation

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
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Error
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

/**
 * Per-item actions for a library row. [privateActionLabel] differs between the
 * normal library ("move to private") and the private area ("move out").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaActionSheet(
    fileName: String,
    privateActionLabel: Int,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = AppWhite,
    ) {
        // The sheet composes in its own window, whose AbstractComposeView re-provides
        // LocalContext from the Activity — discarding the override installed around the
        // main composition. Without this the sheet keeps the launch-time language.
        AppLocaleProvider {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )

                ActionRow(R.drawable.ic_edit, R.string.string_rename, onRename)
                ActionRow(R.drawable.ic_share, R.string.string_share, onShare)
                ActionRow(R.drawable.ic_security, privateActionLabel, onTogglePrivate)
                ActionRow(R.drawable.ic_delete, R.string.string_delete, onDelete, contentColor = Error)
            }
        }
    }
}

@Composable
private fun ActionRow(
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
    contentColor: Color = TextPrimary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.padding(start = 14.dp),
        )
    }
}
