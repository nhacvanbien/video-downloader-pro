package com.smarttool.videodownloader.feature.rating.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.dialogs.AppDialogCard
import com.smarttool.videodownloader.core.ui.dialogs.DialogBody
import com.smarttool.videodownloader.core.ui.dialogs.DialogPrimaryButton
import com.smarttool.videodownloader.core.ui.dialogs.DialogSecondaryButton
import com.smarttool.videodownloader.core.ui.dialogs.DialogTitle
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.Pri
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text

private const val MAX_MEDIA_ATTACHMENTS = 5

@Composable
fun FeedbackDialogContent(
    rating: Int,
    email: String?,
    onDismissRequest: () -> Unit,
    onFeedbackSent: () -> Unit,
) {
    val context = LocalContext.current
    val reasons = feedbackReasons()
    val checked = remember { mutableStateListOf(false, false, false, false, false, false) }
    var description by remember { mutableStateOf("") }
    val mediaUris = remember { mutableStateListOf<Uri>() }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        val filtered = uris.filter { uri ->
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
        }
        mediaUris.addAll(filtered.take(MAX_MEDIA_ATTACHMENTS - mediaUris.size))
    }

    AppDialogCard(onDismissRequest = onDismissRequest) {
        DialogTitle(
            text = stringResource(R.string.string_rating_feedback_title),
            modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
        )
        DialogBody(
            text = stringResource(R.string.string_rating_feedback_subtitle),
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
        )
        Column(
            modifier = Modifier
                .heightIn(max = 340.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp, start = 10.dp, end = 10.dp),
        ) {
            reasons.forEachIndexed { index, reason ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checked[index] = !checked[index] },
                ) {
                    Checkbox(
                        checked = checked[index],
                        onCheckedChange = { checked[index] = it },
                        colors = CheckboxDefaults.colors(checkedColor = Pri),
                    )
                    Text(text = reason, color = Muted, style = MaterialTheme.typography.bodyMedium)
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text(stringResource(R.string.string_rating_description_hint), color = Muted) },
                minLines = 3,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Border,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = Surface,
                    focusedContainerColor = Surface,
                ),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add_image),
                    contentDescription = stringResource(R.string.string_rating_upload_title),
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = mediaUris.size < MAX_MEDIA_ATTACHMENTS) { pickMedia.launch("*/*") },
                )
                if (mediaUris.isEmpty()) {
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = stringResource(R.string.string_rating_upload_title),
                            color = Text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(R.string.string_rating_upload_subtitle),
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        items(mediaUris.toList()) { uri ->
                            FeedbackMediaThumbnail(uri = uri, onRemove = { mediaUris.remove(uri) })
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DialogSecondaryButton(
                text = stringResource(R.string.string_cancel),
                onClick = onDismissRequest,
                modifier = Modifier.weight(1f),
            )
            DialogPrimaryButton(
                text = stringResource(R.string.string_rating_send),
                onClick = {
                    sendFeedbackEmail(
                        context = context,
                        email = email,
                        rating = rating,
                        selectedReasons = reasons.filterIndexed { index, _ -> checked[index] },
                        description = description,
                        mediaUris = mediaUris,
                    )
                    onFeedbackSent()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun feedbackReasons(): List<String> = listOf(
    stringResource(R.string.string_rating_reason_1),
    stringResource(R.string.string_rating_reason_2),
    stringResource(R.string.string_rating_reason_3),
    stringResource(R.string.string_rating_reason_4),
    stringResource(R.string.string_rating_reason_5),
    stringResource(R.string.string_rating_reason_other),
)

private fun sendFeedbackEmail(
    context: Context,
    email: String?,
    rating: Int,
    selectedReasons: List<String>,
    description: String,
    mediaUris: List<Uri>,
) {
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
    var subject = "$appName - "
    val selectedReasonsText = selectedReasons.joinToString(" ")
    if (selectedReasonsText.isNotBlank()) {
        subject += selectedReasonsText
    }

    val ratingText = if (rating > 0) "Stars: $rating\n\n" else ""
    val deviceInfo = """
        Device Info:
        OS Version: ${System.getProperty("os.version")} (${Build.VERSION.INCREMENTAL})
        OS API Level: ${Build.VERSION.SDK_INT}
        Device: ${Build.DEVICE}
        Model (and Product): ${Build.MODEL} (${Build.PRODUCT})
    """.trimIndent()

    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, "$ratingText Feedback: $description\n\n$deviceInfo")
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(mediaUris))
    }

    val chooser = Intent.createChooser(shareIntent, context.getString(R.string.string_choose_one))
    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(chooser)
    }
}
