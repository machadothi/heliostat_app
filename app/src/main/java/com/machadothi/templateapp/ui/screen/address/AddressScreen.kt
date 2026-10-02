package com.machadothi.templateapp.ui.screen.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import com.machadothi.templateapp.ui.component.HeliostatTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Point the app at an address by hand: a heliostat already on WiFi, or the
 * laptop mock server (tools/mock_server.py, e.g. 192.168.50.132:8080).
 */
@Composable
fun AddressScreen(onDone: () -> Unit, onBack: () -> Unit, viewModel: AddressViewModel = hiltViewModel()) {
    AddressContent(
        address = viewModel.address,
        onAddress = { viewModel.address = it },
        onSave = { viewModel.save(onDone) },
        onBack = onBack,
    )
}

@Composable
fun AddressContent(address: String, onAddress: (String) -> Unit, onSave: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { HeliostatTopBar("Heliostat address", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Use the IP shown when it joined WiFi, or heliostat.local. For the laptop mock " +
                    "server, use the laptop's IP and port.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = address,
                onValueChange = onAddress,
                label = { Text("IP or IP:port") },
                placeholder = { Text("192.168.50.77") },
                leadingIcon = { Icon(Icons.Rounded.Router, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = onSave,
                enabled = address.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) { Text("Connect") }
        }
    }
}

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var address by mutableStateOf("")

    init {
        viewModelScope.launch { address = repository.host().orEmpty() }
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.useHost(address.trim().removePrefix("http://").trimEnd('/'))
            onDone()
        }
    }
}
