package com.smarttool.videodownloader.feature.pin.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.AppWhite
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.TextPrimary

private val QUESTION_INDICES = listOf(1, 2, 3)

@Composable
fun SecurityQuestionPickerDialog(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AppWhite)
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.string_security_question),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            QUESTION_INDICES.forEach { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(securityQuestionRes(index)),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    Image(
                        painter = painterResource(
                            if (index == selectedIndex) {
                                R.drawable.ic_dot_selected
                            } else {
                                R.drawable.ic_dot_normal
                            },
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PinSetSuccessDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(AppWhite)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )

            Text(
                text = stringResource(R.string.string_your_pin_code_amp_security_question_has_been_set),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            Text(
                text = stringResource(R.string.string_ok),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = AppWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(120.dp))
                    .background(Primary)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
            )
        }
    }
}
