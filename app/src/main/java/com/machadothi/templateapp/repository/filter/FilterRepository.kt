package com.machadothi.templateapp.repository.filter

interface FilterRepository {
    suspend fun fetchFilters(): List<String>
}