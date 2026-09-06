package com.pulse.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pulse.app.R
import com.pulse.app.core.util.ScheduleCalculator
import com.pulse.app.domain.model.TimeRange
import com.pulse.app.domain.model.WeeklySchedule
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The "opening hours" style editor: the user builds a [WeeklySchedule]
 * entirely themselves — nothing here is pre-filled or assumed — and the
 * live status line shows exactly what a store-hours widget would: whether
 * it's active right now, and how long until the next change.
 */
@Composable
fun ScheduleEditor(
    schedule: WeeklySchedule,
    onScheduleChange: (WeeklySchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = LocalDateTime.now()
        }
    }
    val status = remember(schedule, now) { ScheduleCalculator.computeStatus(schedule, now) }

    Column(modifier) {
        Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (schedule.isEmpty) {
            Text(stringResource(R.string.schedule_no_hours_set), style = MaterialTheme.typography.bodyMedium)
        } else {
            val statusText = if (status.isActiveNow) stringResource(R.string.schedule_status_active) else stringResource(R.string.schedule_status_inactive)
            val countdown = status.timeUntilNextChange?.let { ScheduleCalculator.formatDuration(it) }
            AssistChip(onClick = {}, label = { Text(if (countdown != null) "$statusText · ${stringResource(R.string.schedule_next_change_in, countdown)}" else statusText) })
        }

        Spacer(Modifier.height(12.dp))

        DayOfWeek.entries.forEach { day ->
            val ranges = schedule.rangesByDay[day].orEmpty()
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(dayLabel(day), modifier = Modifier.width(56.dp))
                Text(
                    ranges.joinToString(", ") { "${it.start} – ${it.end}" }.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    val updated = schedule.rangesByDay.toMutableMap()
                    updated[day] = ranges + TimeRange(LocalTime.of(9, 0), LocalTime.of(17, 0))
                    onScheduleChange(WeeklySchedule(updated))
                }) { Text(stringResource(R.string.common_add)) }
            }
        }
    }
}

@Composable
private fun dayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> stringResource(R.string.schedule_day_mon)
    DayOfWeek.TUESDAY -> stringResource(R.string.schedule_day_tue)
    DayOfWeek.WEDNESDAY -> stringResource(R.string.schedule_day_wed)
    DayOfWeek.THURSDAY -> stringResource(R.string.schedule_day_thu)
    DayOfWeek.FRIDAY -> stringResource(R.string.schedule_day_fri)
    DayOfWeek.SATURDAY -> stringResource(R.string.schedule_day_sat)
    DayOfWeek.SUNDAY -> stringResource(R.string.schedule_day_sun)
}
