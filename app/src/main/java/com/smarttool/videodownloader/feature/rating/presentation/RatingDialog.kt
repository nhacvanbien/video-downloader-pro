package com.smarttool.videodownloader.feature.rating.presentation

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.smarttool.videodownloader.core.ui.dialogs.setComposeContent

class RatingDialog(
    context: Context,
    private val email: String?,
    private val onRatingCompleted: () -> Unit,
) : Dialog(context) {
    private val hostContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setComposeContent(hostContext) {
            RatingFlowContent(
                email = email,
                onRatingCompleted = onRatingCompleted,
                onDismissRequest = { dismiss() },
            )
        }
    }
}
