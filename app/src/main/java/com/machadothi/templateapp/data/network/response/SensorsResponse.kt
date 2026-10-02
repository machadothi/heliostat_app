package com.machadothi.templateapp.data.network.response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SensorsResponse(
    @SerialName("eco2")
    val eco2: Int,
    @SerialName("humidity")
    val humidity: Double,
    @SerialName("temperature")
    val temperature: Double,
    @SerialName("tvoc")
    val tvoc: Int
)