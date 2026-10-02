package com.machadothi.templateapp.ble

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Byte layouts for the heliostat's GATT characteristics. Pure Kotlin, no Android,
 * so every layout is unit-tested against the examples in docs/protocol.md.
 */
object BleCodec {

    /** Set on the sequence byte of the LAST chunk of an SSID or password. */
    private const val SEQ_FINAL = 0x80

    /** The firmware accepts sequence numbers 0x00..0x0F: at most 16 chunks. */
    private const val MAX_CHUNKS = 16

    /**
     * Split an SSID or password into framed chunks: `[seq][data...]`, with bit 7
     * set on the last one. The framed form works at any MTU, which is why the
     * app always uses it (the firmware also accepts plain text, for nRF Connect).
     *
     * @param maxWrite the largest write the link allows: negotiated MTU - 3.
     */
    fun frame(value: String, maxWrite: Int): List<ByteArray> {
        val data = value.toByteArray(Charsets.UTF_8)
        val perChunk = maxWrite - 1
        require(perChunk > 0) { "MTU too small to write anything" }
        val pieces = if (data.isEmpty()) listOf(ByteArray(0)) else data.toList().chunked(perChunk)
            .map { it.toByteArray() }
        require(pieces.size <= MAX_CHUNKS) { "value too long: ${data.size} bytes" }
        return pieces.mapIndexed { index, piece ->
            val seq = index or (if (index == pieces.lastIndex) SEQ_FINAL else 0)
            byteArrayOf(seq.toByte()) + piece
        }
    }

    /** Decode the 6-byte WIFI_STATE value: state, reason, IPv4. */
    fun decodeWifiState(bytes: ByteArray): WifiState {
        require(bytes.size >= 6) { "WIFI_STATE is 6 bytes, got ${bytes.size}" }
        val state = WifiState.Phase.fromCode(bytes[0].toInt() and 0xFF)
        val reason = WifiState.Reason.fromCode(bytes[1].toInt() and 0xFF)
        val ip = (2..5).joinToString(".") { (bytes[it].toInt() and 0xFF).toString() }
        return WifiState(state, reason, ip.takeIf { state == WifiState.Phase.JOINED })
    }

    /** TIME: u32 Unix seconds, i16 timezone offset in minutes, little-endian. */
    fun encodeTime(unixSeconds: Long, tzOffsetMinutes: Int): ByteArray =
        ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(unixSeconds.toInt())
            .putShort(tzOffsetMinutes.toShort())
            .array()

    /** LOCATION: f32 latitude, f32 longitude, f32 elevation (m), little-endian. */
    fun encodeLocation(lat: Double, lon: Double, elevationM: Double): ByteArray =
        ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .putFloat(lat.toFloat())
            .putFloat(lon.toFloat())
            .putFloat(elevationM.toFloat())
            .array()

    /** SCAN: one network per line, "ssid<TAB>channel<TAB>rssi", strongest first. */
    fun parseScan(text: String): List<VisibleNetwork> =
        text.lineSequence()
            .mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size != 3) return@mapNotNull null
                val channel = fields[1].toIntOrNull() ?: return@mapNotNull null
                val rssi = fields[2].toIntOrNull() ?: return@mapNotNull null
                VisibleNetwork(fields[0], channel, rssi)
            }
            .toList()
}

data class WifiState(val phase: Phase, val reason: Reason, val ip: String?) {

    enum class Phase(val code: Int) {
        IDLE(0), JOINING(1), JOINED(2), FAILED(3), SCANNING(4), SCAN_READY(5), UNKNOWN(-1);

        companion object {
            fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: UNKNOWN
        }
    }

    enum class Reason(val code: Int, val message: String) {
        NONE(0, ""),
        WRONG_PASSWORD(1, "Wrong password"),
        NOT_FOUND(2, "Network not found. The heliostat only sees 2.4 GHz networks"),
        TIMEOUT(3, "Timed out joining the network (no DHCP address?)"),
        OTHER(4, "Could not join the network"),
        UNKNOWN(-1, "Unknown error");

        companion object {
            fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: UNKNOWN
        }
    }
}

/** A WiFi network as seen by the ESP32. It only ever sees 2.4 GHz ones. */
data class VisibleNetwork(val ssid: String, val channel: Int, val rssi: Int)
