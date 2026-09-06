package com.pulse.app.context

import com.pulse.app.domain.model.*
import com.pulse.app.domain.repository.ContextRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Context Engine. Every signal provider writes into [snapshot] as data
 * arrives (see the various `context/signals/*`, `bluetooth/*`, `calendar/*`,
 * `location/*`, `usage/*` collectors wired up by [com.pulse.app.worker.ContextEvaluationWorker]).
 * [evaluate] then runs [ContextInferenceEngine] against the latest snapshot
 * and applies Minimum Duration / Hysteresis / Grace Period before ever
 * switching the publicly-observed [currentContext] — this is what stops
 * PULSE from flapping between Focus and Available every time a single
 * notification arrives, as required by the spec's Cooldown/Debounce System.
 */
@Singleton
class ContextEngine @Inject constructor(
    private val inferenceEngine: ContextInferenceEngine,
    private val contextRepository: ContextRepository,
    private val learningEngine: LearningEngine,
) {
    private val _snapshot = MutableStateFlow(
        NormalizedContext(
            timeOfDayMinutes = LocalTime.now().let { it.hour * 60 + it.minute },
            dayOfWeek = LocalDate.now().dayOfWeek.value,
        ),
    )
    val snapshot: StateFlow<NormalizedContext> = _snapshot.asStateFlow()

    private val _currentContext = MutableStateFlow<ContextState?>(null)
    val currentContext: StateFlow<ContextState?> = _currentContext.asStateFlow()

    private var pendingCandidateType: ContextType? = null
    private var pendingSince: Instant? = null

    fun updateSnapshot(transform: (NormalizedContext) -> NormalizedContext) {
        _snapshot.update(transform)
    }

    /**
     * Runs one inference tick. [minimumDurationSeconds] is the Grace Period:
     * a candidate context must keep winning for this long, across
     * consecutive evaluations, before PULSE actually switches to it.
     */
    suspend fun evaluate(minimumDurationSeconds: Long = DEFAULT_MIN_DURATION_SECONDS) {
        val overrides = learningEngine.currentThresholdOverrides()
        val candidate = inferenceEngine.infer(_snapshot.value, overrides)
        applyHysteresis(candidate, minimumDurationSeconds)
    }

    private suspend fun applyHysteresis(candidate: ContextState, minDurationSeconds: Long) {
        val current = _currentContext.value

        if (current != null && current.type == candidate.type) {
            // Same context continuing — just refresh confidence, no new Timeline row.
            _currentContext.value = current.copy(confidence = candidate.confidence, signals = candidate.signals)
            pendingCandidateType = null
            pendingSince = null
            return
        }

        if (pendingCandidateType != candidate.type) {
            pendingCandidateType = candidate.type
            pendingSince = Instant.now()
            return
        }

        val elapsed = Duration.between(pendingSince ?: Instant.now(), Instant.now()).seconds
        if (elapsed < minDurationSeconds) return

        // Commit the switch.
        current?.let { contextRepository.updateEvent(it.copy(endedAt = Instant.now())) }
        _currentContext.value = candidate
        contextRepository.recordContext(candidate)
        pendingCandidateType = null
        pendingSince = null
    }

    suspend fun applyUserFeedback(feedback: ContextFeedback) {
        val current = _currentContext.value ?: return
        contextRepository.applyFeedback(current.id, feedback)
        learningEngine.recordFeedback(current.type, feedback)
        if (feedback == ContextFeedback.REJECTED) {
            _currentContext.value = current.copy(feedback = feedback)
            pendingCandidateType = null
            pendingSince = null
        } else {
            _currentContext.value = current.copy(feedback = feedback)
        }
    }

    /** Used by Simulation Mode: bypasses hysteresis entirely for an immediate, disposable inference preview. */
    fun previewSimulated(context: NormalizedContext): ContextState =
        inferenceEngine.infer(context.copy(isSimulated = true))

    private companion object {
        const val DEFAULT_MIN_DURATION_SECONDS = 90L
    }
}
