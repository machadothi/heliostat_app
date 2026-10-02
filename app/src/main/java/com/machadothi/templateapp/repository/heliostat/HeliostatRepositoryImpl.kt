package com.machadothi.templateapp.repository.heliostat

import com.machadothi.templateapp.data.local.HeliostatPrefs
import com.machadothi.templateapp.data.network.ErrorResponse
import com.machadothi.templateapp.data.network.HeliostatService
import com.machadothi.templateapp.data.network.HostSelectionInterceptor
import com.machadothi.templateapp.data.network.JogRequest
import com.machadothi.templateapp.data.network.LocationRequest
import com.machadothi.templateapp.data.network.ModeRequest
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TargetRequest
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.data.network.TimeRequest
import com.machadothi.templateapp.discovery.FoundHeliostat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.util.TimeZone
import javax.inject.Inject

class HeliostatRepositoryImpl @Inject constructor(
    private val service: HeliostatService,
    private val hostSelector: HostSelectionInterceptor,
    private val prefs: HeliostatPrefs,
    private val json: Json,
) : HeliostatRepository {

    override suspend fun host(): String? {
        val saved = hostSelector.host ?: prefs.host()
        hostSelector.host = saved
        return saved
    }

    override suspend fun useHost(host: String) {
        hostSelector.host = host
        prefs.setHost(host)
    }

    override fun telemetry(periodMs: Long): Flow<Result<TelemetryResponse>> = flow {
        host()
        while (true) {
            emit(call { service.telemetry() })
            delay(periodMs)
        }
    }

    override suspend fun status() = call { service.status() }.onSuccess { status ->
        // Learn who is at this address: a heliostat added by typing its IP, or set
        // up over Bluetooth, gets an id here -- and with it, rediscovery by id.
        val device = status.device
        val id = device?.id
        if (id != null) {
            val saved = prefs.remembered()
            if (saved?.id != id || saved.name != device.name) prefs.setIdentity(id, device.name)
        }
    }

    override suspend fun setMode(mode: String) = call { service.setMode(ModeRequest(mode)) }.map {}

    override suspend fun jog(axis: Int, deltaDeg: Double) =
        call { service.jog(JogRequest(axis, deltaDeg)) }.map { it.target_deg }

    override suspend fun clearFault() = call { service.clear() }.map {}

    override suspend fun setTarget(azimuth: Double, elevation: Double) =
        call { service.setTarget(TargetRequest(azimuth, elevation)) }.mapCatching {
            it.target ?: error("no target in reply")
        }

    override suspend fun captureTarget() = call { service.captureTarget() }.mapCatching {
        it.target ?: error("no target in reply")
    }

    override suspend fun sendTime(): Result<Unit> {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault().getOffset(now) / 60_000
        return call { service.setTime(TimeRequest(now / 1000, tz)) }.map {}
    }

    override suspend fun sendLocation(lat: Double, lon: Double, elevationM: Double) =
        call { service.setLocation(LocationRequest(lat, lon, elevationM)) }.map {}

    override suspend fun setLevel() = call { service.setImuLevel() }.map {}

    override suspend fun remembered() = prefs.remembered()

    override suspend fun choose(found: FoundHeliostat) {
        hostSelector.host = found.host
        prefs.remember(found.host, found.id, found.name)
    }

    /**
     * Run one request, turning every failure into a Result with a message a person
     * can act on -- the board's own {"error": ...} text when there is one.
     */
    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        if (host() == null) {
            Result.failure(IllegalStateException("No heliostat set up yet"))
        } else {
            Result.success(block())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string()
        val message = body?.let { runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull() }
        Result.failure(IllegalStateException(message ?: "HTTP ${e.code()}"))
    } catch (e: IOException) {
        // EPERM on socket() is the OS refusing this APP any network access -- the
        // request never leaves the phone. On GrapheneOS that is its per-app Network
        // permission being off (Bluetooth is not covered by it, so provisioning
        // still works). Elsewhere: a firewall app, or stale permissions after an
        // in-place update.
        if (e.message?.contains("EPERM") == true) {
            Result.failure(NetworkBlockedException())
        } else {
            Result.failure(IllegalStateException("No response (${e.message ?: "network error"})"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * The phone refused this app any network access (EPERM on socket creation).
 *
 * On GrapheneOS this is its per-app Network permission being off. It cannot be
 * requested at runtime -- GrapheneOS only offers it at install time and in
 * Settings, and otherwise "pretends the network is down" -- so the app detects
 * the symptom and sends the user to its settings page instead.
 */
class NetworkBlockedException : IllegalStateException(
    "The phone is blocking this app's network access (on GrapheneOS: the Network permission).",
)
