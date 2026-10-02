package com.machadothi.templateapp.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** The WiFi network the phone is on right now. */
data class PhoneNetwork(val ssid: String, val frequencyMhz: Int) {
    val is24GHz: Boolean get() = WifiBands.is24GHz(frequencyMhz)
}

/**
 * Reads the SSID and band of the phone's current WiFi network.
 *
 * Needs ACCESS_FINE_LOCATION *and* Location Services switched on; without either,
 * Android reports the SSID as "<unknown ssid>", which is returned here as null.
 * There is no API for the network's password -- the user types it.
 */
@Singleton
class CurrentNetwork @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(): PhoneNetwork? {
        val info = wifiInfo() ?: return null
        val ssid = info.ssid?.removeSurrounding("\"")
        if (ssid.isNullOrBlank() || ssid == WifiManager.UNKNOWN_SSID.removeSurrounding("\"") ||
            ssid == "<unknown ssid>"
        ) {
            return null
        }
        return PhoneNetwork(ssid, info.frequency)
    }

    private suspend fun wifiInfo(): WifiInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            return wifi.connectionInfo
        }
        // API 31+: WifiManager.connectionInfo no longer carries the SSID. It comes
        // from a network callback registered with FLAG_INCLUDE_LOCATION_INFO.
        val connectivity =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ConnectivityManager.NetworkCallback(
                    FLAG_INCLUDE_LOCATION_INFO,
                ) {
                    override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                        val info = caps.transportInfo as? WifiInfo ?: return
                        if (continuation.isActive) continuation.resume(info)
                        runCatching { connectivity.unregisterNetworkCallback(this) }
                    }
                }
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                connectivity.registerNetworkCallback(request, callback)
                continuation.invokeOnCancellation {
                    runCatching { connectivity.unregisterNetworkCallback(callback) }
                }
            }
        }
    }
}
