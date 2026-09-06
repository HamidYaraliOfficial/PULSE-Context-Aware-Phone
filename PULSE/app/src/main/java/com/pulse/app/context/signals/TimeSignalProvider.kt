package com.pulse.app.context.signals

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class TimeSignal(val minutesSinceMidnight: Int, val isoDayOfWeek: Int)

/**
 * Cheapest possible signal — no permission, no sensor, no battery cost.
 * Ticks once a minute, which is a fine granularity for every time-based
 * Trigger/Rule/Schedule in PULSE.
 */
@Singleton
class TimeSignalProvider @Inject constructor() {

    fun observe(): Flow<TimeSignal> = flow {
        while (true) {
            emit(snapshot())
            delay(60_000L)
        }
    }

    fun snapshot(): TimeSignal {
        val now = LocalDateTime.now()
        val minutes = now.hour * 60 + now.minute
        return TimeSignal(minutes, now.dayOfWeek.value)
    }
}
