package com.smarttool.videodownloader.feature.pin.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.components.AppTopBar
import com.smarttool.videodownloader.core.ui.components.ScreenGradient
import com.smarttool.videodownloader.core.ui.theme.AppRed
import com.smarttool.videodownloader.core.ui.theme.Primary
import com.smarttool.videodownloader.core.ui.theme.TextPrimary
import com.smarttool.videodownloader.core.ui.theme.TextSub

private val KeypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", BACKSPACE_KEY),
)

internal const val BACKSPACE_KEY = "⌫"

@Composable
fun PinScreen(
    state: PinUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
    onBack: () -> Unit,
) {
    val titleRes = when (state.step) {
        PinStep.Verify -> R.string.string_enter_your_pin
        PinStep.Create -> R.string.string_enter_your_new_pin
        PinStep.Confirm -> R.string.string_re_enter_your_pin
    }

    val descriptionRes = when (state.step) {
        PinStep.Verify -> R.string.string_enter_your_pin_to_confirm
        PinStep.Create -> R.string.string_des_pin
        PinStep.Confirm -> R.string.string_re_enter_your_pin_to_confirm
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenGradient)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppTopBar(title = stringResource(R.string.string_set_pin), onBack = onBack)

        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Text(
            text = stringResource(descriptionRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
            color = TextSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(PIN_LENGTH) { index ->
                Image(
                    painter = painterResource(
                        if (index < state.entered.length) {
                            R.drawable.ic_circle_selected
                        } else {
                            R.drawable.ic_circle_unselected
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 10.dp).size(18.dp),
                )
            }
        }

        if (state.showIncorrect) {
            Text(
                text = stringResource(R.string.string_incorrect_password_please_try_again),
                style = MaterialTheme.typography.bodyMedium,
                color = AppRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (state.allowForgotPin) {
            Text(
                text = stringResource(R.string.string_forgot_password),
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onForgotPin)
                    .padding(vertical = 8.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            KeypadRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(8.dp)
                                .then(
                                    when (key) {
                                        "" -> Modifier
                                        BACKSPACE_KEY -> Modifier.clickable(onClick = onBackspace)
                                        else -> Modifier.clickable { onDigit(key) }
                                    },
                                )
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                "" -> Unit

                                BACKSPACE_KEY -> Image(
                                    painter = painterResource(
                                        if (state.entered.isEmpty()) {
                                            R.drawable.ic_remove_disable
                                        } else {
                                            R.drawable.ic_remove
                                        },
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                )

                                else -> Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = TextPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
