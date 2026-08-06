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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class FeedbackAppreciateBottomSheetFragment : BottomSheetDialogFragment() {
    private var theDialogView: View? = null

    private lateinit var imageIcon: AppCompatImageView
    private lateinit var textTitle: TextView
    private lateinit var textSubtitle: TextView
    private lateinit var buttonRate: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog)
    }

    override fun getView(): View? = theDialogView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.feedback_appreciate_bottom_sheet_fragment, container)

        imageIcon = view.findViewById<AppCompatImageView>(R.id.icon)
        textTitle = view.findViewById<TextView>(R.id.text_title)
        textSubtitle = view.findViewById<TextView>(R.id.text_subtitle)

        buttonRate = view.findViewById<AppCompatTextView>(R.id.bt_ratingSend)
        buttonRate.setOnClickListener(
            View.OnClickListener { send: View? ->
                dismiss()
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

    companion object {
        const val KEY: String = "FeedbackAppreciateBottomSheetFragment"
    }
}
