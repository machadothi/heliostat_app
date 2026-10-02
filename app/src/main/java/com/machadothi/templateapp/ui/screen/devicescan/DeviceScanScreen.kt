package com.machadothi.templateapp.ui.screen.devicescan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.ble.DiscoveredDevice
import com.machadothi.templateapp.repository.heliostat.ProvisioningRepository
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.SignalBars
import com.machadothi.templateapp.ui.permission.PermissionGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Finds heliostats in Bluetooth range. The board only advertises while it needs
 * setting up, so an empty list usually means it is already on WiFi -- which the
 * screen says, along with how to force Bluetooth on (hold BOOT for 3 s).
 */
@Composable
fun DeviceScanScreen(
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onUseAddress: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: DeviceScanViewModel = hiltViewModel(),
) {
    PermissionGate {
        DisposableEffect(Unit) {
            viewModel.start()
            onDispose { viewModel.stop() }
        }
        DeviceScanContent(
            state = viewModel.uiState,
            onDeviceSelected = {
                viewModel.stop()
                onDeviceSelected(it)
            },
            onUseAddress = onUseAddress,
            onBack = onBack,
        )
    }
}

@Composable
fun DeviceScanContent(
    state: DeviceScanUiState,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onUseAddress: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = { HeliostatTopBar(title = "Find your heliostat", subtitle = "Over Bluetooth", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val devices = (state as? DeviceScanUiState.Scanning)?.devices.orEmpty()
            Radar(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (devices.isEmpty()) 24.dp else 4.dp),
                compact = devices.isNotEmpty(),
            )

            when (state) {
                is DeviceScanUiState.Error -> Banner(state.message, BannerKind.Error, title = "Bluetooth problem")
                is DeviceScanUiState.Scanning -> if (devices.isEmpty()) {
                    Text(
                        "Looking for my_heliostat…",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Banner(
                        "The heliostat only advertises while it is not on WiFi. To set it up again, " +
                            "hold its BOOT button for 3 seconds: Bluetooth comes on for 5 minutes and " +
                            "its LED blinks three quick pulses.",
                        BannerKind.Info,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(devices, key = { it.address }) { DeviceCard(it) { onDeviceSelected(it) } }
                    }
                }
            }

            // The way out when the board is already on WiFi (Bluetooth off): make it
            // a real button, not a footnote -- otherwise this screen is a dead end.
            OutlinedButton(onClick = onUseAddress, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Already on WiFi? Enter its address")
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DiscoveredDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                SignalBars(device.rssi)
                Text(
                    "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Set up")
        }
    }
}

/** A sun sending out slow rings: "searching". Shared with the network search. */
@Composable
internal fun Radar(modifier: Modifier, compact: Boolean) {
    val transition = rememberInfiniteTransition(label = "radar")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val color = MaterialTheme.colorScheme.primary
    val size = if (compact) 72.dp else 180.dp
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val maxRadius = this.size.minDimension / 2
            for (ring in 0 until 3) {
                val t = (phase + ring / 3f) % 1f
                drawCircle(
                    color = color.copy(alpha = (1f - t) * 0.6f),
                    radius = maxRadius * (0.3f + 0.7f * t),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Icon(
            Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (compact) 28.dp else 56.dp),
        )
    }
}

@HiltViewModel
class DeviceScanViewModel @Inject constructor(
    private val repository: ProvisioningRepository,
) : ViewModel() {

    var uiState by mutableStateOf<DeviceScanUiState>(DeviceScanUiState.Scanning(emptyList()))
        private set

    private var scan: Job? = null

    fun start() {
        if (scan?.isActive == true) return
        scan = viewModelScope.launch {
            repository.discover()
                .catch { uiState = DeviceScanUiState.Error(it.message ?: "Bluetooth scan failed") }
                .collect { uiState = DeviceScanUiState.Scanning(it) }
        }
    }

    fun stop() {
        scan?.cancel()
        scan = null
    }
}

sealed class DeviceScanUiState {
    data class Scanning(val devices: List<DiscoveredDevice>) : DeviceScanUiState()
    data class Error(val message: String) : DeviceScanUiState()
}
