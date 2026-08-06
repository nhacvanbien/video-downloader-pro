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

import android.R.attr.rating
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log.v
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class MaterialRating :
    BottomSheetDialogFragment() {
    private var theDialogView: View? = null
    var email: String? = null

    private lateinit var imageIcon: AppCompatImageView
    private lateinit var textTitle: TextView
    private lateinit var textSubtitle: TextView
    private lateinit var buttonRate: AppCompatTextView
    private lateinit var tvDes2: AppCompatTextView
    private lateinit var icRate111: AppCompatImageView
    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView



    private var rate = 1f
    private var isSelectRate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
        email = arguments?.getString(KEY_EMAIL)
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
        }
    }

    override fun getView(): View? = theDialogView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.lib_material_fragment_rating, container)

        imageIcon = view.findViewById<AppCompatImageView>(R.id.icon)
        textTitle = view.findViewById<TextView>(R.id.text_title)
        textSubtitle = view.findViewById<TextView>(R.id.text_subtitle)
        tvDes2 = view.findViewById<AppCompatTextView>(R.id.tvDes2)
        icRate111 = view.findViewById<AppCompatImageView>(R.id.icRate111)
        star1 = view.findViewById<ImageView>(R.id.star1)
        star2 = view.findViewById<ImageView>(R.id.star2)
        star3 = view.findViewById<ImageView>(R.id.star3)
        star4 = view.findViewById<ImageView>(R.id.star4)
        star5 = view.findViewById<ImageView>(R.id.star5)

        setupStars()
        buttonRate = view.findViewById<AppCompatTextView>(R.id.bt_ratingSend)
        buttonRate.setOnClickListener(
            View.OnClickListener { send: View? ->
                if (rate < 5) {
                    DialogManager.showMaterialFeedback(requireActivity(), rate, email)
                    dismiss()
                } else {
                    try {
                        // Open Play Store rating dialog
                        val packageName = requireActivity().packageName
                        val uri = Uri.parse("market://details?id=$packageName")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                        )
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // If Play Store app is not available, open browser instead
                        val uri = Uri.parse("http://play.google.com/store/apps/details?id=${requireActivity().packageName}")
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                    DialogManager.showFeedbackAppreciate(requireActivity())
                    dismiss()
                }
            },
        )
        theDialogView = view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set rounded top corners for the BottomSheet background
        val parent = view.getParent() as View?
        if (parent != null) {
            val shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCornerSize(24f)
                .setTopRightCornerSize(24f)
                .setBottomLeftCornerSize(0f)
                .setBottomRightCornerSize(0f)
                .build()
            val materialShapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
            materialShapeDrawable.setFillColor(ColorStateList.valueOf(-0x1))
            parent.setBackground(materialShapeDrawable)
        }
    }

    private fun setupStars() {
        val stars =
            listOf(star1, star2, star3, star4, star5)

        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                val rating = (index + 1).toFloat()
                updateStars(rating, stars)
                onRatingSelected(rating)
            }
        }
    }
    private fun updateStars(rating: Float, stars: List<ImageView>) {
        stars.forEachIndexed { index, star ->
            val drawable =
                if (index < rating) R.drawable.ic_start_enable else R.drawable.ic_start_disable
            star.setImageResource(drawable)
        }
    }
    private fun stateButton(rating: Float) {
        if(rating > 0 || isSelectRate) {
            buttonRate.isEnabled = true
            buttonRate.setBackgroundResource(R.drawable.bg_button_action)
        }
    }
    
    private fun onRatingSelected(rating: Float) {
        isSelectRate = true
        rate = rating
        goneViewSuggest()
        stateButton(rating)

        when (rating) {
            1f -> {
                imageIcon.setImageResource(R.drawable.ic_face_1)
                textTitle.text = getString(R.string.lib_material_sorry_message)
                textSubtitle.text = getString(R.string.lib_material_feedback_welcome)
            }
            2f -> {
                imageIcon.setImageResource(R.drawable.ic_face_2)
                textTitle.text = getString(R.string.lib_material_sorry_message)
                textSubtitle.text = getString(R.string.lib_material_feedback_welcome)
            }
            3f -> {
                imageIcon.setImageResource(R.drawable.ic_face_3)
                textTitle.text = getString(R.string.lib_material_sorry_message)
                textSubtitle.text = getString(R.string.lib_material_feedback_welcome)
            }
            4f -> {
                imageIcon.setImageResource(R.drawable.ic_face_4)
                textTitle.text = getString(R.string.lib_material_much_appreciated)
                textSubtitle.text = getString(R.string.lib_material_support_motivation)
            }
            5f -> {
                imageIcon.setImageResource(R.drawable.ic_face_5)
                textTitle.text = getString(R.string.lib_material_much_appreciated)
                textSubtitle.text = getString(R.string.lib_material_support_motivation)
            }
        }
    }
    private fun goneViewSuggest() {
        tvDes2.visibility = View.GONE
        icRate111.visibility = View.GONE
    }

    companion object {
        const val KEY: String = "fragment_rate"
        const val KEY_EMAIL: String = "arg_email"
    }
}
