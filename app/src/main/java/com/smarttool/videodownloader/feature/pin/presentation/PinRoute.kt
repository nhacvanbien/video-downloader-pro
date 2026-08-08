package com.smarttool.videodownloader.feature.pin.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import org.koin.androidx.compose.koinViewModel

/**
 * PIN entry. The same screen sets a new PIN, changes one, and unlocks the private
 * area; [isChangingPin] plus the emitted [PinContract.Effect] decide which.
 */
@Composable
fun PinRoute(
    isChangingPin: Boolean,
    onUnlocked: () -> Unit,
    onPinChosen: (pin: String) -> Unit,
    onOpenRecovery: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: PinViewModel = koinViewModel()
    val context = LocalContext.current

    LaunchedEffect(isChangingPin) {
        viewModel.onEvent(PinContract.Event.Start(changingPin = isChangingPin))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PinContract.Effect.Unlocked -> onUnlocked()

                is PinContract.Effect.PinChosen -> onPinChosen(effect.pin)

                PinContract.Effect.PinChanged -> onBack()

                PinContract.Effect.ConfirmMismatch -> Toast.makeText(
                    context,
                    context.getString(R.string.string_password_does_not_match),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PinScreen(
        state = state,
        onDigit = { viewModel.onEvent(PinContract.Event.Digit(it)) },
        onBackspace = { viewModel.onEvent(PinContract.Event.Backspace) },
        onForgotPin = onOpenRecovery,
        onBack = onBack,
    )
}
