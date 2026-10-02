package com.machadothi.templateapp

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.machadothi.templateapp.ble.DiscoveredDevice
import com.machadothi.templateapp.ble.VisibleNetwork
import com.machadothi.templateapp.data.local.Remembered
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.discovery.FoundHeliostat
import com.machadothi.templateapp.ui.component.SafetyBarContent
import com.machadothi.templateapp.ui.permission.Explain
import com.machadothi.templateapp.ui.screen.dashboard.DashboardContent
import com.machadothi.templateapp.ui.screen.dashboard.DashboardUiState
import com.machadothi.templateapp.ui.screen.devicescan.DeviceScanContent
import com.machadothi.templateapp.ui.screen.devicescan.DeviceScanUiState
import com.machadothi.templateapp.ui.screen.find.FindContent
import com.machadothi.templateapp.ui.screen.find.FindUiState
import com.machadothi.templateapp.ui.screen.jog.JogContent
import com.machadothi.templateapp.ui.screen.provision.ProvisionContent
import com.machadothi.templateapp.ui.screen.provision.ProvisionUiState
import com.machadothi.templateapp.ui.screen.target.TargetContent
import com.machadothi.templateapp.ui.theme.MyApplicationTheme
import com.machadothi.templateapp.wifi.PhoneNetwork

// Values taken from the firmware's recorded responses (src/test/resources/firmware).
private val TRACKING = TelemetryResponse(
    mode = "track", intent = "track", time = 1782042013.5,
    sun = listOf(164.9, 61.3), plan = listOf(175.1, 35.9), pos = listOf(174.9, 35.7),
    moving = listOf(false, false), beam = listOf(179.4, 10.2), efficiency = 0.897,
    trips = emptyList(), latched = null, volts = listOf(12.3, 9.9), temp_c = listOf(25, 24),
    tilt_deg = 0.12, accel_g = 0.99, imu_calibrated = false,
)
private val STATUS = StatusResponse(
    mode = "track", intent = "track", time_valid = true,
    allowed_modes = listOf("idle", "defocus", "stow", "fault", "estop"),
    target = StatusResponse.Target("azel", 180.0, 10.0),
    device = StatusResponse.Device(fw = "0.1.0", name = "my_heliostat"),
)
private val SAFETY = @Composable { SafetyBarContent(message = null, onMode = {}) }

@Composable
private fun Dashboard(state: DashboardUiState, status: StatusResponse? = STATUS) = DashboardContent(
    state = state, status = status, actionMessage = null, onMode = {}, onClearFault = {}, onJog = {},
    onTarget = {}, onSendTime = {}, onSendLocation = {}, onSetLevel = {}, onFind = {}, onSetupAgain = {},
    bottomBar = SAFETY,
)

private const val W = 400
private const val H = 1100

@PreviewTest
@Preview(name = "dashboard_dark", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardTrackingDark() = MyApplicationTheme(darkTheme = true) {
    Dashboard(DashboardUiState.Live(TRACKING, "192.168.50.77"))
}

@PreviewTest
@Preview(name = "dashboard_light", widthDp = W, heightDp = H)
@Composable
fun DashboardTrackingLight() = MyApplicationTheme(darkTheme = false) {
    Dashboard(DashboardUiState.Live(TRACKING, "192.168.50.77"))
}

@PreviewTest
@Preview(name = "dashboard_fault", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardFault() = MyApplicationTheme(darkTheme = true) {
    Dashboard(
        DashboardUiState.Live(
            TRACKING.copy(mode = "stow", intent = "idle", latched = "base tilted 7.5 deg", trips = listOf("tilt"),
                pos = listOf(180.0, -89.9), beam = null, efficiency = null),
            "192.168.50.77",
        ),
    )
}

@PreviewTest
@Preview(name = "dashboard_servo_silent", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardServoSilent() = MyApplicationTheme(darkTheme = true) {
    Dashboard(DashboardUiState.Live(TRACKING.copy(mode = "idle", intent = "idle", volts = listOf(null, null)), "192.168.50.105"))
}

@PreviewTest
@Preview(name = "dashboard_unreachable", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardUnreachable() = MyApplicationTheme(darkTheme = true) {
    Dashboard(DashboardUiState.Unreachable("Can't reach the heliostat (timeout)", "192.168.50.77"), status = null)
}

@PreviewTest
@Preview(name = "dashboard_network_blocked", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardNetworkBlocked() = MyApplicationTheme(darkTheme = true) {
    Dashboard(DashboardUiState.Unreachable("blocked", "192.168.50.105", networkBlocked = true), status = null)
}

@PreviewTest
@Preview(name = "dashboard_reconnecting", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DashboardReconnecting() = MyApplicationTheme(darkTheme = true) {
    Dashboard(DashboardUiState.Live(TRACKING, "192.168.50.105", reconnecting = true))
}

private val FOUND = listOf(
    FoundHeliostat("84cca85ed290", "my_heliostat", "192.168.50.105", "0.1.0", "idle"),
    FoundHeliostat("a0b1c2d3e4f5", "garden", "192.168.50.131", "0.1.0", "track"),
)

@PreviewTest
@Preview(name = "find_searching", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FindSearching() = MyApplicationTheme(darkTheme = true) {
    FindContent(
        FindUiState(searching = true, remembered = Remembered("192.168.50.105", "84cca85ed290", "my_heliostat")),
        onPick = {}, onSearchAgain = {}, onOpenRemembered = {}, onSetUpNew = {}, onEnterAddress = {},
    )
}

@PreviewTest
@Preview(name = "find_several", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FindSeveral() = MyApplicationTheme(darkTheme = true) {
    FindContent(
        FindUiState(searching = false, found = FOUND, remembered = Remembered("192.168.50.105", "84cca85ed290", "my_heliostat")),
        onPick = {}, onSearchAgain = {}, onOpenRemembered = {}, onSetUpNew = {}, onEnterAddress = {},
    )
}

@PreviewTest
@Preview(name = "find_none", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun FindNone() = MyApplicationTheme(darkTheme = true) {
    FindContent(
        FindUiState(searching = false, remembered = Remembered("192.168.50.105", "84cca85ed290", "my_heliostat")),
        onPick = {}, onSearchAgain = {}, onOpenRemembered = {}, onSetUpNew = {}, onEnterAddress = {},
    )
}

@PreviewTest
@Preview(name = "scan_searching", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ScanSearching() = MyApplicationTheme(darkTheme = true) {
    DeviceScanContent(DeviceScanUiState.Scanning(emptyList()), onDeviceSelected = {}, onUseAddress = {})
}

@PreviewTest
@Preview(name = "scan_found", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ScanFound() = MyApplicationTheme(darkTheme = true) {
    DeviceScanContent(
        DeviceScanUiState.Scanning(listOf(DiscoveredDevice("84:CC:A8:5E:D2:92", "my_heliostat", -58))),
        onDeviceSelected = {}, onUseAddress = {},
    )
}

@PreviewTest
@Preview(name = "provision_5ghz", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Provision5GHz() = MyApplicationTheme(darkTheme = true) {
    val visible = listOf(
        VisibleNetwork("UniFi Wireless", 6, -76),
        VisibleNetwork("MACHADO_HOME", 1, -78),
        VisibleNetwork("OWNIT_87", 11, -90),
    )
    ProvisionContent(
        state = ProvisionUiState.Ready(
            phone = PhoneNetwork("MACHADO_HOME", 2412), visible = visible, scanning = false, scanError = null,
            locationSent = true, problems = emptyList(), manual = false, error = null,
        ),
        ssid = "MACHADO_HOME", password = "secret-password",
        onSsid = {}, onPassword = {}, onPick = {}, onRescan = {}, onManual = {}, onJoin = {}, onRetry = {},
        onDone = {}, onBack = {},
    )
}

@PreviewTest
@Preview(name = "provision_joined", widthDp = W, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProvisionJoined() = MyApplicationTheme(darkTheme = true) {
    ProvisionContent(
        state = ProvisionUiState.Joined("MACHADO_HOME", "192.168.50.77"), ssid = "MACHADO_HOME", password = "",
        onSsid = {}, onPassword = {}, onPick = {}, onRescan = {}, onManual = {}, onJoin = {}, onRetry = {},
        onDone = {}, onBack = {},
    )
}

@PreviewTest
@Preview(name = "jog", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Jog() = MyApplicationTheme(darkTheme = true) {
    JogContent(
        telemetry = TRACKING.copy(mode = "idle", intent = "idle"), step = 1.0, message = null,
        onStep = {}, onEnterManual = {}, onJog = { _, _ -> }, onDone = {}, bottomBar = SAFETY,
    )
}

@PreviewTest
@Preview(name = "target", widthDp = W, heightDp = H, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Target() = MyApplicationTheme(darkTheme = true) {
    TargetContent(
        current = StatusResponse.Target("azel", 180.0, 10.0), azimuth = "180.0", elevation = "10.0",
        message = null, onAzimuth = {}, onElevation = {}, onSave = {}, onCapture = {}, onJog = {}, onBack = {},
        bottomBar = SAFETY,
    )
}

@PreviewTest
@Preview(name = "permissions", widthDp = W, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Permissions() = MyApplicationTheme(darkTheme = true) {
    Explain(
        icon = Icons.Rounded.Bluetooth,
        title = "Permissions needed",
        body = "Bluetooth, to find the heliostat and hand it your WiFi details.\n\nLocation, because " +
            "Android only tells apps the name of the WiFi network you are on when location is allowed.",
        action = "Allow",
        onAction = {},
    )
}
