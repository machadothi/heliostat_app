package com.machadothi.templateapp.ui.screen.provision

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.SectionCard
import com.machadothi.templateapp.ui.component.SignalBars
import kotlinx.coroutines.delay

@Composable
fun ProvisionScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProvisionViewModel = hiltViewModel(),
) {
    ProvisionContent(
        state = viewModel.uiState,
        ssid = viewModel.ssid,
        password = viewModel.password,
        onSsid = { viewModel.ssid = it },
        onPassword = { viewModel.password = it },
        onPick = viewModel::pick,
        onRescan = { viewModel.rescan() },
        onManual = viewModel::enterManually,
        onJoin = viewModel::join,
        onRetry = viewModel::connect,
        onDone = onDone,
        onBack = onBack,
    )
}

@Composable
fun ProvisionContent(
    state: ProvisionUiState,
    ssid: String,
    password: String,
    onSsid: (String) -> Unit,
    onPassword: (String) -> Unit,
    onPick: (VisibleNetwork) -> Unit,
    onRescan: () -> Unit,
    onManual: () -> Unit,
    onJoin: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { HeliostatTopBar("Connect to WiFi", subtitle = "my_heliostat", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Stepper(
                current = when (state) {
                    ProvisionUiState.Connecting, is ProvisionUiState.Error -> 0
                    is ProvisionUiState.Ready, is ProvisionUiState.Joining -> 1
                    is ProvisionUiState.Joined -> 2
                },
            )

            when (state) {
                ProvisionUiState.Connecting -> Busy("Connecting over Bluetooth…")

                is ProvisionUiState.Error -> Banner(
                    state.message,
                    BannerKind.Error,
                    title = "Connection failed",
                    actions = { if (state.canRetry) Button(onClick = onRetry) { Text("Try again") } },
                )

                is ProvisionUiState.Joining -> Busy(state.step)

                is ProvisionUiState.Joined -> {
                    Joined(state)
                    LaunchedEffect(state) {
                        delay(1_800)
                        onDone()
                    }
                }

                is ProvisionUiState.Ready ->
                    ReadyForm(state, ssid, password, onSsid, onPassword, onPick, onRescan, onManual, onJoin)
            }
        }
    }
}

@Composable
private fun ReadyForm(
    state: ProvisionUiState.Ready,
    ssid: String,
    password: String,
    onSsid: (String) -> Unit,
    onPassword: (String) -> Unit,
    onPick: (VisibleNetwork) -> Unit,
    onRescan: () -> Unit,
    onManual: () -> Unit,
    onJoin: () -> Unit,
) {
    state.error?.let { Banner(it, BannerKind.Error, title = "Couldn't connect the heliostat") }
    state.problems.forEach { Banner(it, BannerKind.Warning) }

    val phone = state.phone
    if (phone != null && !phone.is24GHz) {
        Banner(
            title = "Your phone is on ${phone.ssid}, a 5 GHz network",
            text = "The heliostat only has 2.4 GHz WiFi. Pick its 2.4 GHz network below." +
                (if (ssid.isNotBlank()) " $ssid is selected: routers usually use the same password for both." else ""),
            kind = BannerKind.Warning,
        )
    }

    SectionCard(title = "Choose a network") {
        Text(
            "Networks the heliostat can hear from where it is.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.scanning && state.visible.isEmpty() -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("The heliostat is scanning…")
            }

            state.scanError != null -> Text(state.scanError, color = MaterialTheme.colorScheme.error)

            state.visible.isEmpty() -> Text("No networks found.")
        }
        state.visible.take(10).forEachIndexed { index, network ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val selected = !state.manual && network.ssid == ssid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onPick(network) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SignalBars(network.rssi)
                Column(Modifier.weight(1f)) {
                    Text(network.ssid, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        buildString {
                            append("channel ${network.channel} · ${network.rssi} dBm")
                            if (phone?.ssid == network.ssid) append(" · your phone's network")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onRescan, enabled = !state.scanning) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(if (state.scanning) "  Scanning…" else "  Scan again")
            }
            if (!state.manual) {
                TextButton(onClick = onManual) { Text("Not listed? Type it") }
            }
        }
    }

    if (state.manual) {
        OutlinedTextField(
            value = ssid,
            onValueChange = onSsid,
            label = { Text("Network name") },
            leadingIcon = { Icon(Icons.Rounded.Wifi, contentDescription = null) },
            supportingText = { Text("Must be a 2.4 GHz network") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (ssid.isNotBlank()) {
        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = password,
            onValueChange = onPassword,
            label = { Text("Password for $ssid") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) { Text("Connect heliostat to $ssid") }
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            if (state.locationSent) "Time and location sent to the heliostat" else "Time sent · location unavailable",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stepper(current: Int) {
    val steps = listOf(
        "Bluetooth" to Icons.Rounded.Bluetooth,
        "WiFi" to Icons.Rounded.Wifi,
        "Done" to Icons.Rounded.Flag,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, (label, icon) ->
            Step(label, icon, done = index < current, active = index == current, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Step(label: String, icon: ImageVector, done: Boolean, active: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val background = when {
        done -> scheme.primary
        active -> scheme.primaryContainer
        else -> scheme.surfaceContainerHigh
    }
    val content = when {
        done -> scheme.onPrimary
        active -> scheme.onPrimaryContainer
        else -> scheme.onSurfaceVariant
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) { Icon(if (done) Icons.Rounded.Check else icon, contentDescription = null, tint = content) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (active || done) scheme.onSurface else scheme.onSurfaceVariant)
    }
}

@Composable
private fun Busy(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(Modifier.size(48.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Joined(state: ProvisionUiState.Joined) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(12.dp)
                    .size(56.dp),
            )
        }
        Text("Connected to ${state.ssid}", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "The heliostat is at ${state.ip}.\nBluetooth switches off in a few seconds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
