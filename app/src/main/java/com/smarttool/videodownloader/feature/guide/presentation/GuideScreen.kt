package com.smarttool.videodownloader.feature.guide.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.components.ScreenBg
import com.smarttool.videodownloader.core.ui.components.SheetCornerRadius
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Surface

private val StepTextColor = Muted

@Composable
fun GuideScreen(
    isHomeGuide: Boolean,
    onBack: () -> Unit,
) {
    val stepOneText = stringResource(
        if (isHomeGuide) R.string.string_step_1 else R.string.string_step_1_process,
    )
    val stepTwoText = stringResource(
        if (isHomeGuide) R.string.string_step_2 else R.string.string_step_2_process,
    )
    val stepOneImage = if (isHomeGuide) R.drawable.img_step_1 else R.drawable.img_step_1_process
    val stepTwoImage = if (isHomeGuide) R.drawable.img_step_2 else R.drawable.img_step_2_process

    Column(modifier = Modifier.fillMaxSize().background(ScreenBg).statusBarsPadding()) {
        AppTopBar(title = stringResource(R.string.string_guide), onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 5.dp)
                .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
                .background(Surface)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        ) {
            GuideStep(text = stepOneText, imageRes = stepOneImage)

            GuideStep(
                text = stepTwoText,
                imageRes = stepTwoImage,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun GuideStep(
    text: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = AnnotatedString.fromHtml(text),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            color = StepTextColor,
        )

        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(170.dp).padding(top = 8.dp),
        )
    }
}
