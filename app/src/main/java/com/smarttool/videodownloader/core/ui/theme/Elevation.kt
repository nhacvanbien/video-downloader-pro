package com.smarttool.videodownloader.core.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth comes from tonal contrast — [Surface] cards on the slightly darker [Bg] page — not from
 * shadows. A shadow is reserved for something that genuinely floats *over* scrolling content,
 * where there is no tonal step to rely on because the thing underneath keeps changing.
 *
 * So: in-flow cards get a whisper at most, and only the FAB and the pinned bars get a real one.
 */
val ElevationCard = 1.dp
val ElevationControl = 0.dp
val ElevationFab = 8.dp

/** Bars pinned over the scrolling list (bottom navigation, the selection action bar). */
val ElevationFloatingBar = 8.dp

/**
 * Neutral slate rather than black or a warm brown — a tinted shadow over the near-white page
 * reads as a smudge, and pure black at these radii reads as a hard edge. The alpha does the
 * lightening: the platform already scales shadow opacity by elevation, and this multiplies it
 * down further.
 *
 * Only honoured on API 28+; below that the platform always renders shadows black, which is why
 * the elevations above stay low enough to look right either way.
 */
private val ShadowTint = Color(0x4D101828)

fun Modifier.softShadow(elevation: Dp, shape: Shape): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = ShadowTint,
    spotColor = ShadowTint,
)
