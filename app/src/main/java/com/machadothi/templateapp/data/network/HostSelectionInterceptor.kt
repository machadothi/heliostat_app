package com.machadothi.templateapp.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Points Retrofit at whatever IP the heliostat currently has.
 *
 * Retrofit's base URL is fixed when the singleton is built, but the heliostat's
 * address is only learned at runtime -- over BLE during provisioning, or from
 * DataStore on later launches. This rewrites the host (and scheme/port) of every
 * request to [host], when one is set.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor() : Interceptor {

    /** "192.168.50.77" or "192.168.50.77:8080" (the laptop mock server). */
    @Volatile
    var host: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val target = host ?: return chain.proceed(chain.request())
        val name = target.substringBefore(':')
        val port = target.substringAfter(':', "80").toIntOrNull() ?: 80
        val url = chain.request().url.newBuilder().scheme("http").host(name).port(port).build()
        return chain.proceed(chain.request().newBuilder().url(url).build())
    }
}
