package com.machadothi.templateapp.ble

import com.machadothi.templateapp.wifi.WifiBands
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Every expected byte string below is copied from the heliostat repo's
 * docs/protocol.md, which was generated from the firmware's own struct layouts.
 * If one of these fails, the app and the board disagree on the wire.
 */
class BleCodecTest {

    private fun hex(s: String) = s.split(" ").filter { it.isNotEmpty() }
        .map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `short value is one framed chunk`() {
        val chunks = BleCodec.frame("MACHADO_HOME", maxWrite = 20)
        assertEquals(1, chunks.size)
        assertArrayEquals(hex("80 4D 41 43 48 41 44 4F 5F 48 4F 4D 45"), chunks[0])
    }

    @Test
    fun `long value splits with the final bit on the last chunk`() {
        // docs/protocol.md example, written with 18 data bytes per chunk.
        val chunks = BleCodec.frame("a-long-password-over-18-bytes", maxWrite = 19)
        assertEquals(2, chunks.size)
        assertArrayEquals(
            hex("00 61 2D 6C 6F 6E 67 2D 70 61 73 73 77 6F 72 64 2D 6F 76"), chunks[0]
        )
        assertArrayEquals(hex("81 65 72 2D 31 38 2D 62 79 74 65 73"), chunks[1])
    }

    @Test
    fun `every chunk fits the write size`() {
        val chunks = BleCodec.frame("x".repeat(64), maxWrite = 20)
        chunks.forEach { assert(it.size <= 20) }
        assertEquals("x".repeat(64), chunks.joinToString("") { String(it, 1, it.size - 1) })
    }

    @Test
    fun `empty password is a single final chunk`() {
        val chunks = BleCodec.frame("", maxWrite = 20)
        assertArrayEquals(hex("80"), chunks.single())
    }

    @Test
    fun `wifi state joined carries the ip`() {
        val state = BleCodec.decodeWifiState(hex("02 00 C0 A8 32 4D"))
        assertEquals(WifiState.Phase.JOINED, state.phase)
        assertEquals("192.168.50.77", state.ip)
    }

    @Test
    fun `wifi state failed carries the reason and no ip`() {
        val state = BleCodec.decodeWifiState(hex("03 01 00 00 00 00"))
        assertEquals(WifiState.Phase.FAILED, state.phase)
        assertEquals(WifiState.Reason.WRONG_PASSWORD, state.reason)
        assertNull(state.ip)
    }

    @Test
    fun `time matches the documented example`() {
        // 2026-09-29 16:40:00 UTC, UTC+2
        assertArrayEquals(hex("E0 E9 BB 6A 78 00"), BleCodec.encodeTime(1790700000, 120))
    }

    @Test
    fun `location matches the documented example`() {
        assertArrayEquals(
            hex("A3 E3 1A 42 93 3A 12 C1 00 00 48 42"),
            BleCodec.encodeLocation(38.7223, -9.1393, 50.0)
        )
    }

    @Test
    fun `scan text parses and skips junk lines`() {
        val networks = BleCodec.parseScan("MACHADO_HOME\t1\t-78\nUniFi Wireless\t6\t-76\nbroken\n")
        assertEquals(
            listOf(VisibleNetwork("MACHADO_HOME", 1, -78), VisibleNetwork("UniFi Wireless", 6, -76)),
            networks
        )
    }

    @Test
    fun `5 GHz network suggests its 2_4 GHz sibling`() {
        val seen = listOf(
            VisibleNetwork("UniFi Wireless", 6, -76),
            VisibleNetwork("MACHADO_HOME", 1, -78),
        )
        assertEquals("MACHADO_HOME", WifiBands.suggest24GHzSibling("MACHADO_HOME_5G", seen)?.ssid)
        assertEquals("Casa", WifiBands.baseName("Casa-5GHz"))
        assertEquals("Casa", WifiBands.baseName("Casa 5G"))
        assertNull(WifiBands.suggest24GHzSibling("Other_5G", seen))
    }

    @Test
    fun `band detection`() {
        assert(WifiBands.is24GHz(2412))
        assert(!WifiBands.is24GHz(5240))
    }
}
