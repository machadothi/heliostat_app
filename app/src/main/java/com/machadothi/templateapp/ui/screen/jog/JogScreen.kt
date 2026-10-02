package com.machadothi.templateapp.ui.screen.jog

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.TelemetryResponse
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.ModeBadge
import com.machadothi.templateapp.ui.component.SafetyBar
import com.machadothi.templateapp.ui.component.SectionCard
import com.machadothi.templateapp.ui.theme.Numeric
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private val STEPS = listOf(0.1, 1.0, 5.0, 10.0)

/**
 * Move each axis by hand. Needs MANUAL mode, which suspends automatic tracking.
 * The heliostat refuses a jog that would put the beam into a keep-out zone.
 */
@Composable
fun JogScreen(onDone: () -> Unit, viewModel: JogViewModel = hiltViewModel()) {
    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }
    JogContent(
        telemetry = viewModel.telemetry,
        step = viewModel.step,
        message = viewModel.message,
        onStep = { viewModel.step = it },
        onEnterManual = viewModel::enterManual,
        onJog = viewModel::jog,
        onDone = { viewModel.done(onDone) },
    )
}

@Composable
fun JogContent(
    telemetry: TelemetryResponse?,
    step: Double,
    message: String?,
    onStep: (Double) -> Unit,
    onEnterManual: () -> Unit,
    onJog: (axis: Int, direction: Int) -> Unit,
    onDone: () -> Unit,
    bottomBar: @Composable () -> Unit = { SafetyBar() },
) {
    val manual = telemetry?.mode == "manual"
    Scaffold(
        topBar = { HeliostatTopBar("Jog", subtitle = "Move the mirror by hand", onBack = onDone) },
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
            telemetry?.let { ModeBadge(it.mode) }
            if (!manual) {
                Banner(
                    title = "Manual mode needed",
                    text = "Jogging pauses tracking. The heliostat goes to idle first, then manual.",
                    kind = BannerKind.Warning,
                    actions = { Button(onClick = onEnterManual) { Text("Enter manual mode") } },
                )
            }

            SectionCard(title = "Step") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    STEPS.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = step == value,
                            onClick = { onStep(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, STEPS.size),
                            icon = {},
                        ) { Text(if (value < 1) "$value°" else "${value.toInt()}°") }
                    }
                }
            }

            AxisPad("Azimuth", "degrees from north", telemetry?.pos?.getOrNull(0), manual,
                { onJog(0, -1) }, { onJog(0, +1) })
            AxisPad("Elevation", "degrees above horizon", telemetry?.pos?.getOrNull(1), manual,
                { onJog(1, -1) }, { onJog(1, +1) })

            message?.let { Banner(it, BannerKind.Error) }

            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done — back to idle") }
        }
    }
}

@Composable
private fun AxisPad(
    name: String,
    hint: String,
    position: Double?,
    enabled: Boolean,
    minus: () -> Unit,
    plus: () -> Unit,
) {
    SectionCard(title = null) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundButton(Icons.Rounded.Remove, "$name minus", enabled, minus)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    position?.let { "%.1f°".format(it) } ?: "—",
                    style = MaterialTheme.typography.displaySmall.merge(Numeric),
                )
                Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RoundButton(Icons.Rounded.Add, "$name plus", enabled, plus)
        }
    }
}

@Composable
private fun RoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(64.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) { Icon(icon, contentDescription = description, modifier = Modifier.size(32.dp)) }
}

@HiltViewModel
class JogViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var telemetry by mutableStateOf<TelemetryResponse?>(null)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var step by mutableDoubleStateOf(1.0)

    private var polling: Job? = null

    fun startPolling() {
        if (polling?.isActive == true) return
        polling = viewModelScope.launch {
            repository.telemetry(periodMs = 300).collect { result ->
                result.onSuccess { telemetry = it }
            }
        }
    }

    fun stopPolling() {
        polling?.cancel()
        polling = null
    }

    /**
     * The heliostat only allows MANUAL from IDLE: an operator stops tracking
     * deliberately before taking the axes by hand. So from any other mode, go
     * through IDLE first.
     */
    fun enterManual() {
        viewModelScope.launch {
            if (telemetry?.mode != "idle") {
                repository.setMode("idle").onFailure {
                    message = it.message
                    return@launch
                }
            }
            message = repository.setMode("manual").exceptionOrNull()?.message
        }
    }

    fun jog(axis: Int, direction: Int) {
        viewModelScope.launch {
            message = repository.jog(axis, direction * step).exceptionOrNull()?.message
        }
    }

    fun done(onDone: () -> Unit) {
        viewModelScope.launch {
            if (telemetry?.mode == "manual") repository.setMode("idle")
            onDone()
        }
    }
}
