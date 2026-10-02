package com.machadothi.templateapp.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** The heliostat's HTTP API. docs/protocol.md in the heliostat repo is the contract. */
interface HeliostatService {

    @GET("/api/status")
    suspend fun status(): StatusResponse

    @GET("/api/telemetry")
    suspend fun telemetry(): TelemetryResponse

    @POST("/api/mode")
    suspend fun setMode(@Body body: ModeRequest): JsonObject

    @POST("/api/jog")
    suspend fun jog(@Body body: JogRequest): JogResponse

    @POST("/api/clear")
    suspend fun clear(): JsonObject

    @POST("/api/target")
    suspend fun setTarget(@Body body: TargetRequest): TargetResponse

    @POST("/api/target/capture")
    suspend fun captureTarget(): TargetResponse

    @POST("/api/time")
    suspend fun setTime(@Body body: TimeRequest): JsonObject

    @POST("/api/location")
    suspend fun setLocation(@Body body: LocationRequest): JsonObject

    @POST("/api/imu/level")
    suspend fun setImuLevel(): JsonObject
}

// --- requests ------------------------------------------------------------------------

@Serializable
data class ModeRequest(val mode: String)

@Serializable
data class JogRequest(val axis: Int, val delta_deg: Double)

@Serializable
data class TargetRequest(val az: Double, val el: Double, val mode: String = "azel")

@Serializable
data class TimeRequest(val epoch: Long, val tz_offset_min: Int)

@Serializable
data class LocationRequest(val lat: Double, val lon: Double, val elev_m: Double = 0.0)

// --- responses -----------------------------------------------------------------------
// Only the fields the app uses; the Json instance ignores the rest.

@Serializable
data class TelemetryResponse(
    val mode: String,
    val intent: String,
    val time: Double? = null,
    val sun: List<Double>? = null,
    val plan: List<Double>? = null,
    val pos: List<Double?> = emptyList(),
    val moving: List<Boolean> = emptyList(),
    val beam: List<Double>? = null,
    val efficiency: Double? = null,
    val trips: List<String> = emptyList(),
    val latched: String? = null,
    val volts: List<Double?> = emptyList(),
    val temp_c: List<Int?> = emptyList(),
    /** Base tilt from the MPU6050, degrees from level (or from the boot attitude until calibrated). */
    val tilt_deg: Double? = null,
    /** Acceleration magnitude in g: 1.0 at rest, more when shaken. */
    val accel_g: Double? = null,
    /** Whether level has been set; null when there is no IMU. */
    val imu_calibrated: Boolean? = null,
)

@Serializable
data class StatusResponse(
    val mode: String,
    val intent: String,
    val time_valid: Boolean = false,
    val allowed_modes: List<String> = emptyList(),
    val target: Target? = null,
    val site: Site? = null,
    val device: Device? = null,
    val wifi: Wifi? = null,
) {
    @Serializable
    data class Target(val mode: String = "azel", val az: Double = 0.0, val el: Double = 0.0)

    @Serializable
    data class Site(val lat: Double = 0.0, val lon: Double = 0.0)

    @Serializable
    data class Device(
        val fw: String = "?", val name: String = "?", val sim: Boolean = false,
        /** Stable id from the board's MAC (firmware with network discovery; null before). */
        val id: String? = null,
        val mem_free: Int? = null, val uptime_s: Double? = null,
    )

    @Serializable
    data class Wifi(val ssid: String? = null, val ip: String? = null, val rssi: Int? = null)
}

@Serializable
data class JogResponse(val ok: Boolean = false, val axis: Int = 0, val target_deg: Double = 0.0)

@Serializable
data class TargetResponse(val ok: Boolean = false, val target: StatusResponse.Target? = null)

@Serializable
data class ErrorResponse(val error: String)
