package com.machadothi.templateapp.ui.screen.dashboard

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.MetricTile
import com.machadothi.templateapp.ui.component.ModeBadge
import com.machadothi.templateapp.ui.component.SafetyBar
import com.machadothi.templateapp.ui.component.SectionCard
import com.machadothi.templateapp.ui.component.SkyDial
import com.machadothi.templateapp.ui.component.SkyPoint

@Composable
fun DashboardScreen(
    onJog: () -> Unit,
    onTarget: () -> Unit,
    onSetupAgain: () -> Unit,
    /** Open the network search; [auto] reconnects by itself if the heliostat is found. */
    onFind: (auto: Boolean) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }
    DashboardContent(
        state = viewModel.uiState,
        status = viewModel.status,
        actionMessage = viewModel.actionMessage,
        onMode = viewModel::setMode,
        onClearFault = viewModel::clearFault,
        onJog = onJog,
        onTarget = onTarget,
        onSendTime = viewModel::sendTime,
        onSendLocation = viewModel::sendLocation,
        onSetLevel = viewModel::setLevel,
        onFind = onFind,
        // The saved address is kept: a successful setup replaces it, and backing
        // out of setup must not leave the app with no heliostat at all.
        onSetupAgain = onSetupAgain,
    )
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    status: StatusResponse?,
    actionMessage: String?,
    onMode: (String) -> Unit,
    onClearFault: () -> Unit,
    onJog: () -> Unit,
    onTarget: () -> Unit,
    onSendTime: () -> Unit,
    onSendLocation: () -> Unit,
    onSetLevel: () -> Unit,
    onFind: (auto: Boolean) -> Unit,
    onSetupAgain: () -> Unit,
    bottomBar: @Composable () -> Unit = { SafetyBar() },
) {
    val host = (state as? DashboardUiState.Live)?.host ?: (state as? DashboardUiState.Unreachable)?.host
    Scaffold(
        topBar = {
            HeliostatTopBar(
                title = status?.device?.name ?: "Heliostat",
                subtitle = listOfNotNull(host, status?.device?.fw?.let { "fw $it" }).joinToString("  ·  ")
                    .ifEmpty { null },
                actions = { OverflowMenu(onSendTime, onSendLocation, onSetLevel, { onFind(false) }, onSetupAgain) },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state) {
                DashboardUiState.Loading -> Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                is DashboardUiState.Unreachable -> if (state.networkBlocked) {
                    NetworkBlocked()
                } else Banner(
                    title = "Can't reach the heliostat at ${state.host}",
                    text = "${state.message}\n\nSearching the network finds it again if its address " +
                        "changed. If the heliostat moved to a different WiFi, set it up again: it turns " +
                        "Bluetooth back on after 5 minutes offline, or hold its BOOT button for 3 seconds.",
                    kind = BannerKind.Error,
                    actions = {
                        Button(onClick = { onFind(true) }) { Text("Search network") }
                        OutlinedButton(onClick = onSetupAgain) { Text("Set up again") }
                    },
                )

                is DashboardUiState.Live -> {
                    if (state.reconnecting) Reconnecting()
                    Live(state.telemetry, status, onMode, onClearFault, onJog, onTarget)
                }
            }
            actionMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Live(
    t: TelemetryResponse,
    status: StatusResponse?,
    onMode: (String) -> Unit,
    onClearFault: () -> Unit,
    onJog: () -> Unit,
    onTarget: () -> Unit,
) {
    t.latched?.let { reason ->
        Banner(
            title = "Stopped for safety",
            text = "$reason\n\nCheck the machine, then clear the fault.",
            kind = BannerKind.Error,
            actions = { Button(onClick = onClearFault) { Text("Clear fault") } },
        )
    }

    // A servo that stops answering reads as null volts. By far the likeliest
    // cause on the bench is its supply being off, so say that first.
    val silent = t.volts.withIndex().filter { it.value == null }.map { if (it.index == 0) "azimuth" else "elevation" }
    if (silent.isNotEmpty() && t.volts.isNotEmpty()) {
        Banner(
            title = "The ${silent.joinToString(" and ")} servo${if (silent.size > 1) "s aren't" else " isn't"} answering",
            text = "Check that servo power is on and the bus cable is seated. The heliostat can't move " +
                "or hold position until ${if (silent.size > 1) "they answer" else "it answers"}.",
            kind = BannerKind.Warning,
        )
    }

    val trips = t.trips.filter { it != "comms" }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModeBadge(t.mode)
        // Only when the machine really is waiting: armed to track, held off by
        // night or weather. After a latched fault the intent is idle -- nothing waits.
        val waiting = t.latched == null && t.intent in listOf("track", "defocus") && t.intent != t.mode
        if (waiting) {
            Text(
                "Will ${t.intent} when ${trips.firstOrNull()?.let { waitingReason(it) } ?: "conditions allow"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (trips.isNotEmpty() && t.latched == null) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            trips.forEach {
                AssistChip(
                    onClick = {},
                    label = { Text(tripLabel(it)) },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null, Modifier.size(16.dp)) },
                )
            }
        }
    }

    SectionCard(title = null) {
        SkyDial(
            sun = t.sun?.toSkyPoint(),
            // A mirror facing below the horizon (stowed face-down) has no place on
            // a map of the sky; its angles are still in the tile below.
            mirror = t.pos.takeIf { it.size == 2 && it.all { v -> v != null } && it[1]!! >= 0 }
                ?.let { SkyPoint(it[0]!!, it[1]!!) },
            beam = t.beam?.toSkyPoint(),
            target = status?.target?.let { SkyPoint(it.az, it.el) },
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile("Sun elevation", t.sun?.getOrNull(1)?.fmt() ?: "—", Modifier.weight(1f), unit = "°")
        MetricTile("Efficiency", t.efficiency?.let { "${(it * 100).toInt()}" } ?: "—", Modifier.weight(1f), unit = "%")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            "Mirror az / el",
            "${t.pos.getOrNull(0)?.fmt() ?: "—"} / ${t.pos.getOrNull(1)?.fmt() ?: "—"}",
            Modifier.weight(1f),
            unit = "°",
        )
        MetricTile(
            "Servos",
            t.volts.joinToString(" / ") { v -> v?.let { "%.1f".format(it) } ?: "—" },
            Modifier.weight(1f),
            unit = "V",
        )
    }
    if (t.tilt_deg != null || t.accel_g != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Base tilt", t.tilt_deg?.let { "%.2f".format(it) } ?: "—", Modifier.weight(1f), unit = "°")
            MetricTile("Acceleration", t.accel_g?.let { "%.2f".format(it) } ?: "—", Modifier.weight(1f), unit = "g")
        }
        if (t.imu_calibrated == false) {
            Text(
                "Tilt is measured from how the base sat at power-on. Once the heliostat is installed, " +
                    "use ⋮ → Set level here to make its position the reference.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionCard(title = "Mode") {
        val choices = listOf(
            Triple("track", "Track", Icons.Rounded.WbSunny),
            Triple("idle", "Idle", Icons.Rounded.PauseCircle),
            Triple("manual", "Manual", Icons.Rounded.PanTool),
        )
        // Manual is reached via idle automatically (see DashboardViewModel.setMode),
        // so it is offered whenever idle is.
        val allowed = status?.allowed_modes.orEmpty().let { if ("idle" in it) it + "manual" else it }
        val current = if (t.intent == "track") "track" else t.mode
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            choices.forEachIndexed { index, (mode, label, icon) ->
                SegmentedButton(
                    selected = current == mode,
                    onClick = { onMode(mode) },
                    enabled = allowed.isEmpty() || mode in allowed || mode == t.mode,
                    shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                ) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Jog", Icons.Rounded.OpenWith, Modifier.weight(1f), onJog)
            ActionButton("Target", Icons.Rounded.GpsFixed, Modifier.weight(1f), onTarget)
        }
    }
}

/** Requests are failing, but not for long enough to call the heliostat unreachable. */
@Composable
private fun Reconnecting() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            "Weak connection — retrying…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The phone refuses this app any network access. On GrapheneOS that is the
 * per-app Network permission, which apps cannot request at runtime -- so send
 * the user straight to this app's settings page, where the toggle lives.
 */
@Composable
private fun NetworkBlocked() {
    val context = LocalContext.current
    Banner(
        title = "This app isn't allowed to use the network",
        text = "The phone is blocking the heliostat app's network access, so it can't reach " +
            "the heliostat over WiFi (Bluetooth setup still works, which is why it got this far).\n\n" +
            "On GrapheneOS: Permissions → Network → Allow. It's in this app's settings page.",
        kind = BannerKind.Error,
        actions = {
            Button(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) { Text("Open app settings") }
        },
    )
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text("  $label")
    }
}

@Composable
private fun OverflowMenu(
    onSendTime: () -> Unit,
    onSendLocation: () -> Unit,
    onSetLevel: () -> Unit,
    onSwitch: () -> Unit,
    onSetupAgain: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More") }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        MenuItem("Send phone time", Icons.Rounded.AccessTime) { open = false; onSendTime() }
        MenuItem("Send phone location", Icons.Rounded.MyLocation) { open = false; onSendLocation() }
        MenuItem("Set level here", Icons.Rounded.Straighten) { open = false; onSetLevel() }
        MenuItem("Switch heliostat", Icons.Rounded.Wifi) { open = false; onSwitch() }
        MenuItem("Set up a new one (Bluetooth)", Icons.Rounded.Bluetooth) { open = false; onSetupAgain() }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

/** Safety rule id -> something a person can read. */
private fun tripLabel(rule: String) = when (rule) {
    "sun_low" -> "Sun too low"
    "no_time" -> "No valid time"
    "unreachable" -> "Target unreachable"
    "keepout" -> "Keep-out zone"
    "tilt" -> "Base tilted"
    "weather" -> "Weather"
    "servo" -> "Servo fault"
    "limit" -> "Limit switch"
    "estop" -> "E-stop"
    else -> rule.replace('_', ' ')
}

private fun waitingReason(rule: String) = when (rule) {
    "sun_low" -> "the sun is high enough"
    "no_time" -> "it has the time"
    "weather" -> "the weather clears"
    "unreachable" -> "the target is reachable"
    else -> "conditions allow"
}

private fun List<Double>.toSkyPoint() = if (size >= 2) SkyPoint(this[0], this[1]) else null

private fun Double.fmt() = "%.1f".format(this)
