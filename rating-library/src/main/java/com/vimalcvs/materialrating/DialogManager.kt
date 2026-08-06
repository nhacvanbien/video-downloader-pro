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
    private var ratingAppOpenedShown = false

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

    fun showRatingAfterDoFunction(context: Context, email: String?) {
        if (RatingCache.getInstance(context).isRatingShown()) {
            return
        }
        showRating(context, email)
    }

    fun showRatingAfterFirstTimePlayVideo(context: Context, email: String?) {
        if (RatingCache.getInstance(context).isRatingShown() ||
            RatingCache.getInstance(context).isPlayedVideoRatingShown()) {
            return
        }
        showRating(context, email)
        RatingCache.getInstance(context).setPlayedVideoRatingShown()
    }


    fun showRatingAfterAppOpened(context: Context, email: String?) {
        if (RatingCache.getInstance(context).isRatingShown() ||
            RatingCache.getInstance(context).isFirstTime() ||
            ratingAppOpenedShown
        ) {
            return
        }
        ratingAppOpenedShown = true
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
}
