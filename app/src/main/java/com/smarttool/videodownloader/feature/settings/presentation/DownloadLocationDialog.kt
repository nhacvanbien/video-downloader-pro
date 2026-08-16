package com.smarttool.videodownloader.feature.settings.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogPrimaryButton
import com.smarttool.videodownloader.core.ui.dialogs.DialogTextField
import com.smarttool.videodownloader.core.ui.dialogs.DialogTitle
import com.smarttool.videodownloader.core.ui.theme.Muted

/** Prompts for the subfolder name new downloads are saved under, inside public Downloads. */
@Composable
fun DownloadLocationDialog(
    currentSubfolder: String,
    onSave: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentSubfolder) }

    AppDialogCard(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            DialogTitle(
                text = stringResource(R.string.string_download_location),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.string_cancel),
                tint = Muted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(24.dp)
                    .clickable(onClick = onDismiss)
                    .padding(4.dp),
            )
        }
        DialogTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.string_download_location_folder_hint),
            modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),
        )
        DialogPrimaryButton(
            text = stringResource(R.string.string_save),
            onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isNotEmpty()) {
                    onSave(trimmedName)
                } else {
                    Toast.makeText(context, R.string.string_invalid_data, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .padding(top = 14.dp),
        )
    }
}
