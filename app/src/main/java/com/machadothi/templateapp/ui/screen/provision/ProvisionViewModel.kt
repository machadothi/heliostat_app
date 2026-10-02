package com.machadothi.templateapp.ui.screen.provision

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.ble.WifiState
import com.machadothi.templateapp.data.local.HeliostatPrefs
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.repository.heliostat.ProvisioningRepository
import com.machadothi.templateapp.ui.navigation.NavRoutes
import com.machadothi.templateapp.wifi.CurrentNetwork
import com.machadothi.templateapp.wifi.PhoneLocation
import com.machadothi.templateapp.wifi.PhoneNetwork
import com.machadothi.templateapp.wifi.WifiBands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HeliostatBle"

/**
 * Hands the heliostat the credentials of a WiFi network it can actually hear.
 *
 * Connect over BLE -> send the phone's time and location -> ask the board to scan
 * -> the user PICKS a network from the board's own list (typing a name is the
 * fallback) -> enter the password -> join -> on success remember the IP and
 * switch to HTTP.
 *
 * Picking from the board's scan is the primary path because the board is the
 * one that has to reach the network: it is 2.4 GHz only, and it may be further
 * from the router than the phone. The phone's own network is used only to
 * pre-select a matching entry, and never picks something the board cannot see.
 *
 * Every step's failure is kept and shown -- a silently swallowed BLE error
 * looks, on screen, exactly like a board that ignores you.
 */
@HiltViewModel
class ProvisionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val provisioning: ProvisioningRepository,
    private val heliostat: HeliostatRepository,
    private val prefs: HeliostatPrefs,
    private val currentNetwork: CurrentNetwork,
    private val phoneLocation: PhoneLocation,
) : ViewModel() {

    private val address = savedStateHandle.toRoute<NavRoutes.Provision>().address

    var uiState by mutableStateOf<ProvisionUiState>(ProvisionUiState.Connecting)
        private set

    var ssid by mutableStateOf("")
    var password by mutableStateOf("")

    init {
        connect()
    }

    fun connect() {
        uiState = ProvisionUiState.Connecting
        viewModelScope.launch {
            Log.d(TAG, "provision: connecting to $address")
            provisioning.connect(address).onFailure {
                Log.d(TAG, "provision: connect failed: $it")
                uiState = ProvisionUiState.Error("Could not connect: ${it.message}", canRetry = true)
                return@launch
            }

            val problems = mutableListOf<String>()
            provisioning.sendTime()
                .onSuccess { Log.d(TAG, "provision: time sent") }
                .onFailure { problems += "Sending the time failed: ${it.message}"; Log.d(TAG, "provision: time failed: $it") }

            val location = phoneLocation.current()
            if (location != null) {
                provisioning.sendLocation(location.latitude, location.longitude, location.altitude)
                    .onSuccess { Log.d(TAG, "provision: location sent") }
                    .onFailure { problems += "Sending the location failed: ${it.message}" }
            } else {
                Log.d(TAG, "provision: no phone location available")
            }

            val phone = currentNetwork.read()
            Log.d(TAG, "provision: phone network = $phone")
            uiState = ProvisionUiState.Ready(
                phone = phone,
                visible = emptyList(),
                scanning = true,
                scanError = null,
                locationSent = location != null && problems.none { it.startsWith("Sending the location") },
                problems = problems,
                manual = false,
                error = null,
            )
            scan(preselect = true)
        }
    }

    /** Ask the board which networks IT can hear. */
    fun rescan() = viewModelScope.launch { scan(preselect = false) }

    private suspend fun scan(preselect: Boolean) {
        val ready = uiState as? ProvisionUiState.Ready ?: return
        uiState = ready.copy(scanning = true, scanError = null)
        val result = provisioning.scanWifi()
        val current = uiState as? ProvisionUiState.Ready ?: return
        result.onSuccess { visible ->
            Log.d(TAG, "provision: board sees ${visible.map { it.ssid }}")
            uiState = current.copy(visible = visible, scanning = false, scanError = null)
            if (preselect && ssid.isBlank()) preselect(current.phone, visible)
        }.onFailure {
            Log.d(TAG, "provision: scan failed: $it")
            uiState = current.copy(scanning = false, scanError = "Couldn't get the network list: ${it.message}")
        }
    }

    /** Pre-select the phone's network -- but only from what the BOARD can hear. */
    private suspend fun preselect(phone: PhoneNetwork?, visible: List<VisibleNetwork>) {
        val choice = when {
            phone == null -> null
            phone.is24GHz -> visible.firstOrNull { it.ssid == phone.ssid }
            else -> WifiBands.suggest24GHzSibling(phone.ssid, visible)
        }
        Log.d(TAG, "provision: preselected ${choice?.ssid}")
        choice?.let { pick(it) }
    }

    fun pick(network: VisibleNetwork) {
        ssid = network.ssid
        viewModelScope.launch { prefs.password(network.ssid)?.let { password = it } }
        (uiState as? ProvisionUiState.Ready)?.let { uiState = it.copy(manual = false, error = null) }
    }

    fun enterManually() {
        (uiState as? ProvisionUiState.Ready)?.let { uiState = it.copy(manual = true) }
    }

    fun join() {
        val ready = uiState as? ProvisionUiState.Ready ?: return
        val chosenSsid = ssid.trim()
        val chosenPassword = password
        uiState = ProvisionUiState.Joining(chosenSsid, "Sending the network details…")
        viewModelScope.launch {
            Log.d(TAG, "provision: join '$chosenSsid' (password ${chosenPassword.length} chars)")
            var outcome: WifiState? = null
            runCatching {
                provisioning.join(chosenSsid, chosenPassword).collect { state ->
                    Log.d(TAG, "provision: board says $state")
                    outcome = state
                    if (state.phase == WifiState.Phase.JOINING) {
                        uiState = ProvisionUiState.Joining(chosenSsid, "The heliostat is joining $chosenSsid…")
                    }
                }
            }.onFailure {
                Log.d(TAG, "provision: join failed: $it")
                uiState = ready.copy(error = "Lost contact with the heliostat: ${it.message}")
                return@launch
            }

            val result = outcome
            if (result?.phase == WifiState.Phase.JOINED && result.ip != null) {
                prefs.saveProvisioned(result.ip, address, chosenSsid, chosenPassword)
                heliostat.useHost(result.ip)
                provisioning.disconnect()
                uiState = ProvisionUiState.Joined(chosenSsid, result.ip)
            } else {
                uiState = ready.copy(error = result?.reason?.message ?: "The heliostat did not answer")
            }
        }
    }

    override fun onCleared() {
        provisioning.disconnect()
    }
}

sealed class ProvisionUiState {
    data object Connecting : ProvisionUiState()

    data class Ready(
        val phone: PhoneNetwork?,
        /** Networks the BOARD can hear, strongest first. */
        val visible: List<VisibleNetwork>,
        val scanning: Boolean,
        val scanError: String?,
        val locationSent: Boolean,
        /** BLE steps that failed on the way here (time, location). */
        val problems: List<String>,
        /** The user chose to type the network name instead of picking one. */
        val manual: Boolean,
        val error: String?,
    ) : ProvisionUiState()

    data class Joining(val ssid: String, val step: String) : ProvisionUiState()
    data class Joined(val ssid: String, val ip: String) : ProvisionUiState()
    data class Error(val message: String, val canRetry: Boolean) : ProvisionUiState()
}
