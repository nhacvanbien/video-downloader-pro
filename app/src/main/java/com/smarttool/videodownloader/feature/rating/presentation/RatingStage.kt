package com.smarttool.videodownloader.feature.rating.presentation

/** The rating prompt's dialog walks through these in order, never backwards. */
sealed interface RatingStage {
    data object Rating : RatingStage
    data class Feedback(val rating: Int) : RatingStage
    data object Thanks : RatingStage
}
