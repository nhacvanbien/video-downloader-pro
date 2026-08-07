package com.smarttool.videodownloader.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope
import org.koin.viewmodel.factory.KoinViewModelFactory

/**
 * A private [ViewModelStore] that resolves Koin `viewModel { }` definitions.
 *
 * The browser and the processing screen both drive the same detection pipeline types
 * (`WebTabViewModel`, `DetectedVideosTabViewModel`, `VideoDetectionAlgVModel`) and each
 * assigns itself as that pipeline's owner. Back when they were separate Activity and
 * Fragment hosts they got separate stores for free; now that both live under one
 * Activity, sharing the Activity store would cross-wire them — the browser's detected
 * videos would land in the processing list. Each host owns a store instead.
 */
class ScopedViewModelStore : ViewModelStoreOwner, KoinComponent {

    override val viewModelStore: ViewModelStore = ViewModelStore()

    /** Exposed because [get] is inline; it is the root scope every module declares into. */
    @OptIn(KoinInternalApi::class)
    val koinScope: Scope = getKoin().scopeRegistry.rootScope

    inline fun <reified T : ViewModel> get(): T = ViewModelProvider(
        this,
        KoinViewModelFactory(T::class, koinScope),
    )[T::class.java]

    /** Runs `onCleared` on everything in the store; call it when the host goes away. */
    fun clear() {
        viewModelStore.clear()
    }
}
