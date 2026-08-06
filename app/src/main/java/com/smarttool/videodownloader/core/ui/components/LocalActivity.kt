package com.smarttool.videodownloader.core.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

/**
 * Walks the context chain to find the hosting activity.
 *
 * A plain cast is not safe: a fragment's `LocalContext.current` can be a
 * `ContextWrapper` rather than the activity itself.
 */
fun Context.findComponentActivity(): ComponentActivity {
    var context = this

    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }

    error("No ComponentActivity found in context chain")
}
