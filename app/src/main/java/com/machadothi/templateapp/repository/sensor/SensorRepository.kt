package com.machadothi.templateapp.repository.sensor

import com.machadothi.templateapp.data.network.response.SensorsResponse

interface SensorRepository {
    suspend fun fetchSensorData(): SensorsResponse
}