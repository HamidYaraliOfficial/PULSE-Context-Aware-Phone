package com.pulse.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room schema notes
 * ------------------
 * Every primary-entity table uses a UUID string primary key (per spec).
 * The recursive Condition tree (IF/AND/OR/NOT) is not modeled as its own
 * relational table — a tree needs either an adjacency-list table with
 * self-referencing parent ids or a nested-set model, both of which add
 * real complexity for a construct that is always read/written as a whole
 * unit (never queried node-by-node). It is instead persisted as a single
 * JSON column (`conditionJson`) on [RuleEntity] via kotlinx.serialization
 * — see [com.pulse.app.database.Converters]. This is a deliberate,
 * documented trade-off, not a missing feature: the domain-level
 * `ConditionNode` sealed class is still the real recursive IF/AND/OR/NOT
 * model the Rule Engine evaluates against.
 */

// ---------------------------------------------------------------------
// ContextEvent — one row per detected/timeline context state
// ---------------------------------------------------------------------
@Entity(tableName = "context_events", indices = [Index("startedAt"), Index("type")])
data class ContextEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // ContextType.name
    val customLabel: String?,
    val confidence: Float,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long?,
    val signalsJson: String, // List<SignalContribution> serialized
    val feedback: String?, // ContextFeedback.name
    val ignored: Boolean = false,
)

// ---------------------------------------------------------------------
// ContextType — user-defined custom contexts (built-ins are not rows here)
// ---------------------------------------------------------------------
@Entity(tableName = "custom_context_types")
data class CustomContextTypeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val relatedSignalKeysJson: String,
    val actionsJson: String,
    val minConfidence: Float,
)

// ---------------------------------------------------------------------
// Mode
// ---------------------------------------------------------------------
@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // ModeType.name
    val name: String,
    val isBuiltIn: Boolean,
    val actionsJson: String,
    val scheduleJson: String,
    val triggeringContextsJson: String,
    val isActive: Boolean,
    val activatedAtEpochMs: Long?,
    val allowedAppsJson: String,
    val minConfidenceToSuggest: Float,
)

@Entity(tableName = "mode_activation_log", indices = [Index("startedAt")])
data class ModeActivationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val modeId: String,
    val modeType: String,
    val startedAt: Long,
    val endedAt: Long?,
    val triggeredBy: String,
    val notificationsDeferred: Int,
    val distractionsDetected: Int,
)

// ---------------------------------------------------------------------
// Session — Focus / Study sessions
// ---------------------------------------------------------------------
@Entity(tableName = "sessions", indices = [Index("startedAt")])
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goal: String,
    val subject: String?,
    val plannedMinutes: Int,
    val allowedAppsJson: String,
    val startedAt: Long,
    val endedAt: Long?,
    val focusedMinutes: Int,
    val distractionEvents: Int,
    val deferredNotifications: Int,
)

// ---------------------------------------------------------------------
// LocationContext — user-labeled known places (Home / University / Gym…)
// ---------------------------------------------------------------------
@Entity(tableName = "location_contexts")
data class LocationContextEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val linkedContextType: String?,
)

// ---------------------------------------------------------------------
// DeviceContext — periodic normalized-signal snapshots (used by Pattern
// Detection + Simulation Mode replay; battery-aware sampling controls how
// often these are written).
// ---------------------------------------------------------------------
@Entity(tableName = "device_context_snapshots", indices = [Index("timestamp")])
data class DeviceContextEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val normalizedContextJson: String,
)

// ---------------------------------------------------------------------
// CalendarContext — cached, privacy-filtered calendar event snapshot
// ---------------------------------------------------------------------
@Entity(tableName = "calendar_contexts", indices = [Index("startTime")])
data class CalendarContextEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventTitle: String,
    val calendarType: String?,
    val startTime: Long,
    val endTime: Long,
    val isMeetingLike: Boolean,
    val attendeeCount: Int,
)
