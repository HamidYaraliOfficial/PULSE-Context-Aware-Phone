package com.pulse.app.core

import com.pulse.app.core.util.ScheduleCalculator
import com.pulse.app.domain.model.TimeRange
import com.pulse.app.domain.model.WeeklySchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleCalculatorTest {

    @Test
    fun `empty schedule is always active with no next change`() {
        val status = ScheduleCalculator.computeStatus(WeeklySchedule(), LocalDateTime.of(2026, 8, 17, 10, 0))
        assertTrue(status.isActiveNow)
        assertEquals(null, status.nextChangeAt)
    }

    @Test
    fun `time within a simple daytime range is active`() {
        val schedule = WeeklySchedule(
            mapOf(DayOfWeek.MONDAY to listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0)))),
        )
        val monday10am = LocalDateTime.of(2026, 8, 17, 10, 0) // a Monday
        val status = ScheduleCalculator.computeStatus(schedule, monday10am)
        assertTrue(status.isActiveNow)
        assertEquals(LocalDateTime.of(2026, 8, 17, 17, 0), status.nextChangeAt)
    }

    @Test
    fun `overnight range correctly wraps past midnight`() {
        val schedule = WeeklySchedule(
            mapOf(DayOfWeek.MONDAY to listOf(TimeRange(LocalTime.of(22, 30), LocalTime.of(6, 30)))),
        )
        // 1am Tuesday should still be "active" because Monday's range wraps into Tuesday
        val tuesday1am = LocalDateTime.of(2026, 8, 18, 1, 0)
        val status = ScheduleCalculator.computeStatus(schedule, tuesday1am)
        assertTrue(status.isActiveNow)
        assertEquals(LocalDateTime.of(2026, 8, 18, 6, 30), status.nextChangeAt)
    }

    @Test
    fun `time outside any range is inactive`() {
        val schedule = WeeklySchedule(
            mapOf(DayOfWeek.MONDAY to listOf(TimeRange(LocalTime.of(8, 0), LocalTime.of(17, 0)))),
        )
        val monday6pm = LocalDateTime.of(2026, 8, 17, 18, 0)
        val status = ScheduleCalculator.computeStatus(schedule, monday6pm)
        assertFalse(status.isActiveNow)
    }
}
