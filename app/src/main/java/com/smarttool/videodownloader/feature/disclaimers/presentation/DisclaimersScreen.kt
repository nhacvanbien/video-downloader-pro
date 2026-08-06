package com.smarttool.videodownloader.feature.disclaimers.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary

private val BodyTextColor = Color(0xFF404040)

@Composable
fun DisclaimersScreen(onAgree: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppWhite)) {
        AppTopBar(
            title = stringResource(R.string.string_disclaimers),
            titleColor = Primary,
        )

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
            color = AppWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(120.dp))
                .background(Primary)
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
        color = BodyTextColor,
        modifier = Modifier.padding(top = 14.dp),
    )

    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
        color = BodyTextColor,
        modifier = Modifier.padding(top = 4.dp),
    )
}
