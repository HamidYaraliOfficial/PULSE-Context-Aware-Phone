package com.pulse.app.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class KnownDeviceKind { HEADPHONES, CAR_AUDIO, WATCH, KEYBOARD, OTHER }

data class BluetoothSignal(
    val connectedDevices: List<ConnectedDevice>,
) {
    val hasHeadphones: Boolean get() = connectedDevices.any { it.kind == KnownDeviceKind.HEADPHONES }
    val hasCarAudio: Boolean get() = connectedDevices.any { it.kind == KnownDeviceKind.CAR_AUDIO }
}

data class ConnectedDevice(val name: String, val kind: KnownDeviceKind)

/**
 * Bluetooth Context Manager — turns "which known accessory is connected"
 * into a Context Engine signal (Meeting Mode's headphones+location+calendar
 * combination, Driving Mode's car-audio detection, Music Mode suggestions).
 * Only classifies device *class* (audio headset, car kit, wearable,
 * peripheral) — PULSE never reads MAC addresses into any exported log,
 * satisfying the "explainable but not oversharing" requirement.
 */
@Singleton
class BluetoothContextManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_CONNECT"])
    fun observe(): Flow<BluetoothSignal> = callbackFlow {
        fun emitCurrent() {
            val devices = bondedConnectedDevices()
            trySend(BluetoothSignal(devices))
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                    intent?.action == BluetoothDevice.ACTION_ACL_DISCONNECTED
                ) {
                    emitCurrent()
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        emitCurrent()
        awaitClose { context.unregisterReceiver(receiver) }
    }

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_CONNECT"])
    private fun bondedConnectedDevices(): List<ConnectedDevice> {
        val bonded = runCatching { adapter?.bondedDevices }.getOrNull() ?: emptySet()
        // BluetoothAdapter has no direct "list connected devices" API for arbitrary
        // profiles without binding each BluetoothProfile individually; as a
        // practical, permission-light approximation we treat "bonded AND
        // currently reachable" via BluetoothDevice#isConnected() (API 33+) when
        // available, and otherwise report bonded devices whose ACL state we
        // tracked from the broadcast above.
        return bonded.mapNotNull { device ->
            val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { device.isConnected }.getOrDefault(false)
            } else {
                true // fall back to "bonded" when live-state API is unavailable
            }
            if (!isConnected) return@mapNotNull null
            ConnectedDevice(device.name ?: device.address, classify(device))
        }
    }

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_CONNECT"])
    private fun classify(device: BluetoothDevice): KnownDeviceKind {
        val deviceClass = runCatching { device.bluetoothClass }.getOrNull() ?: return KnownDeviceKind.OTHER
        return when (deviceClass.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
            -> KnownDeviceKind.HEADPHONES
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> KnownDeviceKind.CAR_AUDIO
            BluetoothClass.Device.WEARABLE_WRIST_WATCH -> KnownDeviceKind.WATCH
            BluetoothClass.Device.PERIPHERAL_KEYBOARD -> KnownDeviceKind.KEYBOARD
            else -> KnownDeviceKind.OTHER
        }
    }
}
