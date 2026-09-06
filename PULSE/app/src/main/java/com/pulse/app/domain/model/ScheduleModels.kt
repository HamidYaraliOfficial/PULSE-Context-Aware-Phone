package com.pulse.app.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A user-defined "active hours" range within a single day, e.g. 08:00–17:00
 * for a Study Mode schedule, or 22:30–06:30 (overnight — [end] < [start]) for
 * a Sleep Mode schedule. Fully editable by the user in [com.pulse.app.ui.components.ScheduleEditor];
 * PULSE never assumes hours on the user's behalf.
 */
data class TimeRange(val start: LocalTime, val end: LocalTime) {
    /** True when this range wraps past midnight (e.g. 22:30 → 06:30). */
    val isOvernight: Boolean get() = end < start

    fun contains(time: LocalTime): Boolean =
        if (isOvernight) time >= start || time < end else time >= start && time < end
}

/** A full weekly schedule the user builds themselves — zero or more [TimeRange]s per day. */
data class WeeklySchedule(
    val rangesByDay: Map<DayOfWeek, List<TimeRange>> = emptyMap(),
) {
    val isEmpty: Boolean get() = rangesByDay.values.all { it.isEmpty() }

    fun copyRangeToAllDays(range: TimeRange): WeeklySchedule =
        WeeklySchedule(DayOfWeek.entries.associateWith { listOf(range) })
}

/** Computed "is this active right now, and when does that next change" status for a schedule. */
data class ScheduleStatus(
    val isActiveNow: Boolean,
    val nextChangeAt: LocalDateTime?,
    val timeUntilNextChange: Duration?,
)
