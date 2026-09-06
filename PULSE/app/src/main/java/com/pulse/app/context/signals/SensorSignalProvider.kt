package com.pulse.app.context.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

data class MotionSignal(
    val isDeviceStill: Boolean,
    val stepsDelta: Int,
    val ambientLightLux: Float?,
    val proximityNear: Boolean,
)

/**
 * Fuses Accelerometer + Step Counter + Light + Proximity into one compact
 * [MotionSignal]. Sampling rate is intentionally SENSOR_DELAY_NORMAL
 * (~200ms) rather than GAME/FASTEST — the Context Engine only needs
 * "is the phone moving right now", not gesture-level precision, and a
 * coarser rate meaningfully lowers battery draw. [SamplingProfile] lets the
 * Battery-Aware Scheduler widen this further under Battery Saver.
 */
@Singleton
class SensorSignalProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class SamplingProfile(val delayUs: Int) {
        NORMAL(SensorManager.SENSOR_DELAY_NORMAL),
        BATTERY_SAVER(SensorManager.SENSOR_DELAY_UI),
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observe(profile: SamplingProfile = SamplingProfile.NORMAL): Flow<MotionSignal> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        var lastMagnitude = 9.8f
        var movingSampleCount = 0
        var lastStepTotal: Float? = null
        var currentLight: Float? = null
        var currentProximityNear = false
        var currentStepsDelta = 0

        fun emitState() {
            trySend(
                MotionSignal(
                    isDeviceStill = movingSampleCount < STILL_THRESHOLD_SAMPLES,
                    stepsDelta = currentStepsDelta,
                    ambientLightLux = currentLight,
                    proximityNear = currentProximityNear,
                ),
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val (x, y, z) = event.values
                        val magnitude = sqrt(x * x + y * y + z * z)
                        val delta = kotlin.math.abs(magnitude - lastMagnitude)
                        lastMagnitude = magnitude
                        if (delta > MOTION_DELTA_THRESHOLD) {
                            movingSampleCount = (movingSampleCount + 1).coerceAtMost(WINDOW_SIZE)
                        } else {
                            movingSampleCount = (movingSampleCount - 1).coerceAtLeast(0)
                        }
                        emitState()
                    }
                    Sensor.TYPE_STEP_COUNTER -> {
                        val total = event.values.firstOrNull() ?: return
                        val previous = lastStepTotal
                        lastStepTotal = total
                        if (previous != null) {
                            currentStepsDelta += (total - previous).toInt().coerceAtLeast(0)
                        }
                        emitState()
                    }
                    Sensor.TYPE_LIGHT -> {
                        currentLight = event.values.firstOrNull()
                        emitState()
                    }
                    Sensor.TYPE_PROXIMITY -> {
                        val maxRange = event.sensor.maximumRange
                        currentProximityNear = (event.values.firstOrNull() ?: maxRange) < maxRange
                        emitState()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        listOfNotNull(accelerometer, stepCounter, light, proximity).forEach { sensor ->
            sensorManager.registerListener(listener, sensor, profile.delayUs)
        }

        emitState()
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        const val MOTION_DELTA_THRESHOLD = 0.9f
        const val WINDOW_SIZE = 8
        const val STILL_THRESHOLD_SAMPLES = 2
    }
}
