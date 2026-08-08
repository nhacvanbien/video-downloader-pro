package com.smarttool.videodownloader.core.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.smarttool.videodownloader.core.ui.theme.AppTheme
import com.smarttool.videodownloader.core.ui.theme.AppWhite

private val TextFieldBorder = Color(0xFFE3E5E8)
private val TextFieldHint = Color(0xFFBFBFBF)

private val CardBorder = Color(0x80868585)
val DialogSecondaryText = Color(0xFF8C9CB3)
val DialogBodyText = Color(0xFF808080)
private val SecondaryButtonBackground = Color(0xFFF2F2F2)

/**
 * Full-screen scrim + centered rounded card, matching the old `dialog_*.xml` shape
 * (`bg_dialog_rate`: white, 20dp corners, 1dp translucent border, 16dp outer margin).
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
        AppTheme {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppWhite)
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp)),
                content = content,
            )
        }
    }
}

private val DialogTitleColor = Color(0xFF404040)

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
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        color = AppWhite,
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
            .clip(RoundedCornerShape(12.dp))
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
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = TextFieldBorder,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedContainerColor = AppWhite,
            focusedContainerColor = AppWhite,
        ),
    )
}
