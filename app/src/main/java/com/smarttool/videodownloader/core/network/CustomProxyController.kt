package com.smarttool.videodownloader.core.network

import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.google.gson.Gson
import com.smarttool.videodownloader.core.datastore.AppPreferencesDataSource
import java.net.Authenticator
import java.net.PasswordAuthentication

/**
 * Sets up the process-wide proxy: system properties for plain sockets, plus a WebView
 * override. Constructed once at app startup — a non-suspend context — so it reads and
 * writes through [AppPreferencesDataSource]'s blocking accessors rather than owning a
 * coroutine scope of its own.
 */
class CustomProxyController(
    private val preferences: AppPreferencesDataSource,
) {

    init {
        if (isProxyOn()) {
            setCurrentProxy(getCurrentRunningProxy())
        }
    }

    fun getCurrentRunningProxy(): Proxy {
        return if (isProxyOn()) {
            Proxy.fromMap(Gson().fromJson(preferences.proxyJsonBlocking(), Map::class.java))
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
            preferences.setProxyOnBlocking(true)

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

        preferences.setProxyJsonBlocking(Gson().toJson(proxy.toMap()))
    }


    fun isProxyOn(): Boolean {
        return preferences.isProxyOnBlocking()
    }

}
