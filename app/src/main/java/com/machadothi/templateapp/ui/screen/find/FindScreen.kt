package com.machadothi.templateapp.ui.screen.find

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.machadothi.templateapp.data.local.Remembered
import com.machadothi.templateapp.discovery.FoundHeliostat
import com.machadothi.templateapp.discovery.HeliostatDiscovery
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.Banner
import com.machadothi.templateapp.ui.component.BannerKind
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import com.machadothi.templateapp.ui.component.ModeBadge
import com.machadothi.templateapp.ui.navigation.NavRoutes
import com.machadothi.templateapp.ui.screen.devicescan.Radar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where the app opens: finds heliostats on the WiFi network.
 *
 * With [NavRoutes.Find.auto], it connects straight away to the heliostat this
 * phone used last -- matched by its stable id, so a new DHCP address does not
 * matter -- or, if none was ever chosen, to the only one on the network. Several,
 * or not the remembered one: the user picks. Bluetooth is only for setting up a
 * NEW heliostat.
 */
@Composable
fun FindScreen(
    onConnected: () -> Unit,
    onSetUpNew: () -> Unit,
    onEnterAddress: () -> Unit,
    onBack: (() -> Unit)?,
    viewModel: FindViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel.connected) {
        if (viewModel.connected) onConnected()
    }
    FindContent(
        state = viewModel.uiState,
        onPick = viewModel::choose,
        onSearchAgain = viewModel::search,
        onOpenRemembered = viewModel::openRemembered,
        onSetUpNew = onSetUpNew,
        onEnterAddress = onEnterAddress,
        onBack = onBack,
    )
}

@Composable
fun FindContent(
    state: FindUiState,
    onPick: (FoundHeliostat) -> Unit,
    onSearchAgain: () -> Unit,
    onOpenRemembered: () -> Unit,
    onSetUpNew: () -> Unit,
    onEnterAddress: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = { HeliostatTopBar(title = "Your heliostats", subtitle = "On this WiFi network", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val searching = state.searching
            if (searching || state.found.isEmpty()) {
                Radar(Modifier.fillMaxWidth().padding(vertical = 20.dp), compact = state.found.isNotEmpty())
            }
            when {
                searching && state.found.isEmpty() -> Text(
                    state.remembered?.name?.let { "Looking for $it…" } ?: "Looking for heliostats…",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.found.isNotEmpty() -> {
                    Text(
                        if (state.found.size == 1) "Found one heliostat" else "Found ${state.found.size} heliostats — pick one",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.found.forEach { HeliostatCard(it, remembered = it.id == state.remembered?.id) { onPick(it) } }
                    state.remembered?.let { saved ->
                        if (state.found.none { it.id == saved.id || (saved.id == null && it.host == saved.host) }) {
                            Text(
                                "${saved.name ?: "The heliostat you used last"} didn't answer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> Banner(
                    title = "No heliostat answered",
                    text = (state.error?.let { "$it\n\n" } ?: "") +
                        "Is the phone on the same WiFi as the heliostat, and is it powered? " +
                        "A heliostat that has never been set up isn't on WiFi yet — set it up over Bluetooth.",
                    kind = BannerKind.Warning,
                    actions = state.remembered?.let { saved ->
                        { OutlinedButton(onClick = onOpenRemembered) { Text("Open ${saved.name ?: saved.host} anyway") } }
                    },
                )
            }

            if (!searching) {
                OutlinedButton(onClick = onSearchAgain, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  Search again")
                }
            }
            Button(onClick = onSetUpNew, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Set up a new heliostat")
            }
            TextButton(onClick = onEnterAddress, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("  Enter an address instead")
            }
        }
    }
}

@Composable
private fun HeliostatCard(heliostat: FoundHeliostat, remembered: Boolean, onClick: () -> Unit) {
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
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(heliostat.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOfNotNull(heliostat.host, heliostat.fw?.let { "fw $it" }, if (remembered) "last used" else null)
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            heliostat.mode?.let { ModeBadge(it) }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Connect")
        }
    }
}

data class FindUiState(
    val searching: Boolean = true,
    val found: List<FoundHeliostat> = emptyList(),
    val remembered: Remembered? = null,
    val error: String? = null,
)

@HiltViewModel
class FindViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val discovery: HeliostatDiscovery,
    private val repository: HeliostatRepository,
) : ViewModel() {

    /** Connect without asking when the answer is obvious (app launch), not when the user asked to switch. */
    private val auto = savedStateHandle.toRoute<NavRoutes.Find>().auto

    var uiState by mutableStateOf(FindUiState())
        private set

    /** Set once a heliostat is chosen; the screen then navigates to the dashboard. */
    var connected by mutableStateOf(false)
        private set

    init {
        search()
    }

    fun search() {
        viewModelScope.launch {
            val remembered = repository.remembered()
            uiState = FindUiState(searching = true, remembered = remembered)
            val isRemembered = { h: FoundHeliostat -> remembered != null && remembers(remembered, h) }
            val result = discovery.discover(
                alsoProbe = listOfNotNull(remembered?.host),
                // On launch, stop the moment the known heliostat answers.
                stopWhen = { auto && isRemembered(it) },
            )
            val found = result.getOrDefault(emptyList()).sortedBy { it.name }
            uiState = uiState.copy(searching = false, found = found, error = result.exceptionOrNull()?.message)
            if (!auto) return@launch
            val pick = found.firstOrNull(isRemembered) ?: found.singleOrNull()?.takeIf { remembered?.id == null }
            pick?.let(::choose)
        }
    }

    fun choose(heliostat: FoundHeliostat) {
        viewModelScope.launch {
            repository.choose(heliostat)
            connected = true
        }
    }

    /** Nothing answered, but the user wants the dashboard for the saved address anyway. */
    fun openRemembered() {
        connected = true
    }

    private fun remembers(saved: Remembered, h: FoundHeliostat) =
        if (saved.id != null) h.id == saved.id else h.host == saved.host
}
