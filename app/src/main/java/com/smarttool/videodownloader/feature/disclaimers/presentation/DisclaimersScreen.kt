package com.smarttool.videodownloader.feature.disclaimers.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

@Composable
fun DisclaimersScreen(onAgree: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppTopBar(title = stringResource(R.string.string_disclaimers))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            DisclaimerSection(R.string.string_legal_use, R.string.string_content_legal_use)
            DisclaimerSection(R.string.string_no_liability, R.string.string_content_no_liability)
            DisclaimerSection(R.string.string_data_security, R.string.string_content_data_security)
            DisclaimerSection(
                R.string.string_limitation_of_liability,
                R.string.string_content_limitation_of_liability,
            )
            DisclaimerSection(
                R.string.string_terms_of_service,
                R.string.string_content_terms_of_service,
            )
        }

        Text(
            text = stringResource(R.string.string_agree),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            color = PriInk,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(ShapePill)
                .background(Pri)
                .clickable(onClick = onAgree)
                .padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun DisclaimerSection(titleRes: Int, bodyRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = TextColor,
        modifier = Modifier.padding(top = 14.dp),
    )

    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
        color = Muted,
        modifier = Modifier.padding(top = 4.dp),
    )
}
