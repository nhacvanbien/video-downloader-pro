package com.smarttool.videodownloader.dialog

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatRatingBar
import com.smarttool.videodownloader.android.R


class RatingDialog(private val context: Context) : Dialog(
    context, R.style.CustomAlertDialogRate
) {
    private var onPress: OnPress? = null
    private val imgIcon: ImageView
    private val rtb: AppCompatRatingBar
    private val btnRate: Button
    private val btnLater: Button

    init {
        setContentView(R.layout.dialog_rating_app)
        val attributes = window!!.attributes
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT

        window!!.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        window!!.attributes = attributes
        window!!.setSoftInputMode(16)
        rtb = findViewById<View>(R.id.rtb) as AppCompatRatingBar
        imgIcon = findViewById<View>(R.id.img_icon) as ImageView

        btnRate = findViewById<View>(R.id.btn_rate) as Button
        btnLater = findViewById<View>(R.id.btn_exit) as Button

        findViewById<AppCompatImageView>(R.id.button_close).setOnClickListener {
            dismiss()
        }


        onclick()
        changeRating()

        setCancelable(false)

        btnRate.text = context.resources.getString(R.string.string_rate)

        imgIcon.setImageResource(R.drawable.ic_rate_5)

        changeRating()

        rtb.rating == 5f

    }

    interface OnPress {
        fun sendThank()
        fun rating()
        fun later()
    }

    fun init(context: Context?, onPress: OnPress?) {
        this.onPress = onPress
    }

    private fun changeRating() {

        rtb.onRatingBarChangeListener =
            RatingBar.OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                val getRating = rtb.rating.toString()
                when (getRating) {
                    "1.0" -> {

                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_1)
                    }

                    "2.0" -> {

                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_2)
                    }

                    "3.0" -> {

                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_3)
                    }

                    "4.0" -> {
                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_4)
                    }

                    "5.0" -> {
                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_5)
                    }

                    else -> {
                        btnRate.text = context.resources.getString(R.string.string_rate)
                        imgIcon.setImageResource(R.drawable.ic_rate_0)
                    }
                }
            }
    }

    private fun onclick() {
        btnRate.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                if (rtb.rating == 0f) {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.string_please_feedback),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (rtb.rating <= 4.0) {
                    onPress!!.sendThank()
                } else {
                    onPress!!.rating()
                }
            }
        })
        btnLater.setOnClickListener { onPress!!.later() }
    }
}