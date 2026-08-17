package com.smarttool.videodownloader.core.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smarttool.videodownloader.core.ui.theme.AppLocaleProvider
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.ui.theme.Border
import com.smarttool.videodownloader.core.ui.theme.Muted
import com.smarttool.videodownloader.core.ui.theme.PriInk
import com.smarttool.videodownloader.core.ui.theme.ShapeLg
import com.smarttool.videodownloader.core.ui.theme.ShapeMd
import com.smarttool.videodownloader.core.ui.theme.ShapePill
import com.smarttool.videodownloader.core.ui.theme.Surface
import com.smarttool.videodownloader.core.ui.theme.Text

private val TextFieldBorder = Border
private val TextFieldHint = Muted

private val CardBorder = Border
val DialogSecondaryText = Muted
val DialogBodyText = Muted
private val SecondaryButtonBackground = Border.copy(alpha = 0.35f)

/**
 * Full-screen scrim + centered rounded card — flat Pinboard sheet: white, 18dp
 * corners, hairline border, 16dp outer margin.
 */
@Composable
fun AppDialogCard(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Dialog content composes in its own window, whose AbstractComposeView re-provides
        // LocalContext from the Activity — discarding the override installed around the main
        // composition. Without re-applying it here the card keeps the launch-time language.
        AppLocaleProvider {
            AppTheme {
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(ShapeLg)
                        .background(Surface)
                        .border(1.dp, CardBorder, ShapeLg),
                    content = content,
                )
            }
        }
    }
}

private val DialogTitleColor = Text

@Composable
fun DialogTitle(text: String, modifier: Modifier = Modifier, color: Color = DialogTitleColor) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun DialogBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = DialogBodyText,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun DialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(ShapePill)
            .background(if (enabled) backgroundColor else SecondaryButtonBackground)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
        color = if (enabled) PriInk else DialogSecondaryText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun DialogSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .clip(ShapeMd)
            .background(SecondaryButtonBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        color = DialogSecondaryText,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextFieldHint) },
        singleLine = true,
        shape = ShapeMd,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TextFieldBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = Surface,
            focusedContainerColor = Surface,
        ),
    )
}
