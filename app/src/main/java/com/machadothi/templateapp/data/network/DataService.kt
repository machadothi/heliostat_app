package com.machadothi.templateapp.data.network

import com.machadothi.templateapp.data.network.response.SensorsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DataService {

    @GET("/api/read_all_sensors")
    suspend fun readAllSensors(): SensorsResponse

    @GET("/api/data/dates")
    suspend fun availableDates(): List<String>

    @GET("/api/data/{filter}")
    suspend fun fetchData(@Path("filter") filter: String): List<List<String>>
}