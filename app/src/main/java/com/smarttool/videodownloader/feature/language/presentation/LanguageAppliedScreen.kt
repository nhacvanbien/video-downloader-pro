package com.smarttool.videodownloader.feature.language.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.LottieView
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

@Composable
fun LanguageAppliedScreen(
    languageName: String,
    languageIconRes: Int,
) {
    Box(modifier = Modifier.fillMaxSize().background(ScreenGradient)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Text(
                text = stringResource(R.string.string_languages),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = AppWhite,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp),
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppWhite)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (languageIconRes != 0) {
                    Image(
                        painter = painterResource(languageIconRes),
                        contentDescription = null,
                        modifier = Modifier.width(44.dp).height(28.dp),
                    )
                }

                Text(
                    text = languageName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp),
                )

                Image(
                    painter = painterResource(R.drawable.ic_dot_selected),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp).padding(end = 5.dp),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LottieView(rawRes = R.raw.language, modifier = Modifier.size(77.dp))

            Text(
                text = stringResource(R.string.string_applied_language),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = TextPrimary,
            )
        }
    }
}
