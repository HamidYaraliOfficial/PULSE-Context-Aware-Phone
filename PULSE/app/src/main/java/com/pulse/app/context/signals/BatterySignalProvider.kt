package com.pulse.app.context.signals

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class BatterySignal(
    val level: Int,
    val isCharging: Boolean,
    val isPowerSaveMode: Boolean,
)

/**
 * Feeds Charging/Battery Context (used e.g. by Sleep Mode's "charging
 * overnight → suggest Sleep Mode" heuristic) and the Battery-Aware
 * Scheduler. Uses a sticky-intent based BroadcastReceiver — no polling,
 * no wakelock, zero measurable battery cost of its own.
 */
@Singleton
class BatterySignalProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun observe(): Flow<BatterySignal> = callbackFlow {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        fun emitCurrent(intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            trySend(BatterySignal(pct, charging, powerManager.isPowerSaveMode))
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    trySend(
                        BatterySignal(
                            level = lastKnownLevel(context),
                            isCharging = lastKnownCharging(context),
                            isPowerSaveMode = powerManager.isPowerSaveMode,
                        ),
                    )
                } else {
                    emitCurrent(intent)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        val sticky = context.registerReceiver(receiver, filter)
        emitCurrent(sticky)

        awaitClose { context.unregisterReceiver(receiver) }
    }

    private fun lastKnownLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun lastKnownCharging(context: Context): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.isCharging
    }
}
