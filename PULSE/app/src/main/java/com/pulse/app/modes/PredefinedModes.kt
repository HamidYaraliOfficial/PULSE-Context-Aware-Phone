package com.pulse.app.modes

import com.pulse.app.domain.model.*

/**
 * Seeded on first launch (see data/repository/ModeRepositoryImpl). Every
 * value here is a starting point, not a hardcoded final answer — the user
 * can edit every Mode's actions and Active Hours from the Mode Detail
 * screen, including clearing the schedule entirely so it applies
 * context-only, exactly as requested.
 */
object PredefinedModes {

    fun defaults(): List<PulseMode> = listOf(
        PulseMode(
            type = ModeType.FOCUS,
            name = "Focus Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
            ),
            triggeringContexts = listOf(ContextType.FOCUS),
        ),
        PulseMode(
            type = ModeType.STUDY,
            name = "Study Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
                RuleAction(ActionType.SET_SCREEN_TIMEOUT, mapOf("seconds" to "120")),
            ),
            triggeringContexts = listOf(ContextType.STUDY),
        ),
        PulseMode(
            type = ModeType.WORK,
            name = "Work Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
            ),
            triggeringContexts = listOf(ContextType.WORK),
        ),
        PulseMode(
            type = ModeType.MEETING,
            name = "Meeting Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
                RuleAction(ActionType.SET_VOLUME, mapOf("percent" to "20")),
            ),
            triggeringContexts = listOf(ContextType.MEETING),
        ),
        PulseMode(
            type = ModeType.DRIVING,
            name = "Driving Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
                RuleAction(ActionType.SET_SCREEN_TIMEOUT, mapOf("seconds" to "15")),
            ),
            triggeringContexts = listOf(ContextType.DRIVING),
        ),
        PulseMode(
            type = ModeType.SLEEP,
            name = "Sleep Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "alarms")),
                RuleAction(ActionType.SET_BRIGHTNESS, mapOf("percent" to "5")),
            ),
            triggeringContexts = listOf(ContextType.SLEEPING),
        ),
        PulseMode(
            type = ModeType.WORKOUT,
            name = "Workout Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
            ),
            triggeringContexts = listOf(ContextType.EXERCISING),
        ),
        PulseMode(
            type = ModeType.TRAVEL,
            name = "Travel Mode",
            actions = emptyList(),
            triggeringContexts = listOf(ContextType.TRAVELING, ContextType.COMMUTING),
        ),
        PulseMode(
            type = ModeType.GAMING,
            name = "Gaming Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "priority")),
            ),
            triggeringContexts = listOf(ContextType.GAMING),
        ),
        PulseMode(
            type = ModeType.QUIET,
            name = "Quiet Mode",
            actions = listOf(
                RuleAction(ActionType.SET_DND, mapOf("filter" to "none")),
                RuleAction(ActionType.SET_VOLUME, mapOf("percent" to "0")),
            ),
            triggeringContexts = emptyList(),
        ),
    )
}
