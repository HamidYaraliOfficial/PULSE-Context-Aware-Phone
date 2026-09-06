package com.pulse.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

/**
 * On Device Reboot, PULSE only re-registers the low-frequency WorkManager
 * backstop (see [ContextEvaluationWorker]) — it deliberately does NOT
 * restart [PulseForegroundService] automatically, since that service is an
 * explicit, visible opt-in the user turns on from within the app.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        WorkScheduler.scheduleBackstop(context)
    }
}

object WorkScheduler {
    fun scheduleBackstop(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        val request = PeriodicWorkRequestBuilder<ContextEvaluationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            ContextEvaluationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
