package com.ads.admob.cmp

import android.content.Context
import com.ads.admob.cmp.interfaces.OnConsentResponse

class ConsentManager private constructor() {
    companion object {
        private val instance = ConsentManager()

        fun getInstance(context: Context): ConsentManager = instance
    }

    fun initReleaseConsent(onConsentResponse: OnConsentResponse) {
        onConsentResponse.onResponse(null)
    }
}
