package com.pulse.app.notifications

import com.pulse.app.domain.model.ContextType
import com.pulse.app.domain.model.NotificationDigest
import com.pulse.app.domain.repository.NotificationEngineRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-context deferral policy. Deliberately simple and inspectable: each
 * active Context maps to a minimum Android notification priority that is
 * allowed straight through; anything below it is deferred and rolled into
 * that context's [NotificationDigest] instead of interrupting the user —
 * this is the "Study only shows essential notifications, Sleep defers
 * everything non-urgent, Meeting prioritizes important ones" behaviour.
 */
@Singleton
class SmartNotificationEngine @Inject constructor(
    private val notificationEngineRepository: NotificationEngineRepository,
) {
    private val minAllowedPriority: Map<ContextType, Int> = mapOf(
        ContextType.SLEEPING to android.app.Notification.PRIORITY_MAX + 1, // effectively "nothing" except alarms (handled by DND ALARMS filter, not here)
        ContextType.STUDY to android.app.Notification.PRIORITY_HIGH,
        ContextType.FOCUS to android.app.Notification.PRIORITY_HIGH,
        ContextType.MEETING to android.app.Notification.PRIORITY_HIGH,
        ContextType.DRIVING to android.app.Notification.PRIORITY_HIGH,
    )

    fun shouldDefer(context: ContextType?, priority: Int): Boolean {
        val threshold = context?.let { minAllowedPriority[it] } ?: return false
        return priority < threshold
    }

    suspend fun buildDigestForSession(context: ContextType, periodStart: Instant, periodEnd: Instant = Instant.now()): NotificationDigest =
        notificationEngineRepository.buildDigest(context, periodStart, periodEnd)
}
