package com.machadothi.templateapp.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HeliostatDiscovery"

/** A heliostat that answered on the local network. */
data class FoundHeliostat(
    /** Stable, from the board's MAC: how the app recognises a heliostat it knows. */
    val id: String,
    val name: String,
    /** "192.168.50.105", or "ip:port" when the API is not on port 80 (mock server). */
    val host: String,
    val fw: String?,
    val mode: String?,
)

/**
 * Finds heliostats on the phone's WiFi network: no Bluetooth, no typed address,
 * and it still works after DHCP gives a board a new IP.
 *
 * Each board answers the UDP probe "HELIOSTAT_DISCOVER 1" on port 47474 with a
 * JSON description of itself (docs/protocol.md section 3 in the heliostat repo).
 * The probe goes to the subnet broadcast address and 255.255.255.255; because
 * some access points filter broadcasts between WiFi clients, it is also sent
 * unicast to every address in the subnet -- a /24 is 254 tiny datagrams.
 */
@Singleton
class HeliostatDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    /**
     * Probe and collect answers for up to [timeoutMs]. Returns early as soon as
     * [stopWhen] accepts one -- finding the remembered heliostat takes a round
     * trip, not the whole window. [alsoProbe] adds hosts outside the subnet sweep
     * (a saved address).
     */
    suspend fun discover(
        timeoutMs: Long = 2_500,
        alsoProbe: List<String> = emptyList(),
        stopWhen: (FoundHeliostat) -> Boolean = { false },
    ): Result<List<FoundHeliostat>> = withContext(Dispatchers.IO) {
        runCatching {
            val wifi = wifiNetwork() ?: error("The phone isn't on a WiFi network")
            val subnet = subnetOf(wifi) ?: error("Couldn't read the WiFi network's address")
            val found = LinkedHashMap<String, FoundHeliostat>()
            DatagramSocket().use { socket ->
                // The probes must leave over WiFi even if Android prefers mobile
                // data for the app's default traffic.
                wifi.bindSocket(socket)
                socket.broadcast = true
                val targets = buildList {
                    add(subnet.broadcast)
                    add(InetAddress.getByName("255.255.255.255"))
                    alsoProbe.mapNotNull { runCatching { InetAddress.getByName(it.substringBefore(':')) }.getOrNull() }
                        .forEach { add(it) }
                    addAll(subnet.hosts())
                }
                val deadline = System.currentTimeMillis() + timeoutMs
                // Two rounds: a single lost datagram must not hide a board.
                var nextRound = 0L
                var rounds = 0
                val buffer = ByteArray(512)
                while (System.currentTimeMillis() < deadline) {
                    if (rounds < 2 && System.currentTimeMillis() >= nextRound) {
                        targets.forEach { send(socket, it) }
                        rounds++
                        nextRound = System.currentTimeMillis() + 800
                    }
                    socket.soTimeout = 150
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val heliostat = parse(packet) ?: continue
                    if (found.put(heliostat.id, heliostat) == null) {
                        Log.d(TAG, "found ${heliostat.name} (${heliostat.id}) at ${heliostat.host}")
                    }
                    if (stopWhen(heliostat)) break
                }
            }
            found.values.toList()
        }.onFailure { Log.d(TAG, "discovery failed: $it") }
    }

    private fun send(socket: DatagramSocket, to: InetAddress) {
        try {
            socket.send(DatagramPacket(PROBE, PROBE.size, to, PORT))
        } catch (e: Exception) {
            // One unroutable address (the broadcast on some networks) must not end the search.
            Log.v(TAG, "probe to $to failed: $e")
        }
    }

    private fun parse(packet: DatagramPacket): FoundHeliostat? {
        val reply = runCatching {
            json.decodeFromString<Reply>(String(packet.data, 0, packet.length, Charsets.UTF_8))
        }.getOrNull() ?: return null
        if (reply.service != "heliostat") return null
        val ip = packet.address.hostAddress ?: return null
        return FoundHeliostat(
            id = reply.id,
            name = reply.name,
            host = if (reply.port == 80) ip else "$ip:${reply.port}",
            fw = reply.fw,
            mode = reply.mode,
        )
    }

    private fun wifiNetwork(): Network? {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val isWifi = { n: Network -> cm.getNetworkCapabilities(n)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true }
        cm.activeNetwork?.takeIf(isWifi)?.let { return it }
        @Suppress("DEPRECATION") // the only way to see a WiFi network that is not the default one
        return cm.allNetworks.firstOrNull(isWifi)
    }

    private fun subnetOf(network: Network): Subnet? {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val link = cm.getLinkProperties(network)?.linkAddresses
            ?.firstOrNull { it.address is Inet4Address } ?: return null
        return Subnet(link.address as Inet4Address, link.prefixLength)
    }

    @Serializable
    private data class Reply(
        val service: String = "",
        val id: String,
        val name: String = "heliostat",
        val fw: String? = null,
        val port: Int = 80,
        val mode: String? = null,
    )

    companion object {
        const val PORT = 47474
        private val PROBE = "HELIOSTAT_DISCOVER 1".toByteArray(Charsets.US_ASCII)
    }
}

/** An IPv4 subnet, for the broadcast address and the unicast sweep. */
internal class Subnet(address: Inet4Address, private val prefixLength: Int) {
    private val ip = address.address.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }
    private val mask = if (prefixLength == 0) 0 else -1 shl (32 - prefixLength)
    private val network = ip and mask

    val broadcast: InetAddress get() = toAddress(network or mask.inv())

    /**
     * Every host address except our own -- capped at a /22 (1022 hosts): on a
     * bigger network the sweep costs more than it is worth, and broadcast has
     * to do.
     */
    fun hosts(): List<InetAddress> {
        if (prefixLength < 22 || prefixLength > 30) return emptyList()
        val first = network + 1
        val last = (network or mask.inv()) - 1
        return (first..last).filter { it != ip }.map(::toAddress)
    }

    private fun toAddress(value: Int): InetAddress = InetAddress.getByAddress(
        byteArrayOf((value ushr 24).toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte()),
    )
}
