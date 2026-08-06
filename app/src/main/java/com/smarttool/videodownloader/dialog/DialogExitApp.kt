package com.smarttool.videodownloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.smarttool.videodownloader.android.databinding.DialogExitAppBinding


class DialogExitApp(private var mContext: Context, var exit: () -> Unit) : Dialog(mContext) {
    private lateinit var binding: DialogExitAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogExitAppBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        window?.apply {
            val windowParams = attributes
            setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            windowParams.dimAmount = 0.8f
            attributes = windowParams
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        initViews()

        initListener()
    }


    private fun initViews() {
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun initListener() {
        binding.tvYes.setOnClickListener {
            dismiss()
            exit()
        }

        binding.tvNo.setOnClickListener {
            dismiss()
        }
    }

}