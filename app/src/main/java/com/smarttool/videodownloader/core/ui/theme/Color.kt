package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.ui.graphics.Color

// Pinboard design tokens (design_handoff_video_downloader)
val Bg = Color(0xFFFFFFFF)
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
