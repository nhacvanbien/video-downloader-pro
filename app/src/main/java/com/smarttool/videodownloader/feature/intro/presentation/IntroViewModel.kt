package com.smarttool.videodownloader.feature.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.MarkOnboardingShownUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.ShouldShowPermissionStepUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Finishing the pager is what marks onboarding as seen. */
class IntroViewModel(
    private val markOnboardingShown: MarkOnboardingShownUseCase,
    private val shouldShowPermissionStep: ShouldShowPermissionStepUseCase,
) : ViewModel() {

    // Pager state lives in the Route; this is here only for shape consistency across
    // features and is never updated.
    val uiState: StateFlow<IntroContract.State> = MutableStateFlow(IntroContract.State())

    /** Carries whether the permission step still has to be shown after the intro. */
    private val _effect = Channel<IntroContract.Effect>(Channel.BUFFERED)
    val effect: Flow<IntroContract.Effect> = _effect.receiveAsFlow()

    private var finishing = false

    fun onEvent(event: IntroContract.Event) {
        when (event) {
            is IntroContract.Event.Finish -> finish()
        }
    }

    private fun finish() {
        // Guards a double tap on the last page from pushing two destinations.
        if (finishing) return
        finishing = true

        viewModelScope.launch {
            markOnboardingShown()
            _effect.send(IntroContract.Effect.Finished(shouldShowPermissionStep()))
        }
    }
}
