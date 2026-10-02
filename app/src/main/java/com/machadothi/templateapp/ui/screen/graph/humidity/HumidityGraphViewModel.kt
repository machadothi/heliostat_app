package com.machadothi.templateapp.ui.screen.graph.humidity

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
class HumidityGraphViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {

    var uiState by mutableStateOf<HumidityGraphUIState>(HumidityGraphUIState.Loading)
        private set

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            val result = repository.filterData(DataFilterRepoModel.Today)
            uiState = HumidityGraphUIState.Loaded(result.map { it.humidity })
        }
    }

}


sealed class HumidityGraphUIState {
    data object Loading : HumidityGraphUIState()
    data class Loaded(val humidity: List<Double>) : HumidityGraphUIState()
}