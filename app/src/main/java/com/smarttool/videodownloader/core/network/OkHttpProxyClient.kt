package com.smarttool.videodownloader.core.network

import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy as JavaNetProxy

class OkHttpProxyClient  constructor(
    private val okHttpClient: OkHttpClient?,
    private val proxyController: CustomProxyController
) {
    private var currentProxy: Proxy
    private var httpClientCached: OkHttpClient? = null

    init {
        currentProxy = getProxy()
    }

    fun getProxyOkHttpClient(): OkHttpClient {
        val proxy = getProxy()

        if (proxy.host != currentProxy.host && proxy.port != currentProxy.port || (httpClientCached == null)) {
            currentProxy = proxy
            val proxyCredentials = getProxyCredentials()
            val proxyAuthenticator = Authenticator { _, response ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", proxyCredentials)
                    .build()
            }
            httpClientCached =
                if (proxy == Proxy.noProxy()) {
                    okHttpClient?.newBuilder()!!.build()
                } else {
                    okHttpClient?.newBuilder()
                        ?.proxy(
                            JavaNetProxy(
                                JavaNetProxy.Type.HTTP,
                                InetSocketAddress(proxy.host, proxy.port.toIntOrNull() ?: 1)
                            )
                        )
                        ?.proxyAuthenticator(proxyAuthenticator)!!.build()
                }
        }

        return httpClientCached!!

    }

    private fun getProxy(): Proxy {
        return proxyController.getCurrentRunningProxy()
    }

    private fun getProxyCredentials(): String {
        val creds = proxyController.getProxyCredentials()
        return Credentials.basic(creds.first, creds.second)
    }
}