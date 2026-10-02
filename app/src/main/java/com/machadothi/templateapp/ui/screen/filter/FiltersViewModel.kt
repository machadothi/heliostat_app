package com.machadothi.templateapp.ui.screen.filter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.machadothi.templateapp.data.network.DataService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val dataService: DataService
) : ViewModel() {

    var uiState by mutableStateOf<FiltersUIState>(FiltersUIState.Loading)

    init {
        viewModelScope.launch {
            fetchAvailableDates()
        }
    }

    fun fetchAvailableDates() {
        viewModelScope.launch {
            uiState = FiltersUIState.Loaded(dataService.availableDates())
        }
    }

}

sealed class FiltersUIState {
    data object Loading : FiltersUIState()
    data class Loaded(
        val dates: List<String>
    ) : FiltersUIState()
}