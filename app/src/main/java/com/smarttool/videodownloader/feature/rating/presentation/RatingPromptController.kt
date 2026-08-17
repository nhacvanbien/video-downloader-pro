package com.smarttool.videodownloader.feature.rating.presentation

import android.content.Context
import com.smarttool.videodownloader.feature.rating.domain.usecase.MarkRatingPromptShownUseCase
import com.smarttool.videodownloader.feature.rating.domain.usecase.ShouldPromptRatingAfterDownloadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single entry point for showing the rating flow — the boundary every call site (Settings
 * tap, post-download prompt) goes through instead of building [RatingDialog] itself.
 *
 * [coroutineScope] is caller-owned (an Activity's `lifecycleScope`, a Composable's
 * `rememberCoroutineScope`) rather than the dialog's own composition scope, so the
 * "mark as shown" write survives the dialog being dismissed right after completion.
 */
class RatingPromptController(
    private val shouldPromptAfterDownload: ShouldPromptRatingAfterDownloadUseCase,
    private val markPromptShown: MarkRatingPromptShownUseCase,
) {
    fun showRating(context: Context, coroutineScope: CoroutineScope, email: String?) {
        RatingDialog(
            context = context,
            email = email,
            onRatingCompleted = { coroutineScope.launch { markPromptShown() } },
        ).show()
    }

    /** No-op if the prompt was already completed, or the download count hasn't hit a threshold. */
    suspend fun maybeShowAfterSuccessfulDownload(
        context: Context,
        coroutineScope: CoroutineScope,
        email: String?,
    ) {
        if (shouldPromptAfterDownload()) {
            showRating(context, coroutineScope, email)
        }
    }
}
