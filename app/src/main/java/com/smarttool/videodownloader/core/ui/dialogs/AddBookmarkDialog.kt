package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R

@Composable
fun AddBookmarkDialogContent(
    initialName: String = "",
    initialUrl: String = "",
    onAdd: (name: String, url: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }

    AppDialogCard(onDismissRequest = onCancel) {
        DialogTitle(
            text = stringResource(R.string.string_add_bookmark),
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
        )
        DialogTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.string_name),
            modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),
        )
        DialogTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = stringResource(R.string.string_url),
            modifier = Modifier.padding(top = 10.dp, start = 10.dp, end = 10.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogSecondaryButton(
                text = stringResource(R.string.string_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            DialogPrimaryButton(
                text = stringResource(R.string.string_add),
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedUrl = url.trim()
                    if (trimmedName.isNotEmpty() && trimmedUrl.isNotEmpty() &&
                        Patterns.WEB_URL.matcher(trimmedUrl).matches()
                    ) {
                        onAdd(name, url)
                    } else {
                        Toast.makeText(context, R.string.string_invalid_data, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

class DialogAddBookmark(
    context: Context,
    private val name: String = "",
    private val url: String = "",
    private val add: (name: String, url: String) -> Unit,
) : Dialog(context) {
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            AddBookmarkDialogContent(
                initialName = name,
                initialUrl = url,
                onAdd = { newName, newUrl -> dismiss(); add(newName, newUrl) },
                onCancel = { dismiss() },
            )
        }
    }
}
