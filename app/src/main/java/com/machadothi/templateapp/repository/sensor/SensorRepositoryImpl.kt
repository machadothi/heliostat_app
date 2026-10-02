package com.machadothi.templateapp.repository.sensor

import com.machadothi.templateapp.data.network.DataService
import com.machadothi.templateapp.data.network.response.SensorsResponse
import javax.inject.Inject

class SensorRepositoryImpl @Inject constructor(
    private val service: DataService
) : SensorRepository {

    override suspend fun fetchSensorData(): SensorsResponse {
        return service.readAllSensors()
    }


}