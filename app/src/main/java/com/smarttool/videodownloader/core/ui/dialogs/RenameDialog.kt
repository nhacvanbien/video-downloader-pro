package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Muted

@Composable
fun RenameDialogContent(
    currentName: String,
    onSave: (name: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentName) }

    AppDialogCard(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
        ) {
            DialogTitle(
                text = stringResource(R.string.string_rename),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.string_cancel),
                tint = Muted,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(24.dp)
                    .clickable(onClick = onCancel)
                    .padding(4.dp),
            )
        }
        DialogTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.string_name),
            modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),
        )
        DialogPrimaryButton(
            text = stringResource(R.string.string_save),
            onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isNotEmpty()) {
                    onSave(name)
                } else {
                    Toast.makeText(context, R.string.string_invalid_data, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .padding(top = 14.dp),
        )
    }
}

class DialogRename(
    context: Context,
    private val nameCurrent: String,
    private val edit: (name: String) -> Unit,
) : Dialog(context) {
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            RenameDialogContent(
                currentName = nameCurrent,
                onSave = { newName -> dismiss(); edit(newName) },
                onCancel = { dismiss() },
            )
        }
    }
}
