package com.pulse.app.data.repository

import com.pulse.app.domain.model.TimeRange
import com.pulse.app.domain.model.WeeklySchedule
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime

@Serializable
data class SerializableTimeRange(val startMinute: Int, val endMinute: Int) {
    fun toDomain() = TimeRange(LocalTime.of(startMinute / 60, startMinute % 60), LocalTime.of(endMinute / 60, endMinute % 60))
    companion object {
        fun from(range: TimeRange) = SerializableTimeRange(
            range.start.hour * 60 + range.start.minute,
            range.end.hour * 60 + range.end.minute,
        )
    }
}

@Serializable
data class SerializableSchedule(val rangesByDayIso: Map<Int, List<SerializableTimeRange>> = emptyMap()) {
    fun toDomain(): WeeklySchedule = WeeklySchedule(
        rangesByDayIso.entries.associate { (iso, ranges) ->
            DayOfWeek.of(iso) to ranges.map { it.toDomain() }
        },
    )

    companion object {
        fun from(schedule: WeeklySchedule) = SerializableSchedule(
            schedule.rangesByDay.entries.associate { (day, ranges) ->
                day.value to ranges.map { SerializableTimeRange.from(it) }
            },
        )
    }
}
