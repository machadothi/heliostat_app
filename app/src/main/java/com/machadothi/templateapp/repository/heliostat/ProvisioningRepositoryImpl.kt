package com.machadothi.templateapp.repository.heliostat

import com.machadothi.templateapp.ble.BleCodec
import com.machadothi.templateapp.ble.BleConstants
import com.machadothi.templateapp.ble.BleGattClient
import com.machadothi.templateapp.ble.BleScanner
import com.machadothi.templateapp.ble.DiscoveredDevice
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.ble.WifiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withTimeout
import java.util.TimeZone
import javax.inject.Inject

class ProvisioningRepositoryImpl @Inject constructor(
    private val scanner: BleScanner,
    private val gatt: BleGattClient,
) : ProvisioningRepository {

    override val isBluetoothOn: Boolean
        get() = scanner.isBluetoothOn

    override fun discover(): Flow<List<DiscoveredDevice>> = scanner.scan()

    override suspend fun connect(address: String): Result<Unit> = runCatching {
        gatt.connect(address)
    }

    override suspend fun scanWifi(): Result<List<VisibleNetwork>> = runCatching {
        withTimeout(SCAN_TIMEOUT_MS) {
            statesAfter(BleConstants.CMD_SCAN).first { it.phase == WifiState.Phase.SCAN_READY }
        }
        BleCodec.parseScan(String(gatt.read(BleConstants.SCAN), Charsets.UTF_8))
    }

    override fun join(ssid: String, password: String): Flow<WifiState> = flow {
        for (chunk in BleCodec.frame(ssid, gatt.maxWrite)) gatt.write(BleConstants.SSID, chunk)
        for (chunk in BleCodec.frame(password, gatt.maxWrite)) gatt.write(BleConstants.PSK, chunk)
        withTimeout(JOIN_TIMEOUT_MS) {
            statesAfter(BleConstants.CMD_JOIN)
                .transformWhile { state ->
                    emit(state)
                    state.phase != WifiState.Phase.JOINED && state.phase != WifiState.Phase.FAILED
                }
                .collect { emit(it) }
        }
    }

    override suspend fun sendTime(): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val tzMinutes = TimeZone.getDefault().getOffset(now) / 60_000
        gatt.write(BleConstants.TIME, BleCodec.encodeTime(now / 1000, tzMinutes))
    }

    override suspend fun sendLocation(lat: Double, lon: Double, elevationM: Double): Result<Unit> =
        runCatching {
            gatt.write(BleConstants.LOCATION, BleCodec.encodeLocation(lat, lon, elevationM))
        }

    override fun disconnect() = gatt.disconnect()

    /**
     * Send [command] and yield the WIFI_STATE notifications that follow.
     * Subscribes BEFORE writing, so a fast board cannot notify before we listen.
     */
    private fun statesAfter(command: Byte): Flow<WifiState> = gatt.notifications
        .onSubscription { gatt.write(BleConstants.COMMAND, byteArrayOf(command)) }
        .filter { it.first == BleConstants.WIFI_STATE }
        .map { BleCodec.decodeWifiState(it.second) }

    private companion object {
        const val SCAN_TIMEOUT_MS = 20_000L
        // The board itself gives up after 20 s; leave room for the notification.
        const val JOIN_TIMEOUT_MS = 40_000L
    }
}
