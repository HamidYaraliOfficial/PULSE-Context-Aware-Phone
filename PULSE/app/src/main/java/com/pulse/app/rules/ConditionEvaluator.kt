package com.pulse.app.rules

import com.pulse.app.domain.model.ComparisonOperator
import com.pulse.app.domain.model.ConditionNode
import com.pulse.app.domain.model.ContextState
import com.pulse.app.domain.model.NormalizedContext

/**
 * Evaluates the recursive IF / AND / OR / NOT [ConditionNode] tree against a
 * flat fact map built from the current [NormalizedContext] + [ContextState].
 * Kept as pure, stateless functions so it is trivially unit-testable and so
 * Simulation Mode can run the exact same evaluator against a synthetic fact
 * map without touching any real Android API.
 */
object ConditionEvaluator {

    fun evaluate(node: ConditionNode?, facts: Map<String, String>): Boolean {
        if (node == null) return true
        return when (node) {
            is ConditionNode.Leaf -> evaluateLeaf(node, facts)
            is ConditionNode.And -> node.children.all { evaluate(it, facts) }
            is ConditionNode.Or -> node.children.any { evaluate(it, facts) }
            is ConditionNode.Not -> !evaluate(node.child, facts)
        }
    }

    private fun evaluateLeaf(leaf: ConditionNode.Leaf, facts: Map<String, String>): Boolean {
        val actual = facts[leaf.signalKey] ?: return false
        return when (leaf.operator) {
            ComparisonOperator.EQUALS -> actual.equals(leaf.value, ignoreCase = true)
            ComparisonOperator.NOT_EQUALS -> !actual.equals(leaf.value, ignoreCase = true)
            ComparisonOperator.CONTAINS -> actual.contains(leaf.value, ignoreCase = true)
            ComparisonOperator.GREATER_THAN -> numeric(actual)?.let { a -> numeric(leaf.value)?.let { a > it } } ?: false
            ComparisonOperator.LESS_THAN -> numeric(actual)?.let { a -> numeric(leaf.value)?.let { a < it } } ?: false
            ComparisonOperator.GREATER_OR_EQUAL -> numeric(actual)?.let { a -> numeric(leaf.value)?.let { a >= it } } ?: false
            ComparisonOperator.LESS_OR_EQUAL -> numeric(actual)?.let { a -> numeric(leaf.value)?.let { a <= it } } ?: false
            ComparisonOperator.IN_RANGE -> {
                val bounds = leaf.value.split("..")
                if (bounds.size != 2) return false
                val a = numeric(actual) ?: return false
                val lo = numeric(bounds[0]) ?: return false
                val hi = numeric(bounds[1]) ?: return false
                a in lo..hi
            }
        }
    }

    private fun numeric(value: String): Double? = value.toDoubleOrNull()

    /** Builds the flat key/value fact map every Leaf condition reads from. */
    fun buildFacts(snapshot: NormalizedContext, currentContext: ContextState?): Map<String, String> = buildMap {
        put("time_minutes", snapshot.timeOfDayMinutes.toString())
        put("day_of_week", snapshot.dayOfWeek.toString())
        put("is_charging", snapshot.isCharging.toString())
        put("battery_level", snapshot.batteryLevel.toString())
        put("is_battery_saver", snapshot.isBatterySaver.toString())
        put("detected_activity", snapshot.detectedActivity.name)
        put("activity_confidence", snapshot.activityConfidence.toString())
        put("steps_last_hour", snapshot.stepsLastHour.toString())
        put("is_device_still", snapshot.isDeviceStill.toString())
        put("headphones_connected", snapshot.headphonesConnected.toString())
        put("has_fixed_location_pattern", snapshot.hasFixedLocationPattern.toString())
        put("known_place", snapshot.isAtKnownPlace ?: "")
        put("calendar_event_title", snapshot.activeCalendarEventTitle ?: "")
        put("calendar_is_meeting", snapshot.calendarEventIsMeetingLike.toString())
        put("foreground_app_category", snapshot.foregroundAppCategory.name)
        put("notification_load_last_hour", snapshot.notificationLoadLastHour.toString())
        put("context_type", currentContext?.type?.name ?: "UNKNOWN")
        put("context_confidence", ((currentContext?.confidence ?: 0f) * 100).toInt().toString())
    }
}
