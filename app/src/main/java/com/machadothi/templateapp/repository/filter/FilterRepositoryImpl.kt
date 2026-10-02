package com.machadothi.templateapp.repository.filter

import com.machadothi.templateapp.data.network.DataService
import javax.inject.Inject

class FilterRepositoryImpl @Inject constructor(
    private val dataService: DataService
) : FilterRepository {
    override suspend fun fetchFilters(): List<String> {
        return dataService.availableDates()
    }
}