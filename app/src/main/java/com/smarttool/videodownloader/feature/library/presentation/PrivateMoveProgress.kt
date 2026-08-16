package com.smarttool.videodownloader.feature.library.presentation

/**
 * Progress of an in-flight "move to/out of private" batch. Present in state only while the
 * move is running — moving copies each file byte by byte, so a large video would otherwise
 * look like the app had frozen.
 *
 * @param completed files fully moved so far
 * @param total files in this batch
 * @param currentFileFraction 0f..1f of the file currently being copied
 */
data class PrivateMoveProgress(
    val movingToPrivate: Boolean,
    val completed: Int,
    val total: Int,
    val currentFileFraction: Float = 0f,
) {
    /** Counts the partially-copied file so the bar advances smoothly within a single item. */
    val overallFraction: Float
        get() = if (total <= 0) 0f else ((completed + currentFileFraction) / total).coerceIn(0f, 1f)
}
