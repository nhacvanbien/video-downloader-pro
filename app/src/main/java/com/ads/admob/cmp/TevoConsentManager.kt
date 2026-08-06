package com.ads.admob.cmp

import android.content.Context
import com.ads.admob.cmp.interfaces.OnConsentResponse

class TevoConsentManager private constructor() {
    companion object {
        private val instance = TevoConsentManager()

        fun getInstance(context: Context): TevoConsentManager = instance
    }

    fun initReleaseConsent(onConsentResponse: OnConsentResponse) {
        onConsentResponse.onResponse(null)
    }
}
