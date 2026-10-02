package com.machadothi.templateapp.ui.screen.dashboard

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.repository.heliostat.NetworkBlockedException
import com.machadothi.templateapp.wifi.PhoneLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UNREACHABLE_AFTER_MS = 12_000L

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: HeliostatRepository,
    private val phoneLocation: PhoneLocation,
) : ViewModel() {

    var uiState by mutableStateOf<DashboardUiState>(DashboardUiState.Loading)
        private set

    /** Result of the last button press, shown under the controls. */
    var actionMessage by mutableStateOf<String?>(null)
        private set

    var status by mutableStateOf<StatusResponse?>(null)
        private set

    private var polling: Job? = null

    /**
     * Poll at 2 Hz while the screen is visible.
     *
     * One failed request is not "unreachable": on a weak WiFi link single requests
     * time out now and then, and flipping the whole screen to an error for each
     * made a working heliostat look dead. The last good telemetry stays up, marked
     * as reconnecting, until nothing has succeeded for [UNREACHABLE_AFTER_MS].
     */
    fun startPolling() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            refreshStatus()
            var lastSuccess = 0L
            repository.telemetry(periodMs = 500).collect { result ->
                val host = repository.host().orEmpty()
                val now = SystemClock.elapsedRealtime()
                val live = uiState as? DashboardUiState.Live
                uiState = result.fold(
                    onSuccess = {
                        lastSuccess = now
                        DashboardUiState.Live(it, host)
                    },
                    onFailure = {
                        if (it !is NetworkBlockedException && live != null && now - lastSuccess < UNREACHABLE_AFTER_MS) {
                            live.copy(reconnecting = true)
                        } else {
                            DashboardUiState.Unreachable(
                                it.message ?: "unreachable", host, networkBlocked = it is NetworkBlockedException,
                            )
                        }
                    },
                )
            }
        }
    }

    fun stopPolling() {
        polling?.cancel()
        polling = null
    }

    /**
     * The heliostat only allows MANUAL from IDLE, so the operator stops tracking
     * deliberately. The button does that step for them instead of failing.
     */
    fun setMode(mode: String) = act("Mode: $mode") {
        val current = (uiState as? DashboardUiState.Live)?.telemetry?.mode
        if (mode == "manual" && current != "idle" && current != "manual") {
            repository.setMode("idle").onFailure { return@act Result.failure<Unit>(it) }
        }
        repository.setMode(mode).also { refreshStatus() }
    }

    fun clearFault() = act("Fault cleared") { repository.clearFault() }

    fun sendTime() = act("Time sent") { repository.sendTime() }

    fun sendLocation() = act("Location sent") {
        val location = phoneLocation.current()
            ?: return@act Result.failure<Unit>(IllegalStateException("Phone location unavailable"))
        repository.sendLocation(location.latitude, location.longitude, location.altitude)
    }

    fun setLevel() = act("Level set: tilt is now measured from here") { repository.setLevel() }

    private suspend fun refreshStatus() {
        repository.status().onSuccess { status = it }
    }

    private fun act(success: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            actionMessage = block().fold({ success }, { it.message ?: "failed" })
        }
    }
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Live(
        val telemetry: TelemetryResponse,
        val host: String,
        /** The last requests failed; [telemetry] is the most recent that arrived. */
        val reconnecting: Boolean = false,
    ) : DashboardUiState()
    data class Unreachable(
        val message: String,
        val host: String,
        /** The PHONE refuses this app network access -- not a heliostat problem. */
        val networkBlocked: Boolean = false,
    ) : DashboardUiState()
}
