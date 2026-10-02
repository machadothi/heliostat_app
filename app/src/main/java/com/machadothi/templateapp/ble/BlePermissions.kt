package com.machadothi.templateapp.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * Runtime permissions, which differ across the app's API 24..35 range.
 *
 * Location is requested on EVERY version, not just the old ones that needed it
 * for BLE scanning: reading the SSID of the network the phone is on requires
 * ACCESS_FINE_LOCATION everywhere, and so does knowing where the heliostat is.
 */
object BlePermissions {

    val required: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }

    fun missing(context: Context): List<String> = required.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    /**
     * Permission alone is not enough: with the system Location toggle OFF, BLE
     * scans on API <= 30 silently return nothing, and on every version the SSID
     * reads as "<unknown ssid>". It gets its own UI state.
     */
    fun locationServicesOn(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }
}
