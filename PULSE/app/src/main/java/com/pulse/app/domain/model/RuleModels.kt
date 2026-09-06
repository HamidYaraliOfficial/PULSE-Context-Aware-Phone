package com.pulse.app.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** What kind of event can start evaluating a rule. */
enum class TriggerType {
    TIME, SENSOR, LOCATION_ENTER, LOCATION_EXIT, CALENDAR_EVENT_START, CALENDAR_EVENT_END,
    APP_OPENED, BLUETOOTH_CONNECTED, BLUETOOTH_DISCONNECTED, BATTERY_LEVEL, CHARGING_STATE,
    CONTEXT_CHANGED, MANUAL
}

@Serializable
data class Trigger(
    val type: TriggerType,
    /** Free-form key/value params interpreted per [type], e.g. {"time":"22:30"} or {"contextType":"MEETING"}. */
    val params: Map<String, String> = emptyMap(),
)

enum class ComparisonOperator { EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN, GREATER_OR_EQUAL, LESS_OR_EQUAL, CONTAINS, IN_RANGE }

/**
 * A boolean condition tree supporting IF / AND / OR / NOT as requested —
 * arbitrarily nestable so the Visual Automation Builder can represent
 * complex compound rules ("driving detected AND NOT (headphones connected OR
 * it's before 07:00)").
 */
@Serializable
sealed class ConditionNode {
    @Serializable
    data class Leaf(
        val signalKey: String,
        val operator: ComparisonOperator,
        val value: String,
    ) : ConditionNode()

    @Serializable
    data class And(val children: List<ConditionNode>) : ConditionNode()

    @Serializable
    data class Or(val children: List<ConditionNode>) : ConditionNode()

    @Serializable
    data class Not(val child: ConditionNode) : ConditionNode()
}

enum class ActionType {
    SET_DND, SET_VOLUME, SET_BRIGHTNESS, SET_SCREEN_TIMEOUT, SHOW_NOTIFICATION,
    SUGGEST_MODE, ACTIVATE_MODE, DEACTIVATE_MODE, LAUNCH_APP, LAUNCH_SHORTCUT,
    VIBRATE, CREATE_REMINDER, CALENDAR_ACTION, DEFER_NOTIFICATIONS, SEND_DIGEST,
    SET_WALLPAPER_THEME, SET_ALARM_TIMER, MEDIA_CONTROL, CUSTOM
}

@Serializable
data class RuleAction(
    val type: ActionType,
    val params: Map<String, String> = emptyMap(),
)

/** Resolution strategy applied when two enabled rules fire in the same evaluation tick and conflict. */
enum class ConflictResolutionStrategy { HIGHEST_PRIORITY, MOST_SPECIFIC, ASK_USER, LAST_WRITER_WINS }

@Serializable
data class RuleException(val description: String, val condition: ConditionNode)

/**
 * A complete user-authored automation: Trigger → Condition tree → Actions,
 * plus the scheduling / anti-flapping controls (Priority, Cooldown,
 * Duration, Schedule, Exception) requested for the Rule Engine.
 */
data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: Trigger,
    val condition: ConditionNode? = null,
    val actions: List<RuleAction>,
    val priority: Int = 0,
    val cooldownMinutes: Int = 0,
    val minimumDurationMinutes: Int = 0,
    val schedule: WeeklySchedule = WeeklySchedule(),
    val exceptions: List<RuleException> = emptyList(),
    val enabled: Boolean = true,
    val canvasLayout: String? = null, // serialized node positions for the Visual Automation Builder
)

/** One row of Rule Execution History. */
data class RuleExecutionRecord(
    val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val ruleName: String,
    val executedAt: java.time.Instant = java.time.Instant.now(),
    val activeContext: ContextType?,
    val actionsPerformed: List<ActionType>,
    val result: ExecutionResult,
    val detail: String? = null,
)

enum class ExecutionResult { SUCCESS, SKIPPED_COOLDOWN, SKIPPED_CONFLICT, SKIPPED_PERMISSION, FAILED }
