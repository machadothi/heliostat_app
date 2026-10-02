package com.machadothi.templateapp.ui.screen.sensor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.sensor.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorsViewModel @Inject constructor(
    private val repository: SensorRepository
) : ViewModel() {

    var uiState by mutableStateOf<SensorsUIState>(SensorsUIState.Loading)
        private set

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            val result = repository.fetchSensorData()
            uiState = SensorsUIState.Loaded(
                eco2 = result.eco2,
                humidity = result.humidity,
                temperature = result.temperature,
                tvoc = result.tvoc
            )
        }
    }
}

sealed class SensorsUIState {
    data object Loading : SensorsUIState()
    data class Loaded(
        val eco2: Int,
        val humidity: Double,
        val temperature: Double,
        val tvoc: Int,
    ) : SensorsUIState()
}