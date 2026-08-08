package com.smarttool.videodownloader.feature.permission.presentation

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.core.permission.MediaPermissionChecker
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.CompletePermissionStepUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The onboarding permission step. Skipping is allowed — the grants are re-requested
 * from the download flow — so both buttons record the step as done and move on.
 */
class PermissionViewModel(
    private val checker: MediaPermissionChecker,
    private val completePermissionStep: CompletePermissionStepUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PermissionUiState(
            showNotificationRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        ),
    )
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private val _completed = Channel<Unit>(Channel.BUFFERED)
    val completed: Flow<Unit> = _completed.receiveAsFlow()

    /** Re-read after every launcher result and on resume, the only ways a grant changes. */
    fun refreshGrants() {
        _uiState.update {
            it.copy(
                storageGranted = checker.hasStorage(),
                notificationGranted = checker.hasNotification(),
            )
        }
    }

    fun complete() {
        viewModelScope.launch {
            completePermissionStep()
            _completed.send(Unit)
        }
    }
}
