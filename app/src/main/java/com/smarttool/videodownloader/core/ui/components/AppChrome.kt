package com.smarttool.videodownloader.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.core.ui.theme.Bg
import com.smarttool.videodownloader.core.ui.theme.Text as TextColor

/** Flat Pinboard screen background — no gradient, no shadow. */
val ScreenBg = Bg

/** Rounded top corners for the bottom-sheet-style content card. */
val SheetCornerRadius = 18.dp

/**
 * Toolbar shared by the detail screens: centered title, optional back arrow and
 * trailing action.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleColor: Color = TextColor,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(60.dp).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(28.dp)) {
            if (onBack != null) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = null,
                    tint = titleColor,
                    modifier = Modifier.size(28.dp).clickable(onClick = onBack).padding(4.dp),
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = titleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).padding(horizontal = 5.dp),
        )

        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            trailing?.invoke()
        }
    }
}
