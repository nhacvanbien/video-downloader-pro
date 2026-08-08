package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R

@Composable
fun InformationImageDialogContent(
    link: String,
    isShowDownload: Boolean,
    isShowOpenTab: Boolean,
    onClickOpenNewTab: (link: String) -> Unit,
    onClickShare: (link: String) -> Unit,
    onClickCopyLink: (link: String) -> Unit,
    onClickDownloadImage: (link: String) -> Unit,
    onClose: () -> Unit,
) {
    AppDialogCard(onDismissRequest = onClose) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.string_cancel),
                tint = Color(0xFF5B5D5F),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(24.dp)
                    .clickable(onClick = onClose)
                    .padding(4.dp),
            )
        }
        Image(
            painter = painterResource(R.drawable.ic_download_image),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(40.dp),
        )
        Text(
            text = link,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 8.dp, end = 8.dp),
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (isShowDownload) {
            DialogPrimaryButton(
                text = stringResource(R.string.string_download_image),
                onClick = { onClickDownloadImage(link) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, start = 32.dp, end = 32.dp),
            )
        }
        DialogLinkAction(
            text = stringResource(R.string.string_share),
            onClick = { onClickShare(link) },
        )
        if (isShowOpenTab) {
            DialogLinkAction(
                text = stringResource(R.string.string_open_in_new_tab),
                onClick = { onClickOpenNewTab(link) },
            )
            DialogLinkAction(
                text = stringResource(R.string.string_copy_link),
                onClick = { onClickCopyLink(link) },
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}

@Composable
private fun DialogLinkAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 8.dp, end = 8.dp)
            .clickable(onClick = onClick),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

class DialogInformationImage(
    context: Context,
    private val link: String,
    private val isShowDownload: Boolean,
    private val isShowOpenTab: Boolean,
    private val onClickOpenNewTab: (link: String) -> Unit,
    private val onClickShare: (link: String) -> Unit,
    private val onClickCopyLink: (link: String) -> Unit,
    private val onClickDownloadImage: (link: String) -> Unit,
) : Dialog(context) {
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            InformationImageDialogContent(
                link = link,
                isShowDownload = isShowDownload,
                isShowOpenTab = isShowOpenTab,
                onClickOpenNewTab = { url -> onClickOpenNewTab(url); dismiss() },
                onClickShare = { url -> onClickShare(url); dismiss() },
                onClickCopyLink = { url -> onClickCopyLink(url); dismiss() },
                onClickDownloadImage = { url -> onClickDownloadImage(url); dismiss() },
                onClose = { dismiss() },
            )
        }
    }
}
