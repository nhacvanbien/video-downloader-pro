package com.smarttool.videodownloader.feature.pin.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import org.koin.androidx.compose.koinViewModel

/**
 * Security question: set as part of choosing a PIN, or answered to recover a
 * forgotten one. [isRecovery] picks which, and [pendingPin] carries the PIN that is
 * saved once the answer is stored.
 */
@Composable
fun SecurityRoute(
    isRecovery: Boolean,
    pendingPin: String,
    onRecovered: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: SecurityViewModel = koinViewModel()
    val context = LocalContext.current

    var showSuccessDialog by remember { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val toast = { messageRes: Int ->
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    SecurityScreen(
        questionIndex = state.questionIndex,
        answer = state.answer,
        onAnswerChange = viewModel::onAnswerChange,
        onPickQuestion = { viewModel.setQuestionPickerVisible(true) },
        onConfirm = {
            when (viewModel.confirm(isRecovery, pendingPin)) {
                SecurityResult.EmptyAnswer -> toast(R.string.string_please_enter_your_answer)

                SecurityResult.RecoveryIncorrect ->
                    toast(R.string.string_incorrect_security_question)

                SecurityResult.RecoveryCorrect -> {
                    toast(R.string.string_correct_security_question)
                    onRecovered()
                }

                SecurityResult.Saved -> showSuccessDialog = true
            }
        },
        onBack = onBack,
    )

    if (state.showQuestionPicker) {
        SecurityQuestionPickerDialog(
            selectedIndex = state.questionIndex,
            onSelect = viewModel::onQuestionSelected,
            onDismiss = { viewModel.setQuestionPickerVisible(false) },
        )
    }

    if (showSuccessDialog) {
        PinSetSuccessDialog(onDismiss = onBack)
    }
}
