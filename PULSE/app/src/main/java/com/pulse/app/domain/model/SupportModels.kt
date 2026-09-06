package com.pulse.app.domain.model

import java.time.Instant
import java.util.UUID

// ---------------------------------------------------------------------
// Permissions
// ---------------------------------------------------------------------
enum class PulsePermission {
    NOTIFICATIONS, LOCATION, ACTIVITY_RECOGNITION, SENSORS, USAGE_ACCESS,
    CALENDAR, BLUETOOTH, NOTIFICATION_ACCESS, DND_ACCESS
}

enum class PermissionStatus { GRANTED, DENIED, NOT_REQUESTED }

data class PermissionFeatureState(
    val permission: PulsePermission,
    val status: PermissionStatus,
    /** Independent per-feature privacy toggle — a permission can be OS-granted but still off inside PULSE. */
    val featureEnabled: Boolean,
)

// ---------------------------------------------------------------------
// App usage
// ---------------------------------------------------------------------
data class AppUsageRecord(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val windowStart: Instant,
    val windowEnd: Instant,
    val foregroundMillis: Long,
)

data class UsagePatternInsight(
    val descriptionKey: String,
    val descriptionParams: Map<String, String>,
    val confidence: Float,
    val isEstimate: Boolean = true,
)

// ---------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------
enum class NotificationPriorityBucket { HIGH, NORMAL, LOW }

data class NotificationRecordModel(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val category: String?,
    val priorityBucket: NotificationPriorityBucket,
    val postedAt: Instant,
    val deferred: Boolean,
    val activeContext: ContextType?,
)

data class NotificationDigest(
    val forContext: ContextType,
    val periodStart: Instant,
    val periodEnd: Instant,
    val deferredCount: Int,
    val topApps: List<Pair<String, Int>>,
)

// ---------------------------------------------------------------------
// Analytics
// ---------------------------------------------------------------------
enum class AnalyticsPeriod { DAILY, WEEKLY, MONTHLY }

data class AnalyticsSummary(
    val period: AnalyticsPeriod,
    val focusMinutes: Int,
    val distractionCount: Int,
    val notificationLoad: Int,
    val modeSwitches: Int,
    val activeHours: Int,
    val timeByContext: Map<ContextType, Int>, // minutes
)

// ---------------------------------------------------------------------
// Learning system
// ---------------------------------------------------------------------
data class ContextLearningWeight(
    val contextType: ContextType,
    val confirmCount: Int = 0,
    val rejectCount: Int = 0,
    val confidenceThresholdAdjustment: Float = 0f, // added/subtracted from base threshold
)

// ---------------------------------------------------------------------
// Data retention
// ---------------------------------------------------------------------
enum class RetentionCategory { CONTEXT_HISTORY, USAGE_ANALYTICS, NOTIFICATION_METADATA, EVENT_LOGS }
enum class RetentionPeriod(val days: Int?) {
    SEVEN_DAYS(7), THIRTY_DAYS(30), NINETY_DAYS(90), FOREVER(null)
}

data class RetentionSetting(
    val category: RetentionCategory,
    val period: RetentionPeriod,
)

// ---------------------------------------------------------------------
// Custom context designer
// ---------------------------------------------------------------------
data class CustomContextDefinition(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val relatedSignalKeys: List<String>,
    val actions: List<RuleAction> = emptyList(),
    val minConfidence: Float = 0.6f,
)

// ---------------------------------------------------------------------
// Simulation mode
// ---------------------------------------------------------------------
data class SimulationScenario(
    val name: String,
    val context: NormalizedContext,
)
