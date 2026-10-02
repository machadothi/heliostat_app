package com.machadothi.templateapp.ble

import java.util.UUID

/**
 * The heliostat's BLE GATT contract.
 *
 * Mirrors firmware/ble/gatt_service.py in the heliostat repo, and docs/protocol.md
 * there is the reference. Change one, change all three.
 */
object BleConstants {

    /** Advertised name. Configurable on the board as `ble.name`. */
    const val DEVICE_NAME = "my_heliostat"

    val SERVICE: UUID = uuid("1000")
    val INFO: UUID = uuid("1001")
    val SSID: UUID = uuid("1002")
    val PSK: UUID = uuid("1003")
    val COMMAND: UUID = uuid("1004")
    val WIFI_STATE: UUID = uuid("1005")
    val SCAN: UUID = uuid("1006")
    val TIME: UUID = uuid("1007")
    val LOCATION: UUID = uuid("1008")

    /** Client Characteristic Configuration descriptor: enables notifications. */
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val CMD_JOIN: Byte = 1
    const val CMD_SCAN: Byte = 2
    const val CMD_FORGET: Byte = 3

    /** MTU requested after connecting. The board asks for 247 as well. */
    const val DESIRED_MTU = 247

    private fun uuid(suffix: String): UUID =
        UUID.fromString("8a4f$suffix-5a10-4c6b-9e2a-68656c696f73")
}
