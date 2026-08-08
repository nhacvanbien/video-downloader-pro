package com.smarttool.videodownloader.feature.splash.presentation

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary

/**
 * The splash chrome. [progressView] is the existing custom seekbar View the activity
 * still drives (it exposes the progress flow the hand-off to [MainActivity] awaits),
 * so it is hosted here rather than reimplemented.
 */
@Composable
fun SplashScreen(
    loadingText: String,
    progressView: View,
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenGradient)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app),
                contentDescription = null,
                modifier = Modifier.size(110.dp),
            )

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Primary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = loadingText,
                style = MaterialTheme.typography.bodyLarge,
                color = AppWhite,
            )

            AndroidView(
                factory = { progressView },
                modifier = Modifier.width(180.dp).height(20.dp).padding(bottom = 8.dp),
            )
        }
    }
}
