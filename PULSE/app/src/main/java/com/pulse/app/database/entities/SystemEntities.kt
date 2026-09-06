package com.pulse.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// ---------------------------------------------------------------------
// UsageRecord — locally analyzed app-foreground windows (UsageStatsManager)
// ---------------------------------------------------------------------
@Entity(tableName = "usage_records", indices = [Index("windowStart"), Index("packageName")])
data class UsageRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String,
    val category: String, // AppCategory.name
    val windowStart: Long,
    val windowEnd: Long,
    val foregroundMillis: Long,
)

// ---------------------------------------------------------------------
// NotificationRecord — metadata only (never full notification text)
// ---------------------------------------------------------------------
@Entity(tableName = "notification_records", indices = [Index("postedAt")])
data class NotificationRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val category: String?,
    val priorityBucket: String, // NotificationPriorityBucket.name
    val postedAt: Long,
    val deferred: Boolean,
    val activeContext: String?,
)

// ---------------------------------------------------------------------
// Preference — generic app-level key/value store for settings that are
// not appearance/language (those live in ThemeManager's DataStore) —
// e.g. retention windows, feature toggles that must be queryable via SQL
// for the Battery-Aware Scheduler.
// ---------------------------------------------------------------------
@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)

// ---------------------------------------------------------------------
// PermissionState — cached OS grant status + independent feature toggle
// ---------------------------------------------------------------------
@Entity(tableName = "permission_states")
data class PermissionStateEntity(
    @PrimaryKey val permission: String, // PulsePermission.name
    val osStatus: String, // PermissionStatus.name
    val featureEnabled: Boolean,
)

// ---------------------------------------------------------------------
// Audit log — every Rule / Settings change, independent of rule execution
// ---------------------------------------------------------------------
@Entity(tableName = "audit_log", indices = [Index("timestamp")])
data class AuditLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val category: String, // "rule" | "mode" | "settings" | "permission"
    val summary: String,
)
