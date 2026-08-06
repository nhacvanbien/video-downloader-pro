package com.smarttool.videodownloader.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import com.smarttool.videodownloader.android.R
import com.smarttool.videodownloader.android.databinding.DialogRenameBinding


class DialogRename(
    context: Context,
    var nameCurrent: String,
    var edit: (name: String) -> Unit
) :
    Dialog(context) {
    private lateinit var binding: DialogRenameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogRenameBinding.inflate(LayoutInflater.from(context))
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

        binding.edtName.setText(this.nameCurrent)
    }

    private fun initListener() {
        binding.tvAdd.setOnClickListener {

            if (binding.edtName.text.toString().trim().isNotEmpty()
            ) {
                dismiss()
                edit(binding.edtName.text.toString())
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.string_invalid_data), Toast.LENGTH_SHORT
                ).show()
            }

        }

        binding.tvCancel.setOnClickListener {
            dismiss()
        }
    }

}