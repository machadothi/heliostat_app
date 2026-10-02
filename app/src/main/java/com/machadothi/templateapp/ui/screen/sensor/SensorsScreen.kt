package com.machadothi.templateapp.ui.screen.sensor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SensorsScreen(
    viewModel: SensorsViewModel = hiltViewModel(),
    onFiltersSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (val uiState = viewModel.uiState) {
            SensorsUIState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            is SensorsUIState.Loaded -> {
                Button(
                    onClick = {
                        onFiltersSelected()
                    },
                    content = {
                        Text(text = "Graphs")
                    }
                )
                Text(text = "ECO2: ${uiState.eco2}")
                Text(text = "Humidity: ${uiState.humidity}")
                Text(text = "Temp: ${uiState.temperature}")
                Text(text = "TVOC: ${uiState.tvoc}")
            }
        }
    }

}