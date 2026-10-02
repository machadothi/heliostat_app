package com.machadothi.templateapp.ui.screen.target

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.StatusResponse
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.MetricTile
import com.machadothi.templateapp.ui.component.SafetyBar
import com.machadothi.templateapp.ui.component.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where the reflected beam should land.
 *
 * Two ways to set it: type the direction (azimuth from true north, elevation
 * above the horizon), or -- far easier in practice -- jog the mirror until the
 * spot sits where you want it and tap "Capture". The heliostat works the target
 * out from the sun's position and the mirror's current angle.
 */
@Composable
fun TargetScreen(onJog: () -> Unit, onBack: () -> Unit, viewModel: TargetViewModel = hiltViewModel()) {
    TargetContent(
        current = viewModel.current,
        azimuth = viewModel.azimuth,
        elevation = viewModel.elevation,
        message = viewModel.message,
        onAzimuth = { viewModel.azimuth = it },
        onElevation = { viewModel.elevation = it },
        onSave = viewModel::save,
        onCapture = viewModel::capture,
        onJog = onJog,
        onBack = onBack,
    )
}

@Composable
fun TargetContent(
    current: StatusResponse.Target?,
    azimuth: String,
    elevation: String,
    message: String?,
    onAzimuth: (String) -> Unit,
    onElevation: (String) -> Unit,
    onSave: () -> Unit,
    onCapture: () -> Unit,
    onJog: () -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = { SafetyBar() },
) {
    Scaffold(
        topBar = { HeliostatTopBar("Target", subtitle = "Where the beam should land", onBack = onBack) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("Target azimuth", current?.let { "%.1f".format(it.az) } ?: "—", Modifier.weight(1f), unit = "°")
                MetricTile("Target elevation", current?.let { "%.1f".format(it.el) } ?: "—", Modifier.weight(1f), unit = "°")
            }

            SectionCard(title = "Capture current aim") {
                Text(
                    "The easy way. Jog the mirror until the bright spot lands exactly where you " +
                        "want it, then capture. Needs the sun up and shining on the mirror.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onJog, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.OpenWith, null, Modifier.size(18.dp))
                        Text("  Jog")
                    }
                    Button(onClick = onCapture, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CenterFocusStrong, null, Modifier.size(18.dp))
                        Text("  Capture")
                    }
                }
            }

            SectionCard(title = "Or type the direction") {
                OutlinedTextField(
                    value = azimuth,
                    onValueChange = onAzimuth,
                    label = { Text("Azimuth") },
                    supportingText = { Text("Degrees clockwise from true north") },
                    suffix = { Text("°") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = elevation,
                    onValueChange = onElevation,
                    label = { Text("Elevation") },
                    supportingText = { Text("Degrees above the horizon") },
                    suffix = { Text("°") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save target") }
            }

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@HiltViewModel
class TargetViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var azimuth by mutableStateOf("")
    var elevation by mutableStateOf("")
    var current by mutableStateOf<StatusResponse.Target?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            repository.status().onSuccess { status ->
                status.target?.let { show(it) }
            }
        }
    }

    fun save() {
        val az = azimuth.replace(',', '.').toDoubleOrNull()
        val el = elevation.replace(',', '.').toDoubleOrNull()
        if (az == null || el == null) {
            message = "Enter numbers for both angles"
            return
        }
        viewModelScope.launch {
            message = repository.setTarget(az, el).fold({ show(it); "Target saved" }, { it.message })
        }
    }

    fun capture() {
        viewModelScope.launch {
            message = repository.captureTarget().fold({ show(it); "Captured the current aim" }, { it.message })
        }
    }

    private fun show(target: StatusResponse.Target) {
        current = target
        azimuth = "%.1f".format(target.az)
        elevation = "%.1f".format(target.el)
    }
}
