package com.smarttool.videodownloader.core.network

import java.net.Authenticator
import java.net.PasswordAuthentication
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.smarttool.videodownloader.helper.PreferenceHelper


class CustomProxyController(
    private val sharedPrefHelper: PreferenceHelper,
) {

    init {
        if (isProxyOn()) {
            setCurrentProxy(getCurrentRunningProxy())
        }
    }

    fun getCurrentRunningProxy(): Proxy {
        return if (isProxyOn()) {
            sharedPrefHelper.getCurrentProxy()
        } else {
            Proxy.noProxy()
        }
    }


    fun getProxyCredentials(): Pair<String, String> {
        val currProx = getCurrentRunningProxy()
        return Pair(currProx.user, currProx.password)
    }

    fun setCurrentProxy(proxy: Proxy) {
        if (proxy == Proxy.noProxy()) {
            System.setProperty("http.proxyUser", "")
            System.setProperty("http.proxyPassword", "")
            System.setProperty("https.proxyUser", "")
            System.setProperty("https.proxyPassword", "")

            Authenticator.setDefault(object : Authenticator() {})

            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().clearProxyOverride({ }) {}
            }
        } else {
            sharedPrefHelper.setIsProxyOn(true)

            System.setProperty("http.proxyUser", proxy.user.trim())
            System.setProperty("http.proxyPassword", proxy.password.trim())
            System.setProperty("https.proxyUser", proxy.user.trim())
            System.setProperty("https.proxyPassword", proxy.password.trim())

            System.setProperty("http.proxyHost", proxy.host.trim())
            System.setProperty("http.proxyPort", proxy.port.trim())

            System.setProperty("https.proxyHost", proxy.host.trim())
            System.setProperty("https.proxyPort", proxy.port.trim())
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")

            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(proxy.user, proxy.password.toCharArray())
                }
            })

            val proxyConfig =
                ProxyConfig.Builder().addProxyRule("${proxy.host}:${proxy.port}").build()
            if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                ProxyController.getInstance().setProxyOverride(proxyConfig, { }) {}
            }
        }

        sharedPrefHelper.setCurrentProxy(proxy)
    }


    fun isProxyOn(): Boolean {
        return sharedPrefHelper.getIsProxyOn()
    }

}
