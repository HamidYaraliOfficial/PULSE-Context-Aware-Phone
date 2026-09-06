package com.pulse.app.rules

import com.pulse.app.domain.model.ComparisonOperator
import com.pulse.app.domain.model.ConditionNode
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionEvaluatorTest {

    @Test
    fun `AND requires all children true`() {
        val node = ConditionNode.And(
            listOf(
                ConditionNode.Leaf("battery_level", ComparisonOperator.GREATER_THAN, "50"),
                ConditionNode.Leaf("is_charging", ComparisonOperator.EQUALS, "true"),
            ),
        )
        val factsTrue = mapOf("battery_level" to "80", "is_charging" to "true")
        val factsFalse = mapOf("battery_level" to "80", "is_charging" to "false")

        assertEquals(true, ConditionEvaluator.evaluate(node, factsTrue))
        assertEquals(false, ConditionEvaluator.evaluate(node, factsFalse))
    }

    @Test
    fun `OR requires any child true`() {
        val node = ConditionNode.Or(
            listOf(
                ConditionNode.Leaf("context_type", ComparisonOperator.EQUALS, "DRIVING"),
                ConditionNode.Leaf("context_type", ComparisonOperator.EQUALS, "COMMUTING"),
            ),
        )
        assertEquals(true, ConditionEvaluator.evaluate(node, mapOf("context_type" to "COMMUTING")))
        assertEquals(false, ConditionEvaluator.evaluate(node, mapOf("context_type" to "SLEEPING")))
    }

    @Test
    fun `NOT inverts child result`() {
        val node = ConditionNode.Not(ConditionNode.Leaf("headphones_connected", ComparisonOperator.EQUALS, "true"))
        assertEquals(true, ConditionEvaluator.evaluate(node, mapOf("headphones_connected" to "false")))
        assertEquals(false, ConditionEvaluator.evaluate(node, mapOf("headphones_connected" to "true")))
    }

    @Test
    fun `nested AND OR NOT tree evaluates correctly`() {
        // (driving AND NOT headphones) OR is_charging
        val node = ConditionNode.Or(
            listOf(
                ConditionNode.And(
                    listOf(
                        ConditionNode.Leaf("detected_activity", ComparisonOperator.EQUALS, "DRIVING"),
                        ConditionNode.Not(ConditionNode.Leaf("headphones_connected", ComparisonOperator.EQUALS, "true")),
                    ),
                ),
                ConditionNode.Leaf("is_charging", ComparisonOperator.EQUALS, "true"),
            ),
        )
        val facts = mapOf("detected_activity" to "DRIVING", "headphones_connected" to "false", "is_charging" to "false")
        assertEquals(true, ConditionEvaluator.evaluate(node, facts))
    }

    @Test
    fun `missing fact key evaluates leaf to false`() {
        val node = ConditionNode.Leaf("unknown_key", ComparisonOperator.EQUALS, "x")
        assertEquals(false, ConditionEvaluator.evaluate(node, emptyMap()))
    }

    @Test
    fun `null condition always evaluates true`() {
        assertEquals(true, ConditionEvaluator.evaluate(null, emptyMap()))
    }
}
