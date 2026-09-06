package com.pulse.app.data.repository

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesDataStore
import android.content.Context
import com.pulse.app.database.dao.NotificationRecordDao
import com.pulse.app.database.dao.UsageRecordDao
import com.pulse.app.database.entities.NotificationRecordEntity
import com.pulse.app.database.entities.UsageRecordEntity
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.NotificationEngineRepository
import com.pulse.app.domain.repository.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.usagePrefsDataStore by preferencesDataStore(name = "pulse_usage_prefs")

@Singleton
class UsageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: UsageRecordDao,
) : UsageRepository {

    private val trackingKey = booleanPreferencesKey("usage_tracking_enabled")

    override val usageTrackingEnabled: Flow<Boolean> =
        context.usagePrefsDataStore.data.map { it[trackingKey] ?: true }

    override suspend fun setUsageTrackingEnabled(enabled: Boolean) {
        context.usagePrefsDataStore.edit { it[trackingKey] = enabled }
        if (!enabled) clearAllUsageHistory()
    }

    override suspend fun recordUsage(record: AppUsageRecord) {
        dao.insert(
            UsageRecordEntity(
                id = record.id,
                packageName = record.packageName,
                appLabel = record.appLabel,
                category = record.category.name,
                windowStart = record.windowStart.toEpochMilli(),
                windowEnd = record.windowEnd.toEpochMilli(),
                foregroundMillis = record.foregroundMillis,
            ),
        )
    }

    override suspend fun usageForWindow(start: Instant, end: Instant): List<AppUsageRecord> =
        dao.between(start.toEpochMilli(), end.toEpochMilli()).map {
            AppUsageRecord(
                id = it.id, packageName = it.packageName, appLabel = it.appLabel,
                category = runCatching { AppCategory.valueOf(it.category) }.getOrDefault(AppCategory.UNKNOWN),
                windowStart = Instant.ofEpochMilli(it.windowStart), windowEnd = Instant.ofEpochMilli(it.windowEnd),
                foregroundMillis = it.foregroundMillis,
            )
        }

    override suspend fun detectedPatterns(): List<UsagePatternInsight> {
        val recent = usageForWindow(Instant.now().minusSeconds(7 * 24 * 3600), Instant.now())
        val byCategory = recent.groupBy { it.category }
        return byCategory.filter { it.value.size >= 5 }.map { (category, records) ->
            UsagePatternInsight(
                descriptionKey = "pattern_frequent_category",
                descriptionParams = mapOf("category" to category.name, "count" to records.size.toString()),
                confidence = (records.size / 20f).coerceAtMost(1f),
                isEstimate = true,
            )
        }
    }

    override suspend fun clearAllUsageHistory() = dao.clearAll()
}

@Singleton
class NotificationEngineRepositoryImpl @Inject constructor(
    private val dao: NotificationRecordDao,
) : NotificationEngineRepository {

    private val _recentDigests = MutableSharedFlow<List<NotificationDigest>>(replay = 1)
    override val recentDigests: Flow<List<NotificationDigest>> = _recentDigests.asSharedFlow()
    private val digestCache = mutableListOf<NotificationDigest>()

    override suspend fun recordNotification(record: NotificationRecordModel) {
        dao.insert(
            NotificationRecordEntity(
                id = record.id, packageName = record.packageName, category = record.category,
                priorityBucket = record.priorityBucket.name, postedAt = record.postedAt.toEpochMilli(),
                deferred = record.deferred, activeContext = record.activeContext?.name,
            ),
        )
    }

    override suspend fun deferredSince(context: ContextType, since: Instant): List<NotificationRecordModel> =
        dao.deferredSince(context.name, since.toEpochMilli()).map {
            NotificationRecordModel(
                id = it.id, packageName = it.packageName, category = it.category,
                priorityBucket = runCatching { NotificationPriorityBucket.valueOf(it.priorityBucket) }.getOrDefault(NotificationPriorityBucket.NORMAL),
                postedAt = Instant.ofEpochMilli(it.postedAt), deferred = it.deferred,
                activeContext = it.activeContext?.let { c -> runCatching { ContextType.valueOf(c) }.getOrNull() },
            )
        }

    override suspend fun buildDigest(context: ContextType, periodStart: Instant, periodEnd: Instant): NotificationDigest {
        val deferred = deferredSince(context, periodStart).filter { it.postedAt.isBefore(periodEnd) }
        val topApps = deferred.groupingBy { it.packageName }.eachCount().entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        val digest = NotificationDigest(context, periodStart, periodEnd, deferred.size, topApps)
        digestCache.add(0, digest)
        _recentDigests.tryEmit(digestCache.take(20))
        return digest
    }
}
