package com.smarttool.videodownloader.core.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Plain [Dialog] shown from non-Compose classes (Activity, WebTabViewHost,
 * ProcessingWebViewHost) needs its own ViewTree*Owner wiring — it isn't part of the host
 * Activity's setContent() tree, so Compose can't resolve them automatically.
 */
fun Dialog.setComposeContent(context: Context, content: @Composable () -> Unit) {
    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

    val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent(content)
    }

    (context as? LifecycleOwner)?.let { composeView.setViewTreeLifecycleOwner(it) }
    (context as? ViewModelStoreOwner)?.let { composeView.setViewTreeViewModelStoreOwner(it) }
    (context as? SavedStateRegistryOwner)?.let { composeView.setViewTreeSavedStateRegistryOwner(it) }

    setContentView(composeView)
}
