package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R

val SfProDisplay = FontFamily(
    Font(R.font.sf_pro_display_light, FontWeight.Light),
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semi_bold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
)

val AppTypography = Typography(
    titleLarge = Typography().titleLarge.copy(fontFamily = SfProDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleMedium = Typography().titleMedium.copy(fontFamily = SfProDisplay, fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = SfProDisplay, fontWeight = FontWeight.Normal),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = SfProDisplay, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = Typography().labelLarge.copy(fontFamily = SfProDisplay, fontWeight = FontWeight.Medium, fontSize = 14.sp),
)
