package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R

@Composable
fun ExitAppDialogContent(onExit: () -> Unit, onStay: () -> Unit) {
    AppDialogCard(onDismissRequest = onStay) {
        DialogTitle(
            text = stringResource(R.string.exit_app),
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        DialogBody(
            text = stringResource(R.string.string_are_you_sure_you_want_to_exit),
            modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogSecondaryButton(
                text = stringResource(R.string.string_exit),
                onClick = onExit,
                modifier = Modifier.weight(1f),
            )
            DialogPrimaryButton(
                text = stringResource(R.string.string_stay),
                onClick = onStay,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

class DialogExitApp(context: Context, private val exit: () -> Unit) : Dialog(context) {
    // Dialog wraps its own `context` in a ContextThemeWrapper (not a LifecycleOwner);
    // the raw Activity context passed in must be captured before that happens.
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            ExitAppDialogContent(
                onExit = { dismiss(); exit() },
                onStay = { dismiss() },
            )
        }
    }
}
