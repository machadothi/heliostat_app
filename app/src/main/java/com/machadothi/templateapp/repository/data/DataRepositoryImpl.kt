package com.machadothi.templateapp.repository.data

import com.machadothi.templateapp.data.network.DataService
import com.machadothi.templateapp.repository.data.model.DataFilterRepoModel
import javax.inject.Inject

class DataRepositoryImpl @Inject constructor(
    private val dataService: DataService
) : DataRepository {

    override suspend fun filterData(
        model: DataFilterRepoModel
    ): List<DataRepoModel> {
        return when (model) {
            is DataFilterRepoModel.Custom -> {
                dataService.fetchData(model.filter).map { data ->
                    DataRepoModel(
                        id = data[0].toInt(),
                        timestamp = data[1],
                        dunno1 = data[2].toInt(),
                        dunno2 = data[3].toInt(),
                        temp = data[4].toDouble(),
                        humidity = data[5].toDouble(),
                        dunno4 = data[6].toDouble()
                    )
                }
            }

            is DataFilterRepoModel.Today -> {
                dataService.fetchData("today").map { data ->
                    DataRepoModel(
                        id = data[0].toInt(),
                        timestamp = data[1],
                        dunno1 = data[2].toInt(),
                        dunno2 = data[3].toInt(),
                        temp = data[4].toDouble(),
                        humidity = data[5].toDouble(),
                        dunno4 = data[6].toDouble()
                    )
                }
            }
        }
    }
}

data class DataRepoModel(
    val id: Int,
    val timestamp: String,
    val dunno1: Int,
    val dunno2: Int,
    val temp: Double,
    val humidity: Double,
    val dunno4: Double
)