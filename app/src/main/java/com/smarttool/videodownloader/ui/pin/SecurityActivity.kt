package com.smarttool.videodownloader.ui.pin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.pin.presentation.PinSetSuccessDialog
import com.smarttool.videodownloader.feature.pin.presentation.SecurityQuestionPickerDialog
import com.smarttool.videodownloader.feature.pin.presentation.SecurityResult
import com.smarttool.videodownloader.feature.pin.presentation.SecurityScreen
import com.smarttool.videodownloader.feature.pin.presentation.SecurityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SecurityActivity : BaseComposeActivity() {

    private val viewModel: SecurityViewModel by viewModel()

    private var showSuccessDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pendingPin = intent.getStringExtra(EXTRA_PASS).orEmpty()
        val isRecovery = intent.getStringExtra(EXTRA_STATUS) == STATUS_FORGOT

        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                SecurityScreen(
                    questionIndex = state.questionIndex,
                    answer = state.answer,
                    onAnswerChange = viewModel::onAnswerChange,
                    onPickQuestion = { viewModel.setQuestionPickerVisible(true) },
                    onConfirm = { onConfirm(isRecovery, pendingPin) },
                    onBack = { finish() },
                )

                if (state.showQuestionPicker) {
                    SecurityQuestionPickerDialog(
                        selectedIndex = state.questionIndex,
                        onSelect = viewModel::onQuestionSelected,
                        onDismiss = { viewModel.setQuestionPickerVisible(false) },
                    )
                }

                if (showSuccessDialog) {
                    PinSetSuccessDialog(onDismiss = { finish() })
                }
            }
        }
    }

    private fun onConfirm(isRecovery: Boolean, pendingPin: String) {
        when (viewModel.confirm(isRecovery, pendingPin)) {
            SecurityResult.EmptyAnswer -> toast(R.string.string_please_enter_your_answer)

            SecurityResult.RecoveryIncorrect -> toast(R.string.string_incorrect_security_question)

            SecurityResult.RecoveryCorrect -> {
                toast(R.string.string_correct_security_question)
                startActivity(
                    Intent(this, PinActivity::class.java).apply {
                        putExtra(EXTRA_STATUS, STATUS_SECURITY)
                    },
                )
                finish()
            }

            SecurityResult.Saved -> showSuccessDialog = true
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_PASS = "pass"
        const val EXTRA_STATUS = "status"
        const val STATUS_FORGOT = "forgot"
        const val STATUS_SECURITY = "security"
    }
}
