package com.ads.admob.helper.appoppen

import android.app.Application
import androidx.lifecycle.LifecycleOwner

data class AppResumeAdConfig(
    val idAds: String,
    val listClassInValid: List<Class<*>>,
    val canShowAds: Boolean,
    val adPlacement: String,
)

class AppResumeAdHelper(
    private val application: Application,
    private val lifecycleOwner: LifecycleOwner,
    private val config: AppResumeAdConfig,
) {
    fun setEnableAppResumeOnScreen() {
    }

    fun setDisableAppResumeOnScreen() {
    }
}
