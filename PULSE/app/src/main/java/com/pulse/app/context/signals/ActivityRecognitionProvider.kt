package com.pulse.app.context.signals

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.pulse.app.domain.model.DetectedActivityType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ActivitySignal(val type: DetectedActivityType, val confidencePercent: Int)

/**
 * Wraps the Activity Recognition Transition/Result API. Sampling interval is
 * intentionally coarse (see [DEFAULT_INTERVAL_MS]) — the Battery-Aware
 * Scheduler widens this further under Battery Saver. Requires
 * android.permission.ACTIVITY_RECOGNITION at call time; PULSE never calls
 * [register] unless the Activity Recognition Privacy Toggle is on AND the OS
 * permission is granted (enforced by PermissionManager upstream).
 */
@Singleton
class ActivityRecognitionProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = ActivityRecognition.getClient(context)
    private var pendingIntent: PendingIntent? = null

    fun observe(intervalMs: Long = DEFAULT_INTERVAL_MS): Flow<ActivitySignal> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null || !ActivityRecognitionResult.hasResult(intent)) return
                val result = ActivityRecognitionResult.extractResult(intent) ?: return
                val most = result.mostProbableActivity
                trySend(ActivitySignal(mapActivity(most.type), most.confidence))
            }
        }

        val filter = IntentFilter(ACTION_ACTIVITY_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_ACTIVITY_UPDATE).setPackage(context.packageName)
        val pi = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        pendingIntent = pi

        runCatching {
            client.requestActivityUpdates(intervalMs, pi)
        }.onFailure { close(it) }

        awaitClose {
            pendingIntent?.let { client.removeActivityUpdates(it) }
            context.unregisterReceiver(receiver)
        }
    }

    private fun mapActivity(type: Int): DetectedActivityType = when (type) {
        DetectedActivity.STILL -> DetectedActivityType.STILL
        DetectedActivity.WALKING, DetectedActivity.ON_FOOT -> DetectedActivityType.WALKING
        DetectedActivity.RUNNING -> DetectedActivityType.RUNNING
        DetectedActivity.ON_BICYCLE -> DetectedActivityType.CYCLING
        DetectedActivity.IN_VEHICLE -> DetectedActivityType.DRIVING
        else -> DetectedActivityType.UNKNOWN
    }

    companion object {
        private const val ACTION_ACTIVITY_UPDATE = "com.pulse.app.ACTION_ACTIVITY_UPDATE"
        private const val REQUEST_CODE = 4201
        private const val DEFAULT_INTERVAL_MS = 60_000L
    }
}
