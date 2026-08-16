package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smarttool.videodownloader.android.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semi_bold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extra_bold, FontWeight.ExtraBold),
)

// Kept for any call site still referencing the pre-reskin font name.
val SfProDisplay = PlusJakartaSans

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    displayMedium = Typography().displayMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    displaySmall = Typography().displaySmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp),
    titleMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    titleSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    bodyLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
    bodyMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp),
    bodySmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = PlusJakartaSans, fontWeight = FontWeight.Medium, fontSize = 10.5.sp),
)

// Numeric readouts (%, speed, file size, timers) — tabular figures, bold weight per spec.
val NumericTextStyle = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)
