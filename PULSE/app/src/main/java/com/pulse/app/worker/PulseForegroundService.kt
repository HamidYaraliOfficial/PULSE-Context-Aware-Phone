package com.pulse.app.worker

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pulse.app.MainActivity
import com.pulse.app.context.ContextEngine
import com.pulse.app.context.signals.BatterySignalProvider
import com.pulse.app.rules.RuleEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Optional real-time evaluation loop, started only when the user turns on
 * "Live Context Detection" from Quick Actions / Settings → Battery. It is
 * NOT auto-started on boot and NOT kept alive indefinitely by PULSE itself
 * — it is a plain foreground service the user can stop at any time from the
 * persistent notification, satisfying "no permanent, unnecessary background
 * service". The loop interval adapts via [BatteryAwareScheduler].
 */
@AndroidEntryPoint
class PulseForegroundService : Service() {

    @Inject lateinit var contextEngine: ContextEngine
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var batterySignalProvider: BatterySignalProvider
    @Inject lateinit var batteryAwareScheduler: BatteryAwareScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        loopJob = scope.launch {
            batterySignalProvider.observe().collectLatest { battery ->
                val plan = batteryAwareScheduler.plan(battery)
                while (isActive) {
                    contextEngine.evaluate()
                    ruleEngine.onTick(contextEngine.snapshot.value, contextEngine.currentContext.value)
                    delay(plan.evaluationIntervalMs)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("PULSE is active")
            .setContentText("Detecting context in real time")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID, "PULSE Live Detection", android.app.NotificationManager.IMPORTANCE_LOW,
        )
        (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "pulse_foreground"
        private const val NOTIFICATION_ID = 9001
    }
}
