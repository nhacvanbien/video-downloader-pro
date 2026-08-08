package com.smarttool.videodownloader.feature.nativefull.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.BuildConfig
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.NativeAdFullContainer
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import kotlinx.coroutines.delay

private const val LOADING_OVERLAY_MILLIS = 2000L
private const val CLOSE_BUTTON_DELAY_MILLIS = 4000L

@Composable
fun NativeFullScreen(onClose: () -> Unit) {
    var showLoading by remember { mutableStateOf(true) }
    var showClose by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(LOADING_OVERLAY_MILLIS)
        showLoading = false
    }

    LaunchedEffect(Unit) {
        delay(CLOSE_BUTTON_DELAY_MILLIS)
        showClose = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NativeAdFullContainer(
            adUnitId = BuildConfig.NATIVE_FULL_ALL,
            layoutRes = R.layout.layout_native_on_boarding_full,
            adPlacement = "native_full_all",
            canShowAds = true,
            modifier = Modifier.fillMaxSize(),
        )

        if (showClose) {
            Icon(
                painter = painterResource(R.drawable.ic_close_circle_grey),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(20.dp)
                    .clickable(onClick = onClose),
            )
        }

        if (showLoading) {
            Column(
                modifier = Modifier.fillMaxSize().background(AppWhite),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    color = Primary,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(35.dp),
                )

                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = AppBlack,
                )
            }
        }
    }
}
