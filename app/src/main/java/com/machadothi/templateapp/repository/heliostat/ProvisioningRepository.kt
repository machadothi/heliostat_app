package com.machadothi.templateapp.repository.heliostat

import com.machadothi.templateapp.ble.DiscoveredDevice
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.ble.WifiState
import kotlinx.coroutines.flow.Flow

/**
 * The BLE side: find a heliostat and hand it the credentials of the phone's own
 * WiFi network. See docs/protocol.md in the heliostat repo.
 */
interface ProvisioningRepository {

    val isBluetoothOn: Boolean

    /** Heliostats advertising nearby, strongest first. Scans while collected. */
    fun discover(): Flow<List<DiscoveredDevice>>

    suspend fun connect(address: String): Result<Unit>

    /** Networks the ESP32 itself can hear. It is 2.4 GHz only. */
    suspend fun scanWifi(): Result<List<VisibleNetwork>>

    /**
     * Hand over SSID + password and ask the board to join. Emits each state the
     * board reports, ending with JOINED (with its IP) or FAILED (with a reason).
     */
    fun join(ssid: String, password: String): Flow<WifiState>

    /** Send the phone's clock. Cheap; done on every connection. */
    suspend fun sendTime(): Result<Unit>

    suspend fun sendLocation(lat: Double, lon: Double, elevationM: Double): Result<Unit>

    fun disconnect()
}
