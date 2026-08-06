package com.smarttool.videodownloader.base

import android.content.Context
import androidx.fragment.app.DialogFragment

abstract class BaseDialogFragment : DialogFragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }
}