package com.pulse.app.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// ---------------------------------------------------------------------
// Rule (the automation itself — trigger + condition tree + priority etc.)
// ---------------------------------------------------------------------
@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val conditionJson: String?, // nullable recursive ConditionNode tree, see ContextEntities.kt doc comment
    val priority: Int,
    val cooldownMinutes: Int,
    val minimumDurationMinutes: Int,
    val scheduleJson: String,
    val exceptionsJson: String,
    val enabled: Boolean,
    val canvasLayoutJson: String?,
)

// ---------------------------------------------------------------------
// Trigger — a rule can have more than one (OR'd) trigger
// ---------------------------------------------------------------------
@Entity(
    tableName = "triggers",
    indices = [Index("ruleId")],
    foreignKeys = [ForeignKey(
        entity = RuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class TriggerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val type: String, // TriggerType.name
    val paramsJson: String,
)

// ---------------------------------------------------------------------
// Action — ordered list of actions a rule performs when it fires
// ---------------------------------------------------------------------
@Entity(
    tableName = "actions",
    indices = [Index("ruleId")],
    foreignKeys = [ForeignKey(
        entity = RuleEntity::class,
        parentColumns = ["id"],
        childColumns = ["ruleId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class ActionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val orderIndex: Int,
    val type: String, // ActionType.name
    val paramsJson: String,
)

// ---------------------------------------------------------------------
// ExecutionLog — Rule Execution History / Audit log
// ---------------------------------------------------------------------
@Entity(tableName = "execution_log", indices = [Index("executedAt"), Index("ruleId")])
data class ExecutionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ruleId: String,
    val ruleName: String,
    val executedAt: Long,
    val activeContext: String?,
    val actionsPerformedJson: String,
    val result: String, // ExecutionResult.name
    val detail: String?,
)

// ---------------------------------------------------------------------
// Feedback — user confirm/reject/correct signal for the Learning System
// ---------------------------------------------------------------------
@Entity(tableName = "feedback", indices = [Index("contextType")])
data class FeedbackEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val contextType: String,
    val feedback: String, // ContextFeedback.name
    val recordedAt: Long,
)

@Entity(tableName = "learning_weights")
data class LearningWeightEntity(
    @PrimaryKey val contextType: String,
    val confirmCount: Int,
    val rejectCount: Int,
    val confidenceThresholdAdjustment: Float,
)
