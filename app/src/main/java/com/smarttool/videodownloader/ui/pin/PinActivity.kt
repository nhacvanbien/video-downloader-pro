package com.smarttool.videodownloader.ui.pin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.base.BaseComposeActivity
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.feature.pin.presentation.PinEvent
import com.smarttool.videodownloader.feature.pin.presentation.PinScreen
import com.smarttool.videodownloader.feature.pin.presentation.PinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.smarttool.videodownloader.ui.privateVideo.PrivateVideoActivity

class PinActivity : BaseComposeActivity() {

    private val viewModel: PinViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isChangingPin = intent.getStringExtra(EXTRA_ACTION) == ACTION_CHANGE_PIN
        viewModel.start(changingPin = isChangingPin)

        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event -> handleEvent(event) }
                }

                PinScreen(
                    state = state,
                    onDigit = viewModel::append,
                    onBackspace = viewModel::backspace,
                    onForgotPin = ::openRecovery,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }

    private fun handleEvent(event: PinEvent) {
        when (event) {
            PinEvent.Unlocked -> {
                startActivity(Intent(this, PrivateVideoActivity::class.java))
                finish()
            }

            is PinEvent.PinChosen -> {
                startActivity(
                    Intent(this, SecurityActivity::class.java).apply {
                        putExtra(SecurityActivity.EXTRA_PASS, event.pin)
                    },
                )
                finish()
            }

            PinEvent.PinChanged -> finish()

            PinEvent.ConfirmMismatch -> Toast.makeText(
                this,
                getString(R.string.string_password_does_not_match),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun openRecovery() {
        startActivity(
            Intent(this, SecurityActivity::class.java).apply {
                putExtra(SecurityActivity.EXTRA_STATUS, SecurityActivity.STATUS_FORGOT)
            },
        )
        finish()
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_CHANGE_PIN = "changePinCode"
    }
}
