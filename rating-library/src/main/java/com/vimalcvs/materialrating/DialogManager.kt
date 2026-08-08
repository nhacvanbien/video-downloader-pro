package com.vimalcvs.materialrating

import MaterialFeedback
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager

object DialogManager {

    fun showMaterialFeedback(context: Context?, rating: Float, email: String?) {
        val fragmentManager = getFragManager(context)
        val materialFeedback = MaterialFeedback(email)
        materialFeedback.setRating(rating)
        materialFeedback.show(fragmentManager, MaterialRating.KEY)
    }

    fun showFeedbackAppreciate(context: Context) {
        RatingCache.getInstance(context).setRatingShown()
        val fragmentManager = getFragManager(context)
        val materialFeedback = FeedbackAppreciateBottomSheetFragment()
        materialFeedback.show(fragmentManager, FeedbackAppreciateBottomSheetFragment.KEY)
    }

    /**
     * Prompts after the user has felt the app deliver value, not on every launch.
     * First ask comes on the [FIRST_PROMPT_DOWNLOAD_COUNT]th successful download; if the
     * user dismisses it without rating, it waits another [PROMPT_COOLDOWN_DOWNLOADS]
     * successful downloads before asking again. Once a rating or feedback is actually
     * submitted, [RatingCache.isRatingShown] is set and this never fires again.
     */
    fun showRatingAfterSuccessfulDownload(context: Context, email: String?) {
        val cache = RatingCache.getInstance(context)
        if (cache.isRatingShown()) return

        val downloadCount = cache.incrementSuccessfulDownloadCount()
        val lastPrompted = cache.getLastPromptedDownloadCount()
        val nextPromptAt = if (lastPrompted == 0) {
            FIRST_PROMPT_DOWNLOAD_COUNT
        } else {
            lastPrompted + PROMPT_COOLDOWN_DOWNLOADS
        }
        if (downloadCount < nextPromptAt) return

        cache.setLastPromptedDownloadCount(downloadCount)
        showRating(context, email)
    }

    fun showRating(context: Context, email: String?) {
        val fragmentManager = getFragManager(context)
        val feedBackDialog = MaterialRating().apply {
            arguments = bundleOf(
                MaterialRating.KEY_EMAIL to email,
            )
        }
        feedBackDialog.show(fragmentManager, "rating")
    }

    private fun getFragManager(context: Context?): FragmentManager {
        val activity = context as AppCompatActivity
        return activity.getSupportFragmentManager()
    }

    private const val FIRST_PROMPT_DOWNLOAD_COUNT = 3
    private const val PROMPT_COOLDOWN_DOWNLOADS = 10
}
