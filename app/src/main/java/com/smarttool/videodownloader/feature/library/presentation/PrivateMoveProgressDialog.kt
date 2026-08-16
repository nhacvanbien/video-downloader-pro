package com.smarttool.videodownloader.feature.library.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogBody
import com.smarttool.videodownloader.core.ui.dialogs.DialogTitle
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.ShapePill

/**
 * Blocking progress for a private-area move. Moving copies each file, so this is the only
 * feedback the user gets that a large video is still being worked on — it deliberately has no
 * dismiss affordance, because cancelling midway would leave a half-copied file behind.
 */
@Composable
fun PrivateMoveProgressDialog(progress: PrivateMoveProgress) {
    AppDialogCard(onDismissRequest = {}) {
        DialogTitle(
            text = stringResource(
                if (progress.movingToPrivate) {
                    R.string.string_moving_to_private
                } else {
                    R.string.string_moving_out_of_private
                },
            ),
            modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
        )

        DialogBody(
            text = stringResource(R.string.string_do_not_close_app),
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
        )

        LinearProgressIndicator(
            progress = { progress.overallFraction },
            color = Pri,
            trackColor = Pri.copy(alpha = 0.2f),
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, start = 16.dp, end = 16.dp)
                .height(6.dp)
                .clip(ShapePill),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.string_moving_file_count,
                    (progress.completed + 1).coerceAtMost(progress.total).toString(),
                    progress.total.toString(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = "${(progress.overallFraction * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = Pri,
            )
        }
    }
}
