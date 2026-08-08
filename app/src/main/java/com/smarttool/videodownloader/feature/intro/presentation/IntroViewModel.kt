package com.smarttool.videodownloader.feature.intro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.MarkOnboardingShownUseCase
import com.smarttool.videodownloader.feature.onboarding.domain.usecase.ShouldShowPermissionStepUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Finishing the pager is what marks onboarding as seen. */
class IntroViewModel(
    private val markOnboardingShown: MarkOnboardingShownUseCase,
    private val shouldShowPermissionStep: ShouldShowPermissionStepUseCase,
) : ViewModel() {

    /** Carries whether the permission step still has to be shown after the intro. */
    private val _finished = Channel<Boolean>(Channel.BUFFERED)
    val finished: Flow<Boolean> = _finished.receiveAsFlow()

    private var finishing = false

    fun finish() {
        // Guards a double tap on the last page from pushing two destinations.
        if (finishing) return
        finishing = true

        viewModelScope.launch {
            markOnboardingShown()
            _finished.send(shouldShowPermissionStep())
        }
    }
}
