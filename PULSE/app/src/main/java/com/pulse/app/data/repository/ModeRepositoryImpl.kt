package com.pulse.app.data.repository

import com.pulse.app.core.util.JsonCodec
import com.pulse.app.database.dao.ModeDao
import com.pulse.app.database.dao.SessionDao
import com.pulse.app.database.entities.ModeActivationEntity
import com.pulse.app.database.entities.ModeEntity
import com.pulse.app.database.entities.SessionEntity
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.ModeRepository
import com.pulse.app.modes.PredefinedModes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModeRepositoryImpl @Inject constructor(
    private val modeDao: ModeDao,
    private val sessionDao: SessionDao,
) : ModeRepository {

    override val modes: Flow<List<PulseMode>> = modeDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    override val activeMode: Flow<PulseMode?> = modeDao.observeActive().map { it?.toDomain() }
    override val activationHistory: Flow<List<ModeActivationRecord>> = modeDao.observeActivationLog().map { list -> list.map { it.toDomain() } }
    override val focusSessions: Flow<List<FocusSession>> = sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun seedDefaultsIfEmpty() {
        val existing = modeDao.observeAll().first()
        if (existing.isNotEmpty()) return
        PredefinedModes.defaults().forEach { saveMode(it) }
    }

    override suspend fun getMode(id: String): PulseMode? = modeDao.get(id)?.toDomain()

    override suspend fun saveMode(mode: PulseMode) {
        modeDao.upsert(
            ModeEntity(
                id = mode.id,
                type = mode.type.name,
                name = mode.name,
                isBuiltIn = mode.isBuiltIn,
                actionsJson = JsonCodec.encode(mode.actions),
                scheduleJson = JsonCodec.encode(SerializableSchedule.from(mode.schedule)),
                triggeringContextsJson = JsonCodec.encode(mode.triggeringContexts.map { it.name }),
                isActive = mode.isActive,
                activatedAtEpochMs = mode.activatedAt?.toEpochMilli(),
                allowedAppsJson = JsonCodec.encode(mode.allowedApps),
                minConfidenceToSuggest = mode.minConfidenceToSuggest,
            ),
        )
    }

    override suspend fun deleteMode(id: String) = modeDao.delete(id)

    override suspend fun activate(modeId: String, triggeredBy: String) {
        modeDao.deactivateAll()
        val now = System.currentTimeMillis()
        modeDao.activate(modeId, now)
        val mode = modeDao.get(modeId) ?: return
        modeDao.insertActivationLog(
            ModeActivationEntity(
                modeId = modeId,
                modeType = mode.type,
                startedAt = now,
                endedAt = null,
                triggeredBy = triggeredBy,
                notificationsDeferred = 0,
                distractionsDetected = 0,
            ),
        )
    }

    override suspend fun deactivate(modeId: String) {
        modeDao.deactivate(modeId)
    }

    override suspend fun startFocusSession(session: FocusSession) {
        sessionDao.upsert(
            SessionEntity(
                id = session.id,
                goal = session.goal,
                subject = session.subject,
                plannedMinutes = session.plannedMinutes,
                allowedAppsJson = JsonCodec.encode(session.allowedApps),
                startedAt = session.startedAt.toEpochMilli(),
                endedAt = null,
                focusedMinutes = 0,
                distractionEvents = 0,
                deferredNotifications = 0,
            ),
        )
    }

    override suspend fun endFocusSession(id: String, focusedMinutes: Int, distractions: Int, deferred: Int) {
        val existing = sessionDao.get(id) ?: return
        sessionDao.upsert(
            existing.copy(
                endedAt = System.currentTimeMillis(),
                focusedMinutes = focusedMinutes,
                distractionEvents = distractions,
                deferredNotifications = deferred,
            ),
        )
    }

    private fun ModeEntity.toDomain() = PulseMode(
        id = id,
        type = runCatching { ModeType.valueOf(type) }.getOrDefault(ModeType.CUSTOM),
        name = name,
        isBuiltIn = isBuiltIn,
        actions = JsonCodec.decode(actionsJson, emptyList()),
        schedule = JsonCodec.decode(scheduleJson, SerializableSchedule()).toDomain(),
        triggeringContexts = JsonCodec.decode<List<String>>(triggeringContextsJson, emptyList())
            .mapNotNull { runCatching { ContextType.valueOf(it) }.getOrNull() },
        isActive = isActive,
        activatedAt = activatedAtEpochMs?.let { Instant.ofEpochMilli(it) },
        allowedApps = JsonCodec.decode(allowedAppsJson, emptyList()),
        minConfidenceToSuggest = minConfidenceToSuggest,
    )

    private fun ModeActivationEntity.toDomain() = ModeActivationRecord(
        id = id,
        modeId = modeId,
        modeType = runCatching { ModeType.valueOf(modeType) }.getOrDefault(ModeType.CUSTOM),
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        triggeredBy = triggeredBy,
        notificationsDeferred = notificationsDeferred,
        distractionsDetected = distractionsDetected,
    )

    private fun SessionEntity.toDomain() = FocusSession(
        id = id,
        goal = goal,
        subject = subject,
        plannedMinutes = plannedMinutes,
        allowedApps = JsonCodec.decode(allowedAppsJson, emptyList()),
        startedAt = Instant.ofEpochMilli(startedAt),
        endedAt = endedAt?.let { Instant.ofEpochMilli(it) },
        focusedMinutes = focusedMinutes,
        distractionEvents = distractionEvents,
        deferredNotifications = deferredNotifications,
    )
}
