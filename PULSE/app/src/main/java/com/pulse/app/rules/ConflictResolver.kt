package com.pulse.app.rules

import com.pulse.app.domain.model.ActionType
import com.pulse.app.domain.model.AutomationRule
import com.pulse.app.domain.model.ConditionNode

data class ConflictResolution(
    val allowedRules: List<AutomationRule>,
    val skippedRules: List<AutomationRule>,
)

/**
 * When more than one enabled rule fires in the same evaluation tick and
 * both want to control the same [ActionType] (e.g. two rules both setting
 * Volume, or one activating Focus Mode while another activates Meeting
 * Mode), exactly one must win. Resolution order: Priority (explicit user
 * setting) → Specificity (more Leaf conditions = more specific = wins) →
 * stable name ordering as a final, deterministic tie-break — this is
 * intentionally never random, so the same inputs always produce the same
 * outcome and Rule History stays explainable.
 */
object ConflictResolver {

    fun resolve(firingRules: List<AutomationRule>): ConflictResolution {
        if (firingRules.size <= 1) return ConflictResolution(firingRules, emptyList())

        val actionTypeOwners = mutableMapOf<ActionType, AutomationRule>()
        val skipped = mutableListOf<AutomationRule>()
        val allowed = mutableListOf<AutomationRule>()

        // Higher priority (and, on ties, higher specificity) is evaluated first
        // so it claims contested action types before lower-priority rules do.
        val ordered = firingRules.sortedWith(
            compareByDescending<AutomationRule> { it.priority }
                .thenByDescending { specificity(it) }
                .thenBy { it.name },
        )

        for (rule in ordered) {
            val ruleActionTypes = rule.actions.map { it.type }.toSet()
            val contested = ruleActionTypes.any { actionTypeOwners.containsKey(it) }
            if (contested) {
                skipped += rule
            } else {
                ruleActionTypes.forEach { actionTypeOwners[it] = rule }
                allowed += rule
            }
        }

        return ConflictResolution(allowed, skipped)
    }

    private fun specificity(rule: AutomationRule): Int = countLeaves(rule.condition)

    private fun countLeaves(node: ConditionNode?): Int = when (node) {
        null -> 0
        is ConditionNode.Leaf -> 1
        is ConditionNode.And -> node.children.sumOf { countLeaves(it) }
        is ConditionNode.Or -> node.children.sumOf { countLeaves(it) }
        is ConditionNode.Not -> countLeaves(node.child)
    }
}
