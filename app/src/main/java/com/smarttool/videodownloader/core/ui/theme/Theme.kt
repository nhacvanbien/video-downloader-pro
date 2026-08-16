package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = Pri,
    onPrimary = PriInk,
    primaryContainer = PriSoft,
    onPrimaryContainer = Pri,
    secondary = Muted,
    tertiary = Muted,
    background = Bg,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = PriSoft,
    onSurfaceVariant = Muted,
    outline = Border,
    outlineVariant = Border,
    error = Error,
    onError = PriInk,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
