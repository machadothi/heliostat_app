package com.machadothi.templateapp.repository.heliostat

import com.machadothi.templateapp.data.local.Remembered
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.discovery.FoundHeliostat
import kotlinx.coroutines.flow.Flow

/**
 * The WiFi side: everything after provisioning, over HTTP on the home network.
 * Every call returns a Result; a failure carries the board's own error message.
 */
interface HeliostatRepository {

    /** The address in use, or null before any heliostat has been provisioned. */
    suspend fun host(): String?

    /** Remember and switch to a host, e.g. an IP learned over BLE, or "ip:8080". */
    suspend fun useHost(host: String)

    /** Telemetry every [periodMs], forever. Errors are emitted, not thrown. */
    fun telemetry(periodMs: Long = 500): Flow<Result<TelemetryResponse>>

    suspend fun status(): Result<StatusResponse>
    suspend fun setMode(mode: String): Result<Unit>
    suspend fun jog(axis: Int, deltaDeg: Double): Result<Double>
    suspend fun clearFault(): Result<Unit>
    suspend fun setTarget(azimuth: Double, elevation: Double): Result<StatusResponse.Target>
    suspend fun captureTarget(): Result<StatusResponse.Target>
    suspend fun sendTime(): Result<Unit>
    suspend fun sendLocation(lat: Double, lon: Double, elevationM: Double): Result<Unit>

    /** Adopt the base's current attitude as level: the tilt safety rule measures from it. */
    suspend fun setLevel(): Result<Unit>

    /** The heliostat this phone used last, or null if none was ever set up. */
    suspend fun remembered(): Remembered?

    /** Switch to a heliostat found on the network, and remember it for next time. */
    suspend fun choose(found: FoundHeliostat)
}
