package com.machadothi.templateapp.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.machadothi.templateapp.ui.theme.StopRed
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.heliostat.HeliostatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DEFOCUS, STOW and E-STOP, reachable from every heliostat screen.
 *
 * These are the controls someone reaches for when the beam is somewhere it should
 * not be, so they are never more than one tap away, whatever screen is open.
 * The software E-stop latches and releases torque; the physical E-stop on the
 * machine additionally cuts servo power in hardware.
 */
@Composable
fun SafetyBar(viewModel: SafetyViewModel = hiltViewModel()) {
    SafetyBarContent(message = viewModel.message, onMode = viewModel::set)
}

@Composable
fun SafetyBarContent(message: String?, onMode: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 3.dp) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SafetyButton("Defocus", Icons.Rounded.CenterFocusWeak, Modifier.weight(1f)) { onMode("defocus") }
                SafetyButton("Stow", Icons.Rounded.VerticalAlignBottom, Modifier.weight(1f)) { onMode("stow") }
                Button(
                    onClick = { onMode("estop") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = StopRed, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Rounded.PanTool, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" STOP", fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SafetyButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" $label", maxLines = 1)
        }
    }
}

@HiltViewModel
class SafetyViewModel @Inject constructor(
    private val repository: HeliostatRepository,
) : ViewModel() {

    var message by mutableStateOf<String?>(null)
        private set

    fun set(mode: String) {
        viewModelScope.launch {
            message = repository.setMode(mode).exceptionOrNull()?.message
        }
    }
}
