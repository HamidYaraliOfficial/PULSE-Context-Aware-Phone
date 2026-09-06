package com.pulse.app.rules

import com.pulse.app.core.util.ScheduleCalculator
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.ModeRepository
import com.pulse.app.domain.repository.RuleRepository
import com.pulse.app.modes.ActionOutcome
import com.pulse.app.modes.ModeActionExecutor
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Rule Engine. Two entry points:
 *  - [onTick] — called every evaluation cycle (see ContextEvaluationWorker)
 *    for continuously-checkable triggers: TIME, CONTEXT_CHANGED,
 *    BATTERY_LEVEL, CHARGING_STATE.
 *  - [onEvent] — called by discrete event sources (app opened, Bluetooth
 *    connect/disconnect, calendar event start/end) for triggers that are
 *    inherently event-driven rather than poll-driven.
 *
 * Both funnel into [evaluateAndExecute], which applies, in order: Schedule
 * (Active Hours) → Exceptions → Condition tree → Cooldown → Conflict
 * Resolution → Action execution → Execution History logging. This is the
 * single choke point that guarantees a Rule can never fire twice within its
 * own cooldown window or against another rule fighting for the same action.
 */
@Singleton
class RuleEngine @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val modeRepository: ModeRepository,
    private val actionExecutor: ModeActionExecutor,
) {
    private var lastContextType: ContextType? = null

    suspend fun onTick(snapshot: NormalizedContext, currentContext: ContextState?) {
        val facts = ConditionEvaluator.buildFacts(snapshot, currentContext)
        val rules = currentEnabledRules()

        val contextChanged = currentContext?.type != lastContextType
        lastContextType = currentContext?.type

        val candidates = rules.filter { rule ->
            rule.enabled && matchesAnyPollableTrigger(rule, snapshot, facts, contextChanged)
        }
        evaluateAndExecute(candidates, facts, currentContext)
    }

    suspend fun onEvent(triggerType: TriggerType, eventParams: Map<String, String>, snapshot: NormalizedContext, currentContext: ContextState?) {
        val facts = ConditionEvaluator.buildFacts(snapshot, currentContext) + eventParams
        val rules = currentEnabledRules()
        val candidates = rules.filter { rule ->
            rule.enabled && rule.trigger.type == triggerType && matchesEventParams(rule.trigger, eventParams)
        }
        evaluateAndExecute(candidates, facts, currentContext)
    }

    private suspend fun currentEnabledRules(): List<AutomationRule> {
        // RuleRepository.rules is a Flow; RuleEngine reads a fresh snapshot each
        // tick via first() rather than staying subscribed, since evaluation is
        // driven by the (already coalesced) ContextEvaluationWorker cadence.
        return ruleRepository.rulesSnapshot()
    }

    private fun matchesAnyPollableTrigger(
        rule: AutomationRule,
        snapshot: NormalizedContext,
        facts: Map<String, String>,
        contextChanged: Boolean,
    ): Boolean = when (rule.trigger.type) {
        TriggerType.TIME -> {
            val target = rule.trigger.params["time"] // "HH:mm"
            val parts = target?.split(":")
            val targetMinutes = parts?.getOrNull(0)?.toIntOrNull()?.times(60)?.plus(parts.getOrNull(1)?.toIntOrNull() ?: 0)
            targetMinutes != null && targetMinutes == snapshot.timeOfDayMinutes
        }
        TriggerType.CONTEXT_CHANGED -> {
            val wanted = rule.trigger.params["contextType"]
            contextChanged && (wanted == null || wanted == facts["context_type"])
        }
        TriggerType.BATTERY_LEVEL -> {
            val op = rule.trigger.params["operator"] ?: "LESS_THAN"
            val value = rule.trigger.params["value"]?.toIntOrNull() ?: return false
            when (op) {
                "GREATER_THAN" -> snapshot.batteryLevel > value
                "EQUALS" -> snapshot.batteryLevel == value
                else -> snapshot.batteryLevel < value
            }
        }
        TriggerType.CHARGING_STATE -> {
            val wanted = rule.trigger.params["state"] == "charging"
            snapshot.isCharging == wanted
        }
        TriggerType.MANUAL -> false // manual triggers only fire via explicit user action, not polling
        else -> false // event-driven triggers are matched by onEvent(), not here
    }

    private fun matchesEventParams(trigger: Trigger, eventParams: Map<String, String>): Boolean {
        // A trigger with no extra params always matches any event of its type;
        // a trigger with e.g. {"packageName":"com.slack"} only matches an
        // APP_OPENED event carrying the same package.
        return trigger.params.all { (k, v) -> eventParams[k] == v }
    }

    private suspend fun evaluateAndExecute(
        candidates: List<AutomationRule>,
        facts: Map<String, String>,
        currentContext: ContextState?,
    ) {
        if (candidates.isEmpty()) return
        val now = LocalDateTime.now()

        val eligible = mutableListOf<AutomationRule>()
        for (rule in candidates) {
            val scheduleActive = ScheduleCalculator.computeStatus(rule.schedule, now).isActiveNow
            if (!scheduleActive) continue

            val exceptionTriggered = rule.exceptions.any { ConditionEvaluator.evaluate(it.condition, facts) }
            if (exceptionTriggered) continue

            if (!ConditionEvaluator.evaluate(rule.condition, facts)) continue

            val lastRun = ruleRepository.lastExecutionFor(rule.id)
            if (rule.cooldownMinutes > 0 && lastRun != null) {
                val minutesSince = java.time.Duration.between(lastRun.executedAt, Instant.now()).toMinutes()
                if (minutesSince < rule.cooldownMinutes) {
                    logExecution(rule, currentContext, emptyList(), ExecutionResult.SKIPPED_COOLDOWN)
                    continue
                }
            }
            eligible += rule
        }

        val resolution = ConflictResolver.resolve(eligible)
        resolution.skippedRules.forEach { logExecution(it, currentContext, emptyList(), ExecutionResult.SKIPPED_CONFLICT) }

        for (rule in resolution.allowedRules) {
            val outcomes = rule.actions.map { action -> action.type to runAction(action) }
            val anyPermissionMissing = outcomes.any { it.second == ActionOutcome.MISSING_PERMISSION }
            val result = if (anyPermissionMissing) ExecutionResult.SKIPPED_PERMISSION else ExecutionResult.SUCCESS
            logExecution(rule, currentContext, outcomes.map { it.first }, result)
        }
    }

    private suspend fun runAction(action: RuleAction): ActionOutcome = when (action.type) {
        ActionType.ACTIVATE_MODE -> {
            val modeId = action.params["modeId"]
            if (modeId != null) {
                modeRepository.activate(modeId, "rule")
                ActionOutcome.SUCCESS
            } else ActionOutcome.FAILED
        }
        ActionType.DEACTIVATE_MODE -> {
            val modeId = action.params["modeId"]
            if (modeId != null) {
                modeRepository.deactivate(modeId)
                ActionOutcome.SUCCESS
            } else ActionOutcome.FAILED
        }
        // SUGGEST_MODE, DEFER_NOTIFICATIONS, SEND_DIGEST and other data-carrying
        // actions are intentionally handled by the engine that owns their state
        // (RecommendationRepository / SmartNotificationEngine) rather than here —
        // see ModeActionExecutor's doc comment for the same design note.
        else -> actionExecutor.execute(action)
    }

    private suspend fun logExecution(
        rule: AutomationRule,
        currentContext: ContextState?,
        actionsPerformed: List<ActionType>,
        result: ExecutionResult,
    ) {
        ruleRepository.recordExecution(
            RuleExecutionRecord(
                ruleId = rule.id,
                ruleName = rule.name,
                activeContext = currentContext?.type,
                actionsPerformed = actionsPerformed,
                result = result,
            ),
        )
    }
}
