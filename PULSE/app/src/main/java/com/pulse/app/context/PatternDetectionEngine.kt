package com.pulse.app.context

import com.pulse.app.domain.model.ContextState
import com.pulse.app.domain.model.ContextType
import com.pulse.app.domain.model.UsagePatternInsight
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Habit/Pattern Detection Engine. Looks only at locally-stored Timeline
 * history — no cloud, no cross-user aggregation — and surfaces simple,
 * clearly-labeled *estimates* ("usually Focus 9–11am", "Tuesdays tend to
 * have more Meetings"), never presented as settled fact. This directly
 * backs [UsagePatternInsight.isEstimate], which the Insights screen must
 * always render as a Suggestion/Estimate badge.
 */
@Singleton
class PatternDetectionEngine @Inject constructor() {

    fun detectPatterns(history: List<ContextState>, zone: ZoneId = ZoneId.systemDefault()): List<UsagePatternInsight> {
        if (history.size < MIN_SAMPLES_FOR_PATTERN) return emptyList()
        val insights = mutableListOf<UsagePatternInsight>()
        insights += detectTypicalHourPattern(history, zone)
        insights += detectTypicalWeekdayPattern(history, zone)
        return insights
    }

    private fun detectTypicalHourPattern(history: List<ContextState>, zone: ZoneId): List<UsagePatternInsight> {
        val byType = history.groupBy { it.type }
        return byType.mapNotNull { (type, events) ->
            if (events.size < MIN_SAMPLES_FOR_PATTERN || type == ContextType.AVAILABLE) return@mapNotNull null
            val hours = events.map { ZonedDateTime.ofInstant(it.startedAt, zone).hour }
            val mode = hours.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return@mapNotNull null
            val concentration = mode.value.toFloat() / events.size
            if (concentration < CONCENTRATION_THRESHOLD) return@mapNotNull null
            UsagePatternInsight(
                descriptionKey = "pattern_typical_hour",
                descriptionParams = mapOf("context" to type.name, "hour" to mode.key.toString()),
                confidence = concentration,
                isEstimate = true,
            )
        }
    }

    private fun detectTypicalWeekdayPattern(history: List<ContextState>, zone: ZoneId): List<UsagePatternInsight> {
        val byType = history.groupBy { it.type }
        return byType.mapNotNull { (type, events) ->
            if (events.size < MIN_SAMPLES_FOR_PATTERN || type == ContextType.AVAILABLE) return@mapNotNull null
            val days = events.map { ZonedDateTime.ofInstant(it.startedAt, zone).dayOfWeek }
            val mode = days.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return@mapNotNull null
            val concentration = mode.value.toFloat() / events.size
            if (concentration < CONCENTRATION_THRESHOLD) return@mapNotNull null
            UsagePatternInsight(
                descriptionKey = "pattern_typical_weekday",
                descriptionParams = mapOf("context" to type.name, "day" to mode.key.name),
                confidence = concentration,
                isEstimate = true,
            )
        }
    }

    private companion object {
        const val MIN_SAMPLES_FOR_PATTERN = 5
        const val CONCENTRATION_THRESHOLD = 0.35f
    }
}
