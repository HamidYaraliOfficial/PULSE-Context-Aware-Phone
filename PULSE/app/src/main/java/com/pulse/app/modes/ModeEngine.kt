package com.pulse.app.modes

import com.pulse.app.domain.model.ModeActivationRecord
import com.pulse.app.domain.model.PulseMode
import com.pulse.app.domain.repository.ModeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mode Engine. Activating a Mode applies its entire action bundle through
 * [ModeActionExecutor] in one shot (DND, volume, brightness, screen
 * timeout…); deactivating reverts what it reasonably can (currently: DND
 * back to ALL — volume/brightness are intentionally left as the user set
 * them, since silently reverting a value the user may have manually
 * changed mid-Mode would be surprising). Only one Mode is active at a time
 * — activating a new one deactivates whichever was running, mirroring how
 * Focus/DND-style modes work system-wide.
 */
@Singleton
class ModeEngine @Inject constructor(
    private val modeRepository: ModeRepository,
    private val actionExecutor: ModeActionExecutor,
) {
    suspend fun activate(modeId: String, triggeredBy: String = "manual") {
        val mode = modeRepository.getMode(modeId) ?: return
        modeRepository.activate(modeId, triggeredBy)
        mode.actions.forEach { action -> actionExecutor.execute(action) }
    }

    suspend fun deactivate(modeId: String) {
        val mode = modeRepository.getMode(modeId) ?: return
        modeRepository.deactivate(modeId)
        // Revert interruption filter only — see class doc comment.
        actionExecutor.execute(
            com.pulse.app.domain.model.RuleAction(
                type = com.pulse.app.domain.model.ActionType.SET_DND,
                params = mapOf("filter" to "all"),
            ),
        )
    }

    suspend fun activeMode(): PulseMode? = modeRepository.activeMode.first()

    suspend fun recentActivations(limit: Int = 20): List<ModeActivationRecord> =
        modeRepository.activationHistory.first().take(limit)
}
