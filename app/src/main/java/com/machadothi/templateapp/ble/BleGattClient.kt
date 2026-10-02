package com.machadothi.templateapp.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class BleConnectionState {
    data object Disconnected : BleConnectionState()
    data object Connecting : BleConnectionState()
    data class Connected(val mtu: Int) : BleConnectionState()
    data class Failed(val reason: String) : BleConnectionState()
}

class GattException(message: String) : Exception(message)

/**
 * Coroutine wrapper around BluetoothGatt, for one heliostat at a time.
 *
 * Android's GATT API is callback-based and processes ONE operation at a time: a
 * second read or write issued before the first completes is silently dropped.
 * Every operation therefore takes [mutex] and suspends until its own callback
 * fires, which turns the whole protocol into plain sequential code.
 *
 * Handles both the pre-33 callbacks (value read from the characteristic object)
 * and the API 33+ ones (value passed in).
 */
@Singleton
@SuppressLint("MissingPermission") // BLUETOOTH_CONNECT is checked by the permission gate
class BleGattClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var gatt: BluetoothGatt? = null
    private var pending: CompletableDeferred<Any?>? = null

    private val _state = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val state: StateFlow<BleConnectionState> = _state

    private val _notifications = MutableSharedFlow<Pair<UUID, ByteArray>>(extraBufferCapacity = 32)

    /** Every notification from the board, as (characteristic UUID, value). */
    val notifications: SharedFlow<Pair<UUID, ByteArray>> = _notifications

    /** Largest write the link allows: negotiated MTU minus the 3-byte ATT header. */
    val maxWrite: Int
        get() = ((_state.value as? BleConnectionState.Connected)?.mtu ?: 23) - 3

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            log("onConnectionStateChange status=$status newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                complete(true)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = if (status == BluetoothGatt.GATT_SUCCESS) {
                    BleConnectionState.Disconnected
                } else {
                    BleConnectionState.Failed("link lost (status $status)")
                }
                fail("disconnected (status $status)")
                gatt.close()
                if (this@BleGattClient.gatt === gatt) this@BleGattClient.gatt = null
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            log("onMtuChanged mtu=$mtu status=$status")
            complete(mtu)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            log("onServicesDiscovered status=$status services=${gatt.services.map { it.uuid.short() }}")
            if (status == BluetoothGatt.GATT_SUCCESS) complete(true)
            else fail("service discovery failed ($status)")
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int,
        ) {
            log("onCharacteristicWrite ${characteristic.uuid.short()} status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) complete(true)
            else fail("write to ${characteristic.uuid.short()} failed (GATT status $status)")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
            value: ByteArray, status: Int,
        ) {
            log("onCharacteristicRead ${characteristic.uuid.short()} status=$status ${value.size} bytes")
            if (status == BluetoothGatt.GATT_SUCCESS) complete(value)
            else fail("read of ${characteristic.uuid.short()} failed (GATT status $status)")
        }

        @Deprecated("API < 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            onCharacteristicRead(gatt, characteristic, characteristic.value ?: ByteArray(0), status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int,
        ) {
            log("onDescriptorWrite ${descriptor.characteristic.uuid.short()} status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) complete(true)
            else fail("enabling notifications failed (GATT status $status)")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray,
        ) {
            log("notification ${characteristic.uuid.short()} ${value.hex()}")
            _notifications.tryEmit(characteristic.uuid to value)
        }

        @Deprecated("API < 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            _notifications.tryEmit(characteristic.uuid to (characteristic.value ?: ByteArray(0)))
        }
    }

    /** Connect, negotiate the MTU, discover services, subscribe to WIFI_STATE. */
    suspend fun connect(address: String) {
        disconnect()
        _state.value = BleConnectionState.Connecting
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device: BluetoothDevice = manager.adapter.getRemoteDevice(address)
            log("connect $address")
            op("connectGatt", CONNECT_TIMEOUT_MS) {
                gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                true
            }
            val mtu = op("requestMtu") { gatt!!.requestMtu(BleConstants.DESIRED_MTU) } as Int
            op("discoverServices") { gatt!!.discoverServices() }
            if (service() == null) throw GattException("not a heliostat: provisioning service missing")
            _state.value = BleConnectionState.Connected(mtu)
            enableNotifications(BleConstants.WIFI_STATE)
            log("connected, mtu=$mtu, notifications on")
        } catch (e: Exception) {
            log("connect failed: $e")
            _state.value = BleConnectionState.Failed(e.message ?: "connection failed")
            disconnect()
            throw e
        }
    }

    suspend fun read(uuid: UUID): ByteArray =
        op("read ${uuid.short()}") { gatt!!.readCharacteristic(characteristic(uuid)) } as ByteArray

    suspend fun write(uuid: UUID, value: ByteArray) {
        val characteristic = characteristic(uuid)
        // Never log the password's bytes.
        val shown = if (uuid == BleConstants.PSK) "(${value.size} bytes, hidden)" else value.hex()
        op("write ${uuid.short()} $shown") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // API 33+ returns a BluetoothStatusCodes value, not a GATT status.
                gatt!!.writeCharacteristic(
                    characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                characteristic.value = value
                @Suppress("DEPRECATION")
                gatt!!.writeCharacteristic(characteristic)
            }
        }
    }

    fun disconnect() {
        if (gatt != null) log("disconnect")
        gatt?.let {
            it.disconnect()
            it.close()
        }
        gatt = null
        fail("disconnected")
        _state.value = BleConnectionState.Disconnected
    }

    // -- internals -------------------------------------------------------------------

    private suspend fun enableNotifications(uuid: UUID) {
        val characteristic = characteristic(uuid)
        gatt!!.setCharacteristicNotification(characteristic, true)
        val cccd = characteristic.getDescriptor(BleConstants.CCCD)
            ?: throw GattException("$uuid has no notification descriptor")
        val enable = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        op("enable notifications ${uuid.short()}") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt!!.writeDescriptor(cccd, enable) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                cccd.value = enable
                @Suppress("DEPRECATION")
                gatt!!.writeDescriptor(cccd)
            }
        }
    }

    private fun service() = gatt?.getService(BleConstants.SERVICE)

    private fun characteristic(uuid: UUID): BluetoothGattCharacteristic =
        service()?.getCharacteristic(uuid) ?: throw GattException("not connected, or no $uuid")

    /**
     * Run one GATT operation and suspend until its callback completes it.
     * [start] returns false when Android refuses to even queue the operation.
     */
    private suspend fun op(name: String, timeoutMs: Long = OP_TIMEOUT_MS, start: () -> Boolean): Any? =
        mutex.withLock {
            val deferred = CompletableDeferred<Any?>()
            pending = deferred
            log("-> $name")
            if (!start()) {
                pending = null
                log("<- $name REFUSED by Android")
                throw GattException("Android refused: $name")
            }
            try {
                withTimeout(timeoutMs) { deferred.await() }.also { log("<- $name ok") }
            } catch (e: TimeoutCancellationException) {
                log("<- $name TIMED OUT after $timeoutMs ms")
                throw GattException("no answer to $name within ${timeoutMs / 1000} s")
            } catch (e: Exception) {
                log("<- $name FAILED: ${e.message}")
                throw e
            } finally {
                pending = null
            }
        }

    private fun complete(result: Any?) {
        pending?.complete(result)
    }

    private fun fail(message: String) {
        pending?.completeExceptionally(GattException(message))
    }

    private companion object {
        const val TAG = "HeliostatBle"
        const val CONNECT_TIMEOUT_MS = 15_000L
        const val OP_TIMEOUT_MS = 8_000L
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}

/** "8a4f1005-..." -> "1005": enough to tell the heliostat's characteristics apart in a log. */
private fun UUID.short(): String = toString().let { if (it.startsWith("8a4f")) it.substring(4, 8) else it.substring(0, 8) }

private fun ByteArray.hex(): String = joinToString(" ") { "%02X".format(it) }.take(72)
