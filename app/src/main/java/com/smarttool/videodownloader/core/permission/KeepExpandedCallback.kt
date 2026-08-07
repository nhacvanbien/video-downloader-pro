package com.smarttool.videodownloader.core.permission

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Snaps a bottom sheet back to expanded whenever the user starts dragging it — the
 * permission sheet is a hard gate, not a peek that can be collapsed away.
 */
class KeepExpandedCallback(
    private val behavior: BottomSheetBehavior<*>,
) : BottomSheetBehavior.BottomSheetCallback() {

    override fun onStateChanged(bottomSheet: View, newState: Int) {
        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
}
