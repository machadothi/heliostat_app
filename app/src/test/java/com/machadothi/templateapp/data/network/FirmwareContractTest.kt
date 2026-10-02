package com.machadothi.templateapp.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.machadothi.templateapp.di.DataModule
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

/**
 * The app's HTTP layer against the firmware's REAL responses.
 *
 * Every fixture in src/test/resources/firmware was recorded from the heliostat
 * repo's tools/mock_server.py, which runs the firmware's own net/api.py -- except
 * the `*_hardware_*` ones, recorded from the real board (single-precision floats,
 * a real MPU6050). If the
 * firmware renames a field or changes a shape, these tests fail here -- before a
 * phone ever shows a blank dashboard.
 */
class FirmwareContractTest {

    private lateinit var server: MockWebServer
    private lateinit var service: HeliostatService
    private val hostSelector = HostSelectionInterceptor()
    private val json = DataModule.providesJson()

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResource("firmware/$name")!!.readText()

    private fun serve(name: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(fixture(name)))

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        // Exactly as the app does it: a placeholder base URL, rewritten per request.
        hostSelector.host = "${server.hostName}:${server.port}"
        service = Retrofit.Builder()
            .baseUrl("http://heliostat.local/")
            .client(OkHttpClient.Builder().addInterceptor(hostSelector).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(HeliostatService::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `telemetry while tracking parses`() = runBlocking {
        serve("telemetry_tracking.json")
        val t = service.telemetry()
        assertEquals("track", t.mode)
        assertEquals(2, t.sun!!.size)
        assertEquals(2, t.pos.size)
        assertNotNull(t.efficiency)
        assertEquals(2, t.volts.size)
    }

    @Test
    fun `hardware telemetry with IMU readings and a latched tilt parses`() = runBlocking {
        serve("telemetry_hardware_tilt_latched.json")
        val t = service.telemetry()
        assertEquals("stow", t.mode)
        assertEquals(listOf("tilt"), t.trips)
        assertNotNull(t.latched)
        assertEquals(47.36, t.tilt_deg!!, 0.01)
        assertEquals(0.974, t.accel_g!!, 0.001)
        assertEquals(false, t.imu_calibrated)
        assertEquals(null, t.beam)
    }

    @Test
    fun `hardware status carries the stable id used to find the heliostat again`() = runBlocking {
        serve("status_hardware.json")
        val s = service.status()
        assertEquals("84cca85ed290", s.device!!.id)
        assertEquals("my_heliostat", s.device!!.name)
    }

    @Test
    fun `status parses including nested objects`() = runBlocking {
        serve("status.json")
        val s = service.status()
        assertTrue(s.time_valid)
        assertEquals("my_heliostat", s.device!!.name)
        assertEquals(180.0, s.target!!.az, 1e-9)
        assertTrue(s.allowed_modes.isNotEmpty())
    }

    @Test
    fun `jog and target replies parse`() = runBlocking {
        serve("jog_ok.json")
        val jog = service.jog(JogRequest(0, 1.5))
        assertTrue(jog.ok)
        assertEquals(181.41, jog.target_deg, 0.01)

        serve("target_ok.json")
        assertEquals(190.0, service.setTarget(TargetRequest(190.0, 12.0)).target!!.az, 1e-9)
    }

    @Test
    fun `requests go to the selected host with the right path and body`() = runBlocking {
        serve("mode_ok.json")
        service.setMode(ModeRequest("track"))
        val request = server.takeRequest()
        assertEquals("/api/mode", request.path)
        assertEquals("""{"mode":"track"}""", request.body.readUtf8())
    }

    @Test
    fun `refusals carry the firmware's own message`() = runBlocking {
        serve("error_409_notime.json", code = 409)
        val error = runCatching { service.setMode(ModeRequest("track")) }.exceptionOrNull()
        val body = (error as HttpException).response()!!.errorBody()!!.string()
        assertEquals(
            "cannot go from manual to track (allowed: idle, stow, fault, estop)",
            json.decodeFromString<ErrorResponse>(body).error,
        )
    }

    @Test
    fun `every recorded error decodes`() {
        listOf("error_400.json", "error_409.json", "error_409_notime.json").forEach {
            assertTrue(json.decodeFromString<ErrorResponse>(fixture(it)).error.isNotBlank())
        }
    }
}
