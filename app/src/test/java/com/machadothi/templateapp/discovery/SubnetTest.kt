package com.machadothi.templateapp.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class SubnetTest {

    private fun subnet(ip: String, prefix: Int) = Subnet(InetAddress.getByName(ip) as Inet4Address, prefix)

    @Test
    fun `a home slash-24 broadcasts to dot 255 and sweeps the other 253 hosts`() {
        val s = subnet("192.168.50.132", 24)
        assertEquals("192.168.50.255", s.broadcast.hostAddress)
        val hosts = s.hosts().map { it.hostAddress }
        assertEquals(253, hosts.size)
        assertEquals("192.168.50.1", hosts.first())
        assertEquals("192.168.50.254", hosts.last())
        assertTrue("192.168.50.105" in hosts)
        assertFalse("the phone itself is skipped", "192.168.50.132" in hosts)
    }

    @Test
    fun `a slash-22 is still swept, a slash-16 relies on broadcast alone`() {
        assertEquals(1021, subnet("10.0.1.7", 22).hosts().size)
        assertEquals("10.0.3.255", subnet("10.0.1.7", 22).broadcast.hostAddress)
        assertTrue(subnet("10.0.1.7", 16).hosts().isEmpty())
        assertEquals("10.0.255.255", subnet("10.0.1.7", 16).broadcast.hostAddress)
    }

    @Test
    fun `addresses with the top bit set do not go negative`() {
        val s = subnet("172.20.10.2", 28)
        assertEquals("172.20.10.15", s.broadcast.hostAddress)
        assertEquals((1..14).map { "172.20.10.$it" } - "172.20.10.2", s.hosts().map { it.hostAddress })
    }
}
