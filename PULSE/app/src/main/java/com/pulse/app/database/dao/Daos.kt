package com.pulse.app.database.dao

import androidx.room.*
import com.pulse.app.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextEventDao {
    @Query("SELECT * FROM context_events WHERE ignored = 0 ORDER BY startedAtEpochMs DESC")
    fun observeTimeline(): Flow<List<ContextEventEntity>>

    @Query("SELECT * FROM context_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContextEventEntity?

    @Query("SELECT * FROM context_events WHERE endedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeCurrent(): Flow<ContextEventEntity?>

    @Query("SELECT * FROM context_events WHERE startedAtEpochMs BETWEEN :start AND :end ORDER BY startedAtEpochMs ASC")
    suspend fun between(start: Long, end: Long): List<ContextEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContextEventEntity)

    @Update
    suspend fun update(entity: ContextEventEntity)

    @Query("DELETE FROM context_events WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM context_events WHERE startedAtEpochMs < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface CustomContextDao {
    @Query("SELECT * FROM custom_context_types")
    suspend fun all(): List<CustomContextTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomContextTypeEntity)

    @Query("DELETE FROM custom_context_types WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ModeDao {
    @Query("SELECT * FROM modes ORDER BY name ASC")
    fun observeAll(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<ModeEntity?>

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun get(id: String): ModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModeEntity)

    @Query("DELETE FROM modes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE modes SET isActive = 0, activatedAtEpochMs = NULL WHERE isActive = 1")
    suspend fun deactivateAll()

    @Query("UPDATE modes SET isActive = 1, activatedAtEpochMs = :atEpochMs WHERE id = :id")
    suspend fun activate(id: String, atEpochMs: Long)

    @Query("UPDATE modes SET isActive = 0, activatedAtEpochMs = NULL WHERE id = :id")
    suspend fun deactivate(id: String)

    @Insert
    suspend fun insertActivationLog(entity: ModeActivationEntity)

    @Query("SELECT * FROM mode_activation_log ORDER BY startedAt DESC")
    fun observeActivationLog(): Flow<List<ModeActivationEntity>>
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun get(id: String): SessionEntity?
}

@Dao
interface LocationContextDao {
    @Query("SELECT * FROM location_contexts")
    suspend fun all(): List<LocationContextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocationContextEntity)

    @Query("DELETE FROM location_contexts WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DeviceContextDao {
    @Insert
    suspend fun insert(entity: DeviceContextEntity)

    @Query("SELECT * FROM device_context_snapshots ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<DeviceContextEntity>

    @Query("DELETE FROM device_context_snapshots WHERE timestamp < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface CalendarContextDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CalendarContextEntity)

    @Query("SELECT * FROM calendar_contexts WHERE startTime <= :now AND endTime >= :now LIMIT 1")
    suspend fun activeAt(now: Long): CalendarContextEntity?

    @Query("DELETE FROM calendar_contexts WHERE endTime < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY priority DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun get(id: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("SELECT * FROM triggers WHERE ruleId = :ruleId")
    suspend fun triggersFor(ruleId: String): List<TriggerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggers(triggers: List<TriggerEntity>)

    @Query("DELETE FROM triggers WHERE ruleId = :ruleId")
    suspend fun deleteTriggersFor(ruleId: String)

    @Query("SELECT * FROM actions WHERE ruleId = :ruleId ORDER BY orderIndex ASC")
    suspend fun actionsFor(ruleId: String): List<ActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<ActionEntity>)

    @Query("DELETE FROM actions WHERE ruleId = :ruleId")
    suspend fun deleteActionsFor(ruleId: String)
}

@Dao
interface ExecutionLogDao {
    @Insert
    suspend fun insert(entity: ExecutionLogEntity)

    @Query("SELECT * FROM execution_log ORDER BY executedAt DESC LIMIT 500")
    fun observeRecent(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_log WHERE ruleId = :ruleId ORDER BY executedAt DESC LIMIT 1")
    suspend fun lastFor(ruleId: String): ExecutionLogEntity?

    @Query("DELETE FROM execution_log WHERE executedAt < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface FeedbackDao {
    @Insert
    suspend fun insert(entity: FeedbackEntity)

    @Query("SELECT * FROM learning_weights WHERE contextType = :contextType")
    suspend fun weightFor(contextType: String): LearningWeightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(entity: LearningWeightEntity)

    @Query("SELECT * FROM learning_weights")
    suspend fun allWeights(): List<LearningWeightEntity>
}

@Dao
interface UsageRecordDao {
    @Insert
    suspend fun insert(entity: UsageRecordEntity)

    @Query("SELECT * FROM usage_records WHERE windowStart BETWEEN :start AND :end")
    suspend fun between(start: Long, end: Long): List<UsageRecordEntity>

    @Query("DELETE FROM usage_records")
    suspend fun clearAll()

    @Query("DELETE FROM usage_records WHERE windowStart < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface NotificationRecordDao {
    @Insert
    suspend fun insert(entity: NotificationRecordEntity)

    @Query("SELECT * FROM notification_records WHERE deferred = 1 AND activeContext = :context AND postedAt >= :since")
    suspend fun deferredSince(context: String, since: Long): List<NotificationRecordEntity>

    @Query("SELECT * FROM notification_records WHERE postedAt BETWEEN :start AND :end")
    suspend fun between(start: Long, end: Long): List<NotificationRecordEntity>

    @Query("DELETE FROM notification_records WHERE postedAt < :beforeEpochMs")
    suspend fun deleteOlderThan(beforeEpochMs: Long)
}

@Dao
interface PreferenceDao {
    @Query("SELECT value FROM preferences WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: PreferenceEntity)

    @Query("SELECT * FROM preferences")
    fun observeAll(): Flow<List<PreferenceEntity>>
}

@Dao
interface PermissionStateDao {
    @Query("SELECT * FROM permission_states")
    fun observeAll(): Flow<List<PermissionStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PermissionStateEntity)
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entity: AuditLogEntity)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT 500")
    fun observeRecent(): Flow<List<AuditLogEntity>>
}
