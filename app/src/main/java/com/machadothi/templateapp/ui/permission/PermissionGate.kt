package com.machadothi.templateapp.ui.permission

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.machadothi.templateapp.ble.BlePermissions
import android.bluetooth.BluetoothManager
import android.content.Context

/**
 * Shows [content] only once everything BLE provisioning needs is in place:
 * the runtime permissions, Bluetooth switched on, and Location Services on.
 *
 * Each missing piece gets its own explanation and its own fix, because the
 * failure modes are silent otherwise: without Location Services, scans on older
 * Android return nothing and the phone's SSID reads as "<unknown ssid>".
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Bumped on every resume, so returning from Settings re-checks everything.
    var generation by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        generation++
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { generation++ }
    val enableBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { generation++ }

    @Suppress("UNUSED_EXPRESSION") generation // recompose on change
    val missing = BlePermissions.missing(context)
    val bluetoothOn = bluetoothOn(context)
    val locationOn = BlePermissions.locationServicesOn(context)

    when {
        missing.isNotEmpty() -> Explain(
            icon = Icons.Rounded.Bluetooth,
            title = "Permissions needed",
            body = "Bluetooth, to find the heliostat and hand it your WiFi details.\n\n" +
                "Location, because Android only tells apps the name of the WiFi network " +
                "you are on when location is allowed. It is also where the heliostat " +
                "computes the sun's position from.",
            action = "Allow",
        ) { permissionLauncher.launch(BlePermissions.required) }

        !bluetoothOn -> Explain(
            icon = Icons.Rounded.BluetoothDisabled,
            title = "Bluetooth is off",
            body = "Setup talks to the heliostat over Bluetooth.",
            action = "Turn on Bluetooth",
        ) {
            @Suppress("MissingPermission") // BLUETOOTH_CONNECT granted above
            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        !locationOn -> Explain(
            icon = Icons.Rounded.LocationOff,
            title = "Location Services are off",
            body = "With Location off, Android hides the name of your WiFi network and " +
                "older phones find no Bluetooth devices at all.",
            action = "Open location settings",
        ) { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }

        else -> content()
    }
}

private fun bluetoothOn(context: Context): Boolean =
    (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true

@Composable
fun Explain(icon: ImageVector, title: String, body: String, action: String, onAction: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAction, modifier = Modifier.height(52.dp)) { Text(action) }
        }
    }
}
