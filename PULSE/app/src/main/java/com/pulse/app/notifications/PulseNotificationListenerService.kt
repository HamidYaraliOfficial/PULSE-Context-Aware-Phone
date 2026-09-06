package com.pulse.app.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pulse.app.context.ContextEngine
import com.pulse.app.domain.model.ContextType
import com.pulse.app.domain.model.NotificationPriorityBucket
import com.pulse.app.domain.model.NotificationRecordModel
import com.pulse.app.domain.repository.NotificationEngineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Bound only after the user explicitly grants Notification Access in the
 * Permission Center (an OS settings-page grant, never silently assumed).
 * Stores metadata only — package, category, priority bucket, timestamp —
 * never notification title/body text, matching the "keep only what a
 * feature needs" privacy rule.
 */
@AndroidEntryPoint
class PulseNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var notificationEngineRepository: NotificationEngineRepository
    @Inject lateinit var contextEngine: ContextEngine
    @Inject lateinit var smartNotificationEngine: SmartNotificationEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        scope.launch {
            val currentContext = contextEngine.currentContext.value
            val shouldDefer = smartNotificationEngine.shouldDefer(currentContext?.type, sbn.notification.priority)
            val record = NotificationRecordModel(
                packageName = sbn.packageName,
                category = sbn.notification.category,
                priorityBucket = bucketFor(sbn.notification.priority),
                postedAt = Instant.ofEpochMilli(sbn.postTime),
                deferred = shouldDefer,
                activeContext = currentContext?.type,
            )
            notificationEngineRepository.recordNotification(record)
            if (shouldDefer) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun bucketFor(priority: Int): NotificationPriorityBucket = when {
        priority >= android.app.Notification.PRIORITY_HIGH -> NotificationPriorityBucket.HIGH
        priority <= android.app.Notification.PRIORITY_LOW -> NotificationPriorityBucket.LOW
        else -> NotificationPriorityBucket.NORMAL
    }
}
