package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R

private val DeleteButtonColor = Color(0xFFFF6666)

@Composable
fun ConfirmDeleteDialogContent(onDelete: () -> Unit, onCancel: () -> Unit) {
    AppDialogCard(onDismissRequest = onCancel) {
        DialogTitle(
            text = stringResource(R.string.string_confirm_delete),
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
        )
        DialogBody(
            text = stringResource(R.string.string_are_you_sure_want_to_delete_photo),
            modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp),
        )
        Image(
            painter = painterResource(R.drawable.img_confirm_delete),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .size(120.dp),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogSecondaryButton(
                text = stringResource(R.string.string_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            DialogPrimaryButton(
                text = stringResource(R.string.string_delete),
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                backgroundColor = DeleteButtonColor,
            )
        }
    }
}

class DialogConfirmDelete(context: Context, private val delete: () -> Unit) : Dialog(context) {
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            ConfirmDeleteDialogContent(
                onDelete = { dismiss(); delete() },
                onCancel = { dismiss() },
            )
        }
    }
}
