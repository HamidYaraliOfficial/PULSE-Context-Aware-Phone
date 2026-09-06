package com.pulse.app.data.repository

import com.pulse.app.core.util.JsonCodec
import com.pulse.app.database.dao.ContextEventDao
import com.pulse.app.database.dao.CustomContextDao
import com.pulse.app.database.entities.ContextEventEntity
import com.pulse.app.database.entities.CustomContextTypeEntity
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.ContextRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextRepositoryImpl @Inject constructor(
    private val dao: ContextEventDao,
    private val customContextDao: CustomContextDao,
) : ContextRepository {

    override val currentContext: Flow<ContextState?> = dao.observeCurrent().map { it?.toDomain() }
    override val timeline: Flow<List<ContextState>> = dao.observeTimeline().map { list -> list.map { it.toDomain() } }

    override suspend fun recordContext(state: ContextState) {
        dao.upsert(state.toEntity())
    }

    override suspend fun applyFeedback(contextId: String, feedback: ContextFeedback) {
        val existing = dao.getById(contextId) ?: return
        dao.update(existing.copy(feedback = feedback.name))
    }

    override suspend fun updateEvent(state: ContextState) {
        dao.update(state.toEntity())
    }

    override suspend fun deleteEvent(contextId: String) {
        dao.delete(contextId)
    }

    override suspend fun timelineBetween(start: Instant, end: Instant): List<ContextState> =
        dao.between(start.toEpochMilli(), end.toEpochMilli()).map { it.toDomain() }

    override suspend fun customContexts(): List<CustomContextDefinition> =
        customContextDao.all().map {
            CustomContextDefinition(
                id = it.id,
                label = it.label,
                relatedSignalKeys = JsonCodec.decode(it.relatedSignalKeysJson, emptyList()),
                actions = JsonCodec.decode(it.actionsJson, emptyList()),
                minConfidence = it.minConfidence,
            )
        }

    override suspend fun saveCustomContext(definition: CustomContextDefinition) {
        customContextDao.upsert(
            CustomContextTypeEntity(
                id = definition.id,
                label = definition.label,
                relatedSignalKeysJson = JsonCodec.encode(definition.relatedSignalKeys),
                actionsJson = JsonCodec.encode(definition.actions),
                minConfidence = definition.minConfidence,
            ),
        )
    }

    override suspend fun deleteCustomContext(id: String) = customContextDao.delete(id)

    private fun ContextState.toEntity() = ContextEventEntity(
        id = id,
        type = type.name,
        customLabel = customLabel,
        confidence = confidence,
        startedAtEpochMs = startedAt.toEpochMilli(),
        endedAtEpochMs = endedAt?.toEpochMilli(),
        signalsJson = JsonCodec.encode(signals),
        feedback = feedback?.name,
    )

    private fun ContextEventEntity.toDomain() = ContextState(
        id = id,
        type = runCatching { ContextType.valueOf(type) }.getOrDefault(ContextType.UNKNOWN),
        customLabel = customLabel,
        confidence = confidence,
        startedAt = Instant.ofEpochMilli(startedAtEpochMs),
        endedAt = endedAtEpochMs?.let { Instant.ofEpochMilli(it) },
        signals = JsonCodec.decode(signalsJson, emptyList()),
        feedback = feedback?.let { runCatching { ContextFeedback.valueOf(it) }.getOrNull() },
    )
}
