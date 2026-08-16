package com.smarttool.videodownloader.feature.permission.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.ScreenBg
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

@Composable
fun PermissionScreen(
    state: PermissionContract.State,
    onSkip: () -> Unit,
    onRequestStorage: () -> Unit,
    onRequestNotification: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.string_permission),
                style = MaterialTheme.typography.titleLarge,
                color = TextColor,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = stringResource(
                    if (state.allGranted) R.string.string_continue else R.string.string_skip,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = Pri,
                modifier = Modifier.clickable(onClick = onSkip).padding(8.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.img_permission),
                contentDescription = null,
                modifier = Modifier.padding(top = 16.dp).height(180.dp),
            )

            Text(
                text = stringResource(R.string.string_allow_per_app),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            PermissionRow(
                iconRes = R.drawable.ic_storage,
                label = stringResource(R.string.string_storage),
                granted = state.storageGranted,
                onRequest = onRequestStorage,
            )

            if (state.showNotificationRow) {
                PermissionRow(
                    iconRes = R.drawable.ic_notification,
                    label = stringResource(R.string.string_notification),
                    granted = state.notificationGranted,
                    onRequest = onRequestNotification,
                )
            }
        }

        if (state.storageGranted) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(ShapePill)
                    .background(Pri)
                    .clickable(onClick = onContinue)
                    .padding(vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.txt_continue),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = PriInk,
                )

                Image(
                    painter = painterResource(R.drawable.ic_arrow_next),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    iconRes: Int,
    label: String,
    granted: Boolean,
    onRequest: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(ShapeMd)
            .background(Surface)
            .border(1.dp, Border, ShapeMd)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )

        Image(
            painter = painterResource(
                if (granted) R.drawable.ic_switch_on else R.drawable.ic_switch_off,
            ),
            contentDescription = null,
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .then(if (granted) Modifier else Modifier.clickable(onClick = onRequest)),
        )
    }
}
