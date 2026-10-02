package com.machadothi.templateapp.ui.screen.graph.temperature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.repository.data.DataRepository
import com.machadothi.templateapp.repository.data.model.DataFilterRepoModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemperatureGraphViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {

    var uiState by mutableStateOf<TemperatureGraphUIState>(TemperatureGraphUIState.Loading)
        private set

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            val result = repository.filterData(DataFilterRepoModel.Today)
            uiState = TemperatureGraphUIState.Loaded(result.map { it.temp })
        }
    }
}

sealed class TemperatureGraphUIState {
    data object Loading : TemperatureGraphUIState()
    data class Loaded(val temperatures: List<Double>) : TemperatureGraphUIState()
}