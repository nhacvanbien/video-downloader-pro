package com.ads.admob.config

enum class NetworkProvider {
    ADMOB
}

data class EventConfig(
    val exchangeRate: Int,
    val exchangeCurrency: String,
)

class TevoAdjustConfig private constructor() {
    class Build(
        private val appToken: String,
        private val isProduction: Boolean,
    ) {
        fun build(): TevoAdjustConfig = TevoAdjustConfig()
    }
}

class TevoAdsConfig private constructor() {
    class Builder(private val mexaAdjustConfig: TevoAdjustConfig) {
        fun intervalBetweenInterstitial(value: Long): Builder = this
        fun buildVariantProduce(value: Boolean): Builder = this
        fun mediationProvider(value: NetworkProvider): Builder = this
        fun eventConfig(value: EventConfig): Builder = this
        fun listTestDevices(value: List<String>): Builder = this
        fun build(): TevoAdsConfig = TevoAdsConfig()
    }
}
