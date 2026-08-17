package com.smarttool.videodownloader.feature.rating.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val MAX_STARS = 5

/**
 * Walks [RatingStage.Rating] -> [RatingStage.Feedback] -> [RatingStage.Thanks], or
 * [RatingStage.Rating] straight to [RatingStage.Thanks] for a top rating. [onRatingCompleted]
 * fires exactly once, the moment the flow reaches the thank-you stage — matching the old
 * `RatingCache.setRatingShown()` call site, not the close button on that stage.
 */
@Composable
fun RatingFlowContent(
    email: String?,
    onRatingCompleted: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var stage by remember { mutableStateOf<RatingStage>(RatingStage.Rating) }
    val context = LocalContext.current

    when (val current = stage) {
        RatingStage.Rating -> RatingPromptDialogContent(
            onDismissRequest = onDismissRequest,
            onRatingSelected = { rating ->
                if (rating >= MAX_STARS) {
                    openPlayStoreListing(context)
                    onRatingCompleted()
                    stage = RatingStage.Thanks
                } else {
                    stage = RatingStage.Feedback(rating)
                }
            },
        )

        is RatingStage.Feedback -> FeedbackDialogContent(
            rating = current.rating,
            email = email,
            onDismissRequest = onDismissRequest,
            onFeedbackSent = {
                onRatingCompleted()
                stage = RatingStage.Thanks
            },
        )

        RatingStage.Thanks -> FeedbackThanksDialogContent(onClose = onDismissRequest)
    }
}
