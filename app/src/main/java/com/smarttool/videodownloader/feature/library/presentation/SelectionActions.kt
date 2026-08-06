package com.smarttool.videodownloader.feature.library.presentation

/** Bulk operations offered while the list is in selection mode. */
data class SelectionActions(
    val onSelectAll: () -> Unit,
    val onDelete: () -> Unit,
    val onTogglePrivate: () -> Unit,
    val onCancel: () -> Unit,
    val privateActionIconRes: Int,
)
