package com.pulse.app.worker

import com.pulse.app.context.signals.BatterySignal
import com.pulse.app.context.signals.SensorSignalProvider
import javax.inject.Inject
import javax.inject.Singleton

data class SchedulingPlan(
    val evaluationIntervalMs: Long,
    val sensorProfile: SensorSignalProvider.SamplingProfile,
    val locationUpdatesEnabled: Boolean,
    val backgroundWorkEnabled: Boolean,
)

/**
 * Battery-Aware Scheduler. Every sampling-rate decision in PULSE funnels
 * through this one function so the "under Battery Saver, samplings reduce
 * and unnecessary background work stops" requirement is enforced in one
 * place instead of scattered battery-level checks across every collector.
 */
@Singleton
class BatteryAwareScheduler @Inject constructor() {

    fun plan(battery: BatterySignal): SchedulingPlan = when {
        battery.isPowerSaveMode -> SchedulingPlan(
            evaluationIntervalMs = 5 * 60_000L,
            sensorProfile = SensorSignalProvider.SamplingProfile.BATTERY_SAVER,
            locationUpdatesEnabled = false,
            backgroundWorkEnabled = battery.isCharging,
        )
        battery.level <= LOW_BATTERY_THRESHOLD && !battery.isCharging -> SchedulingPlan(
            evaluationIntervalMs = 3 * 60_000L,
            sensorProfile = SensorSignalProvider.SamplingProfile.BATTERY_SAVER,
            locationUpdatesEnabled = false,
            backgroundWorkEnabled = true,
        )
        else -> SchedulingPlan(
            evaluationIntervalMs = 60_000L,
            sensorProfile = SensorSignalProvider.SamplingProfile.NORMAL,
            locationUpdatesEnabled = true,
            backgroundWorkEnabled = true,
        )
    }

    private companion object {
        const val LOW_BATTERY_THRESHOLD = 20
    }
}
