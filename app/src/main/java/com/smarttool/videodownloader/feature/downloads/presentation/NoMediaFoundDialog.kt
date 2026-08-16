package com.smarttool.videodownloader.feature.downloads.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogPrimaryButton
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriSoft
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

/** P0: shown when the user asks to download but nothing was detected on the page. */
@Composable
fun NoMediaFoundDialog(onDismiss: () -> Unit, onReportIssue: () -> Unit) {
    AppDialogCard(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .padding(top = 22.dp)
                .align(Alignment.CenterHorizontally)
                .size(56.dp)
                .clip(ShapePill)
                .background(PriSoft),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_airboat),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        }

        Text(
            text = stringResource(R.string.string_no_media_found),
            style = MaterialTheme.typography.titleMedium,
            color = TextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp),
        )

        Text(
            text = stringResource(R.string.string_no_media_found_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 6.dp),
        )

        DialogPrimaryButton(
            text = stringResource(R.string.string_got_it),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 18.dp),
        )

        Text(
            text = stringResource(R.string.string_report_issue),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
            color = Pri,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onReportIssue)
                .padding(vertical = 14.dp),
        )
    }
}
