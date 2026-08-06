package com.smarttool.videodownloader.feature.pin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppBlack
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.SearchFieldHint
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

/** The three fixed recovery questions, indexed 1..3 as persisted in preferences. */
fun securityQuestionRes(index: Int): Int = when (index) {
    2 -> R.string.string_what_is_your_favourite_pet_name
    3 -> R.string.string_what_was_your_favorite_food_as_a_child
    else -> R.string.string_what_is_your_lucky_number
}

@Composable
fun SecurityScreen(
    questionIndex: Int,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onPickQuestion: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(ScreenGradient)) {
        AppTopBar(
            title = stringResource(R.string.string_security_question),
            onBack = onBack,
            trailing = {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = AppWhite,
                    modifier = Modifier.size(28.dp).clickable(onClick = onConfirm).padding(4.dp),
                )
            },
        )

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppWhite)
                .clickable(onClick = onPickQuestion)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(securityQuestionRes(questionIndex)),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )

            Icon(
                painter = painterResource(R.drawable.ic_arrow_next),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp),
            )
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppWhite)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (answer.isEmpty()) {
                    Text(
                        text = stringResource(R.string.string_enter_your_answer),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = SearchFieldHint,
                    )
                }

                BasicTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Primary),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        color = AppBlack,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
