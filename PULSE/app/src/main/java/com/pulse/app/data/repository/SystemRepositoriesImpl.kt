package com.pulse.app.data.repository

import com.pulse.app.database.dao.*
import com.pulse.app.database.entities.*
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.*
import com.pulse.app.permissions.PermissionManager
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val dao: PermissionStateDao,
    private val permissionManager: PermissionManager,
) : PermissionRepository {

    override val permissionStates: Flow<List<PermissionFeatureState>> = dao.observeAll().map { entities ->
        val map = entities.associateBy { it.permission }
        PulsePermission.entries.map { perm ->
            val entity = map[perm.name]
            PermissionFeatureState(
                permission = perm,
                status = entity?.osStatus?.let { runCatching { PermissionStatus.valueOf(it) }.getOrNull() }
                    ?: permissionManager.osStatus(perm),
                featureEnabled = entity?.featureEnabled ?: false,
            )
        }
    }

    override suspend fun setFeatureEnabled(permission: PulsePermission, enabled: Boolean) {
        dao.upsert(
            PermissionStateEntity(
                permission = permission.name,
                osStatus = permissionManager.osStatus(permission).name,
                featureEnabled = enabled,
            ),
        )
    }

    override suspend fun refreshOsGrantedStatus() {
        PulsePermission.entries.forEach { perm ->
            val current = dao.observeAll().first().firstOrNull { it.permission == perm.name }
            dao.upsert(
                PermissionStateEntity(
                    permission = perm.name,
                    osStatus = permissionManager.osStatus(perm).name,
                    featureEnabled = current?.featureEnabled ?: false,
                ),
            )
        }
    }
}

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val contextEventDao: ContextEventDao,
    private val modeDao: ModeDao,
    private val notificationRecordDao: NotificationRecordDao,
) : AnalyticsRepository {

    override suspend fun summary(period: AnalyticsPeriod, at: Instant): AnalyticsSummary {
        val start = when (period) {
            AnalyticsPeriod.DAILY -> at.truncatedTo(ChronoUnit.DAYS)
            AnalyticsPeriod.WEEKLY -> at.minus(7, ChronoUnit.DAYS)
            AnalyticsPeriod.MONTHLY -> at.minus(30, ChronoUnit.DAYS)
        }
        val events = contextEventDao.between(start.toEpochMilli(), at.toEpochMilli())
        val byType = events.groupBy { it.type }
        val timeByContext = byType.mapNotNull { (typeName, list) ->
            val type = runCatching { ContextType.valueOf(typeName) }.getOrNull() ?: return@mapNotNull null
            val minutes = list.sumOf { e ->
                val end = e.endedAtEpochMs ?: at.toEpochMilli()
                ((end - e.startedAtEpochMs) / 60000).coerceAtLeast(0)
            }.toInt()
            type to minutes
        }.toMap()

        val focusMinutes = (timeByContext[ContextType.FOCUS] ?: 0) + (timeByContext[ContextType.STUDY] ?: 0)
        val notifications = notificationRecordDao.between(start.toEpochMilli(), at.toEpochMilli())
        val activationLog = modeDao.observeActivationLog().first().filter { it.startedAt in start.toEpochMilli()..at.toEpochMilli() }

        return AnalyticsSummary(
            period = period,
            focusMinutes = focusMinutes,
            distractionCount = notifications.count { !it.deferred && it.priorityBucket == "HIGH" },
            notificationLoad = notifications.size,
            modeSwitches = activationLog.size,
            activeHours = timeByContext.values.sum() / 60,
            timeByContext = timeByContext,
        )
    }
}

@Singleton
class RetentionRepositoryImpl @Inject constructor(
    private val preferenceDao: PreferenceDao,
    private val contextEventDao: ContextEventDao,
    private val usageRecordDao: UsageRecordDao,
    private val notificationRecordDao: NotificationRecordDao,
    private val executionLogDao: ExecutionLogDao,
    private val deviceContextDao: DeviceContextDao,
) : RetentionRepository {

    override val settings: Flow<List<RetentionSetting>> = preferenceDao.observeAll().map { prefs ->
        val map = prefs.associate { it.key to it.value }
        RetentionCategory.entries.map { category ->
            val stored = map["retention_${category.name}"]
            RetentionSetting(category, stored?.let { runCatching { RetentionPeriod.valueOf(it) }.getOrNull() } ?: RetentionPeriod.THIRTY_DAYS)
        }
    }

    override suspend fun setPeriod(category: RetentionCategory, period: RetentionPeriod) {
        preferenceDao.set(PreferenceEntity("retention_${category.name}", period.name))
    }

    override suspend fun clearNow(category: RetentionCategory) {
        when (category) {
            RetentionCategory.CONTEXT_HISTORY -> contextEventDao.deleteOlderThan(Instant.now().toEpochMilli())
            RetentionCategory.USAGE_ANALYTICS -> usageRecordDao.clearAll()
            RetentionCategory.NOTIFICATION_METADATA -> notificationRecordDao.deleteOlderThan(Instant.now().toEpochMilli())
            RetentionCategory.EVENT_LOGS -> executionLogDao.deleteOlderThan(Instant.now().toEpochMilli())
        }
    }

    override suspend fun runScheduledCleanup() {
        val current = settings.first()
        current.forEach { setting ->
            val days = setting.period.days ?: return@forEach
            val cutoff = Instant.now().minus(days.toLong(), ChronoUnit.DAYS).toEpochMilli()
            when (setting.category) {
                RetentionCategory.CONTEXT_HISTORY -> contextEventDao.deleteOlderThan(cutoff)
                RetentionCategory.USAGE_ANALYTICS -> usageRecordDao.deleteOlderThan(cutoff)
                RetentionCategory.NOTIFICATION_METADATA -> notificationRecordDao.deleteOlderThan(cutoff)
                RetentionCategory.EVENT_LOGS -> executionLogDao.deleteOlderThan(cutoff)
            }
        }
        deviceContextDao.deleteOlderThan(Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli())
    }
}

@Singleton
class LearningRepositoryImpl @Inject constructor(
    private val dao: FeedbackDao,
) : LearningRepository {

    override suspend fun weightFor(contextType: ContextType): ContextLearningWeight {
        val entity = dao.weightFor(contextType.name)
        return entity?.toDomain(contextType) ?: ContextLearningWeight(contextType)
    }

    override suspend fun recordFeedback(contextType: ContextType, feedback: ContextFeedback) {
        dao.insert(FeedbackEntity(contextType = contextType.name, feedback = feedback.name, recordedAt = System.currentTimeMillis()))
        val current = dao.weightFor(contextType.name) ?: LearningWeightEntity(contextType.name, 0, 0, 0f)
        val step = com.pulse.app.context.LearningEngine.STEP
        val updated = when (feedback) {
            ContextFeedback.CONFIRMED -> current.copy(
                confirmCount = current.confirmCount + 1,
                confidenceThresholdAdjustment = (current.confidenceThresholdAdjustment - step)
                    .coerceAtLeast(com.pulse.app.context.LearningEngine.MIN_BOUND - com.pulse.app.context.ContextInferenceEngine.BASE_MIN_CONFIDENCE),
            )
            ContextFeedback.REJECTED -> current.copy(
                rejectCount = current.rejectCount + 1,
                confidenceThresholdAdjustment = (current.confidenceThresholdAdjustment + step)
                    .coerceAtMost(com.pulse.app.context.LearningEngine.MAX_BOUND - com.pulse.app.context.ContextInferenceEngine.BASE_MIN_CONFIDENCE),
            )
            ContextFeedback.CORRECTED -> current.copy(rejectCount = current.rejectCount + 1)
        }
        dao.upsertWeight(updated)
    }

    override suspend fun allWeights(): List<ContextLearningWeight> =
        dao.allWeights().mapNotNull { entity ->
            runCatching { ContextType.valueOf(entity.contextType) }.getOrNull()?.let { entity.toDomain(it) }
        }

    private fun LearningWeightEntity.toDomain(type: ContextType) =
        ContextLearningWeight(type, confirmCount, rejectCount, confidenceThresholdAdjustment)
}

/**
 * Recommendations are ephemeral by design (they are re-derived on every
 * evaluation tick, not a durable record the user would expect to survive
 * an app restart) — kept as an in-memory hot flow rather than a Room table.
 */
@Singleton
class RecommendationRepositoryImpl @Inject constructor() : RecommendationRepository {
    private val _pending = MutableStateFlow<List<Recommendation>>(emptyList())
    override val pending: Flow<List<Recommendation>> = _pending.asStateFlow()

    override suspend fun push(recommendation: Recommendation) {
        _pending.update { current -> (current.filterNot { it.titleKey == recommendation.titleKey } + recommendation) }
    }

    override suspend fun resolve(id: String, decision: RecommendationDecision) {
        _pending.update { current -> current.filterNot { it.id == id } }
    }
}
