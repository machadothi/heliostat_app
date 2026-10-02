package com.machadothi.templateapp.repository.data

import com.machadothi.templateapp.repository.data.model.DataFilterRepoModel

interface DataRepository {
    suspend fun filterData(model: DataFilterRepoModel): List<DataRepoModel>
}