package com.pulse.app.domain.model

import java.time.Instant
import java.util.UUID

enum class ModeType {
    FOCUS, STUDY, WORK, MEETING, DRIVING, SLEEP, WORKOUT, TRAVEL, GAMING, QUIET, CUSTOM
}

/**
 * A Mode bundles a set of [RuleAction]s that get applied together, plus an
 * optional [WeeklySchedule] of active hours the user fills in themselves
 * (e.g. Study Mode 08:00–17:00 on weekdays) and the [ContextType]s that make
 * the Recommendation Engine suggest switching into it.
 */
data class PulseMode(
    val id: String = UUID.randomUUID().toString(),
    val type: ModeType,
    val name: String,
    val isBuiltIn: Boolean = true,
    val actions: List<RuleAction> = emptyList(),
    val schedule: WeeklySchedule = WeeklySchedule(),
    val triggeringContexts: List<ContextType> = emptyList(),
    val isActive: Boolean = false,
    val activatedAt: Instant? = null,
    val allowedApps: List<String> = emptyList(),
    val minConfidenceToSuggest: Float = 0.7f,
)

data class ModeActivationRecord(
    val id: String = UUID.randomUUID().toString(),
    val modeId: String,
    val modeType: ModeType,
    val startedAt: Instant,
    val endedAt: Instant?,
    val triggeredBy: String, // "manual" | "rule:<id>" | "recommendation"
    val notificationsDeferred: Int = 0,
    val distractionsDetected: Int = 0,
)

/** A single Focus/Study session — separate from Mode activation because it carries a goal + summary. */
data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val subject: String? = null, // Study Mode only
    val plannedMinutes: Int,
    val allowedApps: List<String> = emptyList(),
    val startedAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
    val focusedMinutes: Int = 0,
    val distractionEvents: Int = 0,
    val deferredNotifications: Int = 0,
)

/** Recommendation Engine suggestion, awaiting Accept / Dismiss / Always Allow. */
data class Recommendation(
    val id: String = UUID.randomUUID().toString(),
    val titleKey: String,
    val messageParams: Map<String, String> = emptyMap(),
    val suggestedModeId: String? = null,
    val suggestedRuleId: String? = null,
    val createdAt: Instant = Instant.now(),
    val confidence: Float,
)

enum class RecommendationDecision { ACCEPTED, DISMISSED, ALWAYS_ALLOW }
