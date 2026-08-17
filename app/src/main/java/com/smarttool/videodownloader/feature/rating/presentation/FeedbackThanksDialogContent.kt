package com.smarttool.videodownloader.feature.rating.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogBody
import com.smarttool.videodownloader.core.ui.dialogs.DialogPrimaryButton
import com.smarttool.videodownloader.core.ui.dialogs.DialogTitle

@Composable
fun FeedbackThanksDialogContent(onClose: () -> Unit) {
    AppDialogCard(onDismissRequest = onClose) {
        Image(
            painter = painterResource(R.drawable.ic_face_feedback),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 20.dp)
                .size(96.dp),
        )
        DialogTitle(
            text = stringResource(R.string.string_rating_appreciate_title),
            modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
        )
        DialogBody(
            text = stringResource(R.string.string_rating_appreciate_subtitle),
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
        )
        DialogPrimaryButton(
            text = stringResource(R.string.string_rating_close),
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp, vertical = 20.dp),
        )
    }
}
