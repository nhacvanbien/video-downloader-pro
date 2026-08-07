/*
 * Copyright (c) 2025. Tevo Global Limited
 *
 * This software and all accompanying documentation is the sole property of
 * Tevo Global Limited and is protected by copyright law and international treaties.
 *
 * Unauthorized copying, distribution, or reproduction of this software, or any
 * portion of it, is strictly prohibited. The software is licensed to you solely for
 * your personal use and may not be used for commercial purposes without
 * a separate license agreement.
 *
 * You may not modify, reverse engineer, decompile, or disassemble this software.
 * You are not permitted to remove or alter any copyright notices or proprietary
 * legends from the software.
 *
 * All rights not expressly granted herein are reserved by Tevo Global Limited.
 *
 * Contact information: hello@tevo.app
 */

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
