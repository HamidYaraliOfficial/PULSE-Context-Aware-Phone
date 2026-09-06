package com.pulse.app.rules

import com.pulse.app.domain.model.ActionType
import com.pulse.app.domain.model.AutomationRule
import com.pulse.app.domain.model.RuleAction
import com.pulse.app.domain.model.Trigger
import com.pulse.app.domain.model.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictResolverTest {

    private fun rule(name: String, priority: Int, actionType: ActionType) = AutomationRule(
        name = name,
        trigger = Trigger(TriggerType.MANUAL),
        actions = listOf(RuleAction(actionType)),
        priority = priority,
    )

    @Test
    fun `higher priority rule wins a contested action type`() {
        val low = rule("Low", priority = 1, actionType = ActionType.SET_VOLUME)
        val high = rule("High", priority = 10, actionType = ActionType.SET_VOLUME)

        val resolution = ConflictResolver.resolve(listOf(low, high))

        assertEquals(1, resolution.allowedRules.size)
        assertEquals("High", resolution.allowedRules.first().name)
        assertEquals(1, resolution.skippedRules.size)
        assertEquals("Low", resolution.skippedRules.first().name)
    }

    @Test
    fun `non-conflicting rules both proceed`() {
        val volumeRule = rule("Volume", priority = 1, actionType = ActionType.SET_VOLUME)
        val dndRule = rule("DND", priority = 1, actionType = ActionType.SET_DND)

        val resolution = ConflictResolver.resolve(listOf(volumeRule, dndRule))

        assertEquals(2, resolution.allowedRules.size)
        assertTrue(resolution.skippedRules.isEmpty())
    }

    @Test
    fun `single firing rule always proceeds`() {
        val onlyRule = rule("Solo", priority = 0, actionType = ActionType.VIBRATE)
        val resolution = ConflictResolver.resolve(listOf(onlyRule))
        assertEquals(1, resolution.allowedRules.size)
        assertTrue(resolution.skippedRules.isEmpty())
    }
}
