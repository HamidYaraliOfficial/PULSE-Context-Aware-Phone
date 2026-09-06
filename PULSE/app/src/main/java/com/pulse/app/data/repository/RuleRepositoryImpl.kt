package com.pulse.app.data.repository

import com.pulse.app.core.util.JsonCodec
import com.pulse.app.database.dao.ExecutionLogDao
import com.pulse.app.database.dao.RuleDao
import com.pulse.app.database.entities.*
import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: RuleDao,
    private val executionLogDao: ExecutionLogDao,
) : RuleRepository {

    override val rules: Flow<List<AutomationRule>> = ruleDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun rulesSnapshot(): List<AutomationRule> = rules.first()

    override suspend fun getRule(id: String): AutomationRule? = ruleDao.get(id)?.toDomain()

    override suspend fun saveRule(rule: AutomationRule) {
        ruleDao.upsert(
            RuleEntity(
                id = rule.id,
                name = rule.name,
                conditionJson = rule.condition?.let { JsonCodec.encode(it) },
                priority = rule.priority,
                cooldownMinutes = rule.cooldownMinutes,
                minimumDurationMinutes = rule.minimumDurationMinutes,
                scheduleJson = JsonCodec.encode(SerializableSchedule.from(rule.schedule)),
                exceptionsJson = JsonCodec.encode(rule.exceptions),
                enabled = rule.enabled,
                canvasLayoutJson = rule.canvasLayout,
            ),
        )
        ruleDao.deleteTriggersFor(rule.id)
        ruleDao.insertTriggers(listOf(TriggerEntity(ruleId = rule.id, type = rule.trigger.type.name, paramsJson = JsonCodec.encode(rule.trigger.params))))
        ruleDao.deleteActionsFor(rule.id)
        ruleDao.insertActions(
            rule.actions.mapIndexed { index, action ->
                ActionEntity(ruleId = rule.id, orderIndex = index, type = action.type.name, paramsJson = JsonCodec.encode(action.params))
            },
        )
    }

    override suspend fun deleteRule(id: String) = ruleDao.delete(id)
    override suspend fun setEnabled(id: String, enabled: Boolean) = ruleDao.setEnabled(id, enabled)

    override val executionHistory: Flow<List<RuleExecutionRecord>> = executionLogDao.observeRecent().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun recordExecution(record: RuleExecutionRecord) {
        executionLogDao.insert(
            ExecutionLogEntity(
                id = record.id,
                ruleId = record.ruleId,
                ruleName = record.ruleName,
                executedAt = record.executedAt.toEpochMilli(),
                activeContext = record.activeContext?.name,
                actionsPerformedJson = JsonCodec.encode(record.actionsPerformed.map { it.name }),
                result = record.result.name,
                detail = record.detail,
            ),
        )
    }

    override suspend fun lastExecutionFor(ruleId: String): RuleExecutionRecord? =
        executionLogDao.lastFor(ruleId)?.toDomain()

    private suspend fun RuleEntity.toDomain(): AutomationRule {
        val triggers = ruleDao.triggersFor(id)
        val actions = ruleDao.actionsFor(id)
        return AutomationRule(
            id = id,
            name = name,
            trigger = triggers.firstOrNull()?.let {
                Trigger(TriggerType.valueOf(it.type), JsonCodec.decode(it.paramsJson, emptyMap()))
            } ?: Trigger(TriggerType.MANUAL),
            condition = conditionJson?.let { JsonCodec.decode<ConditionNode?>(it, null) },
            actions = actions.map { RuleAction(ActionType.valueOf(it.type), JsonCodec.decode(it.paramsJson, emptyMap())) },
            priority = priority,
            cooldownMinutes = cooldownMinutes,
            minimumDurationMinutes = minimumDurationMinutes,
            schedule = JsonCodec.decode(scheduleJson, SerializableSchedule()).toDomain(),
            exceptions = JsonCodec.decode(exceptionsJson, emptyList()),
            enabled = enabled,
            canvasLayout = canvasLayoutJson,
        )
    }

    private fun ExecutionLogEntity.toDomain() = RuleExecutionRecord(
        id = id,
        ruleId = ruleId,
        ruleName = ruleName,
        executedAt = Instant.ofEpochMilli(executedAt),
        activeContext = activeContext?.let { runCatching { ContextType.valueOf(it) }.getOrNull() },
        actionsPerformed = JsonCodec.decode<List<String>>(actionsPerformedJson, emptyList()).mapNotNull {
            runCatching { ActionType.valueOf(it) }.getOrNull()
        },
        result = runCatching { ExecutionResult.valueOf(result) }.getOrDefault(ExecutionResult.FAILED),
        detail = detail,
    )
}
