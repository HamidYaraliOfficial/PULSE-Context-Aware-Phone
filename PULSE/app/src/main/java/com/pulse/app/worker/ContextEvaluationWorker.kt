package com.pulse.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pulse.app.context.ContextEngine
import com.pulse.app.context.signals.BatterySignalProvider
import com.pulse.app.context.signals.TimeSignalProvider
import com.pulse.app.data.repository.ModeRepositoryImpl
import com.pulse.app.domain.repository.RetentionRepository
import com.pulse.app.rules.RuleEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Low-frequency (WorkManager's own 15-minute floor) backstop evaluation.
 * This is what keeps Sleep suggestions, schedule-only rules and retention
 * cleanup ticking over even when [com.pulse.app.worker.PulseForegroundService]
 * is not running — PULSE deliberately does not require a permanent
 * foreground service to function, per the spec's event-driven requirement.
 */
@HiltWorker
class ContextEvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contextEngine: ContextEngine,
    private val ruleEngine: RuleEngine,
    private val timeSignalProvider: TimeSignalProvider,
    private val batterySignalProvider: BatterySignalProvider,
    private val retentionRepository: RetentionRepository,
    private val modeRepositoryImpl: ModeRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            modeRepositoryImpl.seedDefaultsIfEmpty()

            val time = timeSignalProvider.snapshot()
            val battery = runCatching { batterySignalProvider.observe().first() }.getOrNull()

            contextEngine.updateSnapshot { current ->
                current.copy(
                    timeOfDayMinutes = time.minutesSinceMidnight,
                    dayOfWeek = time.isoDayOfWeek,
                    isCharging = battery?.isCharging ?: current.isCharging,
                    batteryLevel = battery?.level ?: current.batteryLevel,
                    isBatterySaver = battery?.isPowerSaveMode ?: current.isBatterySaver,
                )
            }

            contextEngine.evaluate()
            ruleEngine.onTick(contextEngine.snapshot.value, contextEngine.currentContext.value)
            retentionRepository.runScheduledCleanup()

            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "pulse_context_evaluation"
    }
}
