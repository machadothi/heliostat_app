package com.machadothi.templateapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredDevice(val address: String, val name: String, val rssi: Int)

/**
 * Finds heliostats nearby.
 *
 * The board puts its NAME in the advertisement and its service UUID in the scan
 * RESPONSE (the two do not fit together in 31 bytes). Hardware scan filters on
 * some phones only look at the advertisement, so filtering is done here in
 * software, on either the service UUID or the name.
 */
@Singleton
class BleScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val adapter
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    val isBluetoothOn: Boolean
        get() = adapter?.isEnabled == true

    /** Emits the list of heliostats seen so far, updated as results arrive. */
    @SuppressLint("MissingPermission") // checked by the permission gate before scanning
    fun scan(): Flow<List<DiscoveredDevice>> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            close(IllegalStateException("Bluetooth is off or unavailable"))
            return@callbackFlow
        }
        val seen = linkedMapOf<String, DiscoveredDevice>()
        val service = ParcelUuid(BleConstants.SERVICE)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                val name = record?.deviceName ?: result.device.name
                val advertisesService = record?.serviceUuids?.contains(service) == true
                if (!advertisesService && name != BleConstants.DEVICE_NAME) return
                seen[result.device.address] = DiscoveredDevice(
                    address = result.device.address,
                    name = name ?: BleConstants.DEVICE_NAME,
                    rssi = result.rssi,
                )
                trySend(seen.values.sortedByDescending { it.rssi })
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed (error $errorCode)"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // active scan: gets the scan response
            .build()
        scanner.startScan(null, settings, callback)
        trySend(emptyList())
        awaitClose { scanner.stopScan(callback) }
    }
}
