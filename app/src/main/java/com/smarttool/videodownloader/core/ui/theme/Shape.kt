package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Pinboard radii tokens: cards/active rows/sheet top corners, standard rows/inputs, pill CTAs.
val ShapeLg = RoundedCornerShape(18.dp)
val ShapeMd = RoundedCornerShape(14.dp)
val ShapePill = CircleShape
val SheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

val AppShapes = Shapes(
    extraSmall = ShapeMd,
    small = ShapeMd,
    medium = ShapeMd,
    large = ShapeLg,
    extraLarge = ShapeLg,
)
