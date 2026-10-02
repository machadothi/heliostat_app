package com.machadothi.templateapp.wifi

import com.machadothi.templateapp.ble.VisibleNetwork

/**
 * The ESP32 has a 2.4 GHz radio only. A phone on a 5 GHz network can hand over
 * credentials the heliostat can never use, so the app detects that case and
 * suggests the router's 2.4 GHz sibling -- e.g. MACHADO_HOME for MACHADO_HOME_5G.
 */
object WifiBands {

    /** WifiInfo.frequency is in MHz; everything from 4.9 GHz up is not 2.4 GHz. */
    fun is24GHz(frequencyMhz: Int): Boolean = frequencyMhz in 2400..2500

    // Common ways routers mark the 5 GHz network's name.
    private val FIVE_GHZ_SUFFIX = Regex("""[\s_\-.]*(5\s*g(hz)?|5)$""", RegexOption.IGNORE_CASE)

    /** "MACHADO_HOME_5G" -> "MACHADO_HOME". Names without a 5 GHz marker are unchanged. */
    fun baseName(ssid: String): String = ssid.replace(FIVE_GHZ_SUFFIX, "")

    /**
     * The network among [visible] (the ESP32's own scan) most likely to be the
     * 2.4 GHz twin of the phone's 5 GHz [phoneSsid], or null if none matches.
     */
    fun suggest24GHzSibling(phoneSsid: String, visible: List<VisibleNetwork>): VisibleNetwork? {
        val base = baseName(phoneSsid)
        return visible
            .filter { network ->
                network.ssid.equals(base, ignoreCase = true) ||
                    network.ssid.equals("${base}_2G", ignoreCase = true) ||
                    network.ssid.equals("${base}-2G", ignoreCase = true) ||
                    network.ssid.equals("${base}_2.4G", ignoreCase = true) ||
                    network.ssid.equals("$base 2.4G", ignoreCase = true)
            }
            .maxByOrNull { it.rssi }
    }
}
