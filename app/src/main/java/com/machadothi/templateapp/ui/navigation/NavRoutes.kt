package com.machadothi.templateapp.ui.navigation

import kotlinx.serialization.Serializable

object NavRoutes {

    // --- heliostat -------------------------------------------------------------------

    /**
     * Finds heliostats on the WiFi network. [auto]: connect without asking to the
     * one this phone used last, or to the only one there (the app's start).
     */
    @Serializable
    data class Find(val auto: Boolean = true)

    @Serializable
    data object DeviceScan

    /** BLE provisioning of the device at [address]. */
    @Serializable
    data class Provision(val address: String)

    @Serializable
    data object Address

    @Serializable
    data object Dashboard

    @Serializable
    data object Jog

    @Serializable
    data object Target

    // --- the original sensor demo, kept as a reference for the conventions -------------

    @Serializable
    data object Sensors

    @Serializable
    data object Filter

    @Serializable
    data object Graph {

        @Serializable
        data object Temperature

        @Serializable
        data object Humidity

    }
}
