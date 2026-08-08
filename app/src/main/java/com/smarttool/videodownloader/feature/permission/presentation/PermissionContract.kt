package com.smarttool.videodownloader.feature.permission.presentation

import com.smarttool.videodownloader.core.presentation.UiEffect
import com.smarttool.videodownloader.core.presentation.UiEvent
import com.smarttool.videodownloader.core.presentation.UiState

interface PermissionContract {
    data class State(
        val storageGranted: Boolean = false,
        val notificationGranted: Boolean = false,
        val showNotificationRow: Boolean = true,
    ) : UiState {
        /** The skip button becomes "Continue" only once everything asked for is granted. */
        val allGranted: Boolean get() = storageGranted && notificationGranted
    }

    sealed interface Event : UiEvent {
        data object RefreshGrants : Event
        data object Complete : Event
    }

    sealed interface Effect : UiEffect {
        data object Completed : Effect
    }
}
