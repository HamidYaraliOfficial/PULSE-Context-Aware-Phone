package com.pulse.app.core.util

import com.pulse.app.domain.model.ScheduleStatus
import com.pulse.app.domain.model.WeeklySchedule
import java.time.Duration
import java.time.LocalDateTime

/**
 * Pure function engine that turns a fully user-entered [WeeklySchedule]
 * (the "opening hours" style editor used for Modes, Rules, Sleep bedtime,
 * etc.) into a live status: is it active right now, and — critically —
 * how long until the next transition, computed the same way a "open now,
 * closes in 42 min" store-hours widget would. Nothing here is inferred;
 * every range comes straight from what the user typed into [com.pulse.app.ui.components.ScheduleEditor].
 */
object ScheduleCalculator {

    fun computeStatus(schedule: WeeklySchedule, now: LocalDateTime = LocalDateTime.now()): ScheduleStatus {
        if (schedule.isEmpty) {
            // No hours configured at all → treated as "always eligible",
            // i.e. the schedule places no restriction on the Mode/Rule.
            return ScheduleStatus(isActiveNow = true, nextChangeAt = null, timeUntilNextChange = null)
        }

        val active = isActiveAt(schedule, now)
        val nextBoundary = findNextBoundary(schedule, now)
        val remaining = nextBoundary?.let { Duration.between(now, it) }
        return ScheduleStatus(active, nextBoundary, remaining)
    }

    private fun isActiveAt(schedule: WeeklySchedule, dt: LocalDateTime): Boolean {
        val time = dt.toLocalTime()
        val today = dt.dayOfWeek
        val todayRanges = schedule.rangesByDay[today].orEmpty()
        if (todayRanges.any { it.contains(time) }) return true

        // An overnight range started yesterday can still be active past midnight.
        val yesterday = today.minus(1)
        val yesterdayRanges = schedule.rangesByDay[yesterday].orEmpty()
        return yesterdayRanges.any { it.isOvernight && time < it.end }
    }

    private fun findNextBoundary(schedule: WeeklySchedule, from: LocalDateTime): LocalDateTime? {
        val candidates = mutableListOf<LocalDateTime>()
        for (dayOffset in 0..7) {
            val date = from.toLocalDate().plusDays(dayOffset.toLong())
            val ranges = schedule.rangesByDay[date.dayOfWeek].orEmpty()
            for (range in ranges) {
                candidates += LocalDateTime.of(date, range.start)
                candidates += if (range.isOvernight) {
                    LocalDateTime.of(date.plusDays(1), range.end)
                } else {
                    LocalDateTime.of(date, range.end)
                }
            }
        }
        return candidates.filter { it.isAfter(from) }.minOrNull()
    }

    /** Formats a [Duration] as "2h 14m" / "42m" for compact UI display. */
    fun formatDuration(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
