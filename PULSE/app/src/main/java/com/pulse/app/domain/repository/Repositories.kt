package com.pulse.app.domain.repository

import com.pulse.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ContextRepository {
    val currentContext: Flow<ContextState?>
    val timeline: Flow<List<ContextState>>
    suspend fun recordContext(state: ContextState)
    suspend fun applyFeedback(contextId: String, feedback: ContextFeedback)
    suspend fun updateEvent(state: ContextState)
    suspend fun deleteEvent(contextId: String)
    suspend fun timelineBetween(start: Instant, end: Instant): List<ContextState>
    suspend fun customContexts(): List<CustomContextDefinition>
    suspend fun saveCustomContext(definition: CustomContextDefinition)
    suspend fun deleteCustomContext(id: String)
}

interface RuleRepository {
    val rules: Flow<List<AutomationRule>>
    suspend fun rulesSnapshot(): List<AutomationRule>
    suspend fun getRule(id: String): AutomationRule?
    suspend fun saveRule(rule: AutomationRule)
    suspend fun deleteRule(id: String)
    suspend fun setEnabled(id: String, enabled: Boolean)
    val executionHistory: Flow<List<RuleExecutionRecord>>
    suspend fun recordExecution(record: RuleExecutionRecord)
    suspend fun lastExecutionFor(ruleId: String): RuleExecutionRecord?
}

interface ModeRepository {
    val modes: Flow<List<PulseMode>>
    val activeMode: Flow<PulseMode?>
    suspend fun getMode(id: String): PulseMode?
    suspend fun saveMode(mode: PulseMode)
    suspend fun deleteMode(id: String)
    suspend fun activate(modeId: String, triggeredBy: String)
    suspend fun deactivate(modeId: String)
    val activationHistory: Flow<List<ModeActivationRecord>>

    val focusSessions: Flow<List<FocusSession>>
    suspend fun startFocusSession(session: FocusSession)
    suspend fun endFocusSession(id: String, focusedMinutes: Int, distractions: Int, deferred: Int)
}

interface UsageRepository {
    suspend fun recordUsage(record: AppUsageRecord)
    suspend fun usageForWindow(start: Instant, end: Instant): List<AppUsageRecord>
    suspend fun detectedPatterns(): List<UsagePatternInsight>
    suspend fun clearAllUsageHistory()
    val usageTrackingEnabled: Flow<Boolean>
    suspend fun setUsageTrackingEnabled(enabled: Boolean)
}

interface NotificationEngineRepository {
    suspend fun recordNotification(record: NotificationRecordModel)
    suspend fun deferredSince(context: ContextType, since: Instant): List<NotificationRecordModel>
    suspend fun buildDigest(context: ContextType, periodStart: Instant, periodEnd: Instant): NotificationDigest
    val recentDigests: Flow<List<NotificationDigest>>
}

interface PermissionRepository {
    val permissionStates: Flow<List<PermissionFeatureState>>
    suspend fun setFeatureEnabled(permission: PulsePermission, enabled: Boolean)
    suspend fun refreshOsGrantedStatus()
}

interface AnalyticsRepository {
    suspend fun summary(period: AnalyticsPeriod, at: Instant = Instant.now()): AnalyticsSummary
}

interface RetentionRepository {
    val settings: Flow<List<RetentionSetting>>
    suspend fun setPeriod(category: RetentionCategory, period: RetentionPeriod)
    suspend fun clearNow(category: RetentionCategory)
    suspend fun runScheduledCleanup()
}

interface LearningRepository {
    suspend fun weightFor(contextType: ContextType): ContextLearningWeight
    suspend fun recordFeedback(contextType: ContextType, feedback: ContextFeedback)
    suspend fun allWeights(): List<ContextLearningWeight>
}

interface RecommendationRepository {
    val pending: Flow<List<Recommendation>>
    suspend fun push(recommendation: Recommendation)
    suspend fun resolve(id: String, decision: RecommendationDecision)
}
