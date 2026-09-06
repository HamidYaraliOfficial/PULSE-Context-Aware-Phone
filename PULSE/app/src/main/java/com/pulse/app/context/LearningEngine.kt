package com.pulse.app.context

import com.pulse.app.domain.model.ContextFeedback
import com.pulse.app.domain.model.ContextType
import com.pulse.app.domain.repository.LearningRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device Learning System. Every Confirm nudges that context type's
 * effective minimum-confidence threshold down slightly (PULSE trusts its
 * own detection a bit more next time); every Reject nudges it up (PULSE
 * becomes more conservative). Adjustments are small, bounded, and fully
 * visible via [LearningRepository.allWeights] in Settings → Context
 * Detection, so the "learning" stays observable rather than opaque.
 */
@Singleton
class LearningEngine @Inject constructor(
    private val learningRepository: LearningRepository,
) {
    suspend fun recordFeedback(contextType: ContextType, feedback: ContextFeedback) {
        if (contextType == ContextType.CUSTOM || contextType == ContextType.UNKNOWN) return
        learningRepository.recordFeedback(contextType, feedback)
    }

    suspend fun currentThresholdOverrides(): Map<ContextType, Float> {
        val weights = learningRepository.allWeights()
        return weights.associate { weight ->
            val base = ContextInferenceEngine.BASE_MIN_CONFIDENCE
            val adjusted = (base + weight.confidenceThresholdAdjustment).coerceIn(MIN_BOUND, MAX_BOUND)
            weight.contextType to adjusted
        }
    }

    companion object {
        const val STEP = 0.02f
        const val MIN_BOUND = 0.35f
        const val MAX_BOUND = 0.85f
    }
}
