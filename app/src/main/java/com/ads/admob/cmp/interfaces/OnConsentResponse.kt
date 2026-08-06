package com.ads.admob.cmp.interfaces

interface OnConsentResponse {
    fun onResponse(errorMessage: String?) {}
    fun onPolicyRequired(isRequired: Boolean) {}
}
