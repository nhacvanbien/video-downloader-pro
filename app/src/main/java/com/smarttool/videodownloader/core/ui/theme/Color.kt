package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.ui.graphics.Color

// Pinboard design tokens (design_handoff_video_downloader)

/**
 * Page background — deliberately a shade off [Surface], not the same white.
 *
 * Cards separate from the page by tonal contrast, the way the reference designs do it; a drop
 * shadow is then only needed by something that genuinely floats *over* scrolling content (the
 * FAB, the bottom bar). When both were `#FFFFFF` every card had to carry a shadow just to be
 * visible at all, which reads as heavy no matter how it is tuned.
 *
 * Warm rather than neutral grey, to sit with [Text], [Muted] and [Border], which are all warm.
 */
val Bg = Color(0xFFFAF8F7)
val Surface = Color(0xFFFFFFFF)
val Text = Color(0xFF281C1A)
val Muted = Color(0xFF7D6D6B)
val Pri = Color(0xFFE60023)
val PriInk = Color(0xFFFFFFFF)
val PriSoft = Color(0xFFFFE5E3)
val Border = Color(0xFFE3DCDA)
val Warn = Pri
val WarnSoft = Color(0xFFFFEEC5)
val WarnInk = Color(0xFF824103)
val Success = Color(0xFF249057)
val SuccessTint = Color(0xFFEAF3EA)
val Error = Pri

/**
 * Opaque on purpose. A translucent tint over an elevated card lets the card's own drop shadow
 * show through the fill — densest along the edges — which paints a darker frame around a lighter
 * middle instead of one flat wash.
 */
val ErrorSoft = Color(0xFFFBDDE0)

// In-progress / informational accent (download progress rows, storage meters).
val Info = Color(0xFF2F80ED)
val InfoSoft = Color(0xFFE3EEFD)

// Settings-screen icon accent tokens (colored circle backgrounds + icon tints).
val SettingsPurpleSoft = Color(0xFFECE9FE)
val SettingsPurple = Color(0xFF7B6EF6)
val SettingsBlueSoft = Color(0xFFE1EEFF)
val SettingsBlue = Color(0xFF2E86F5)
val SettingsOrangeSoft = Color(0xFFFFEDDA)
val SettingsOrange = Color(0xFFF5A623)
val SettingsGoldSoft = Color(0xFFFFF3D6)
val SettingsGold = Color(0xFFEAA400)
val SettingsGraySoft = Color(0xFFEDEDED)
val SettingsGrayIcon = Color(0xFF8E8E93)

// Legacy names kept for existing call sites, values updated to Pinboard tokens.
val Primary = Pri
val Secondary = Muted
val Tertiary = Muted
val TextPrimary = Text
val TextSub = Muted
val AppWhite = Surface
val AppBlack = Color(0xFF000000)
val AppRed = Pri
val AppGray = Border
val SearchFieldHint = Muted
