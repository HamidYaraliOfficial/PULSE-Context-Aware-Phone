package com.pulse.app.modes

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pulse.app.domain.model.ActionType
import com.pulse.app.domain.model.RuleAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ActionOutcome { SUCCESS, MISSING_PERMISSION, UNSUPPORTED, FAILED }

/**
 * The single real actuator PULSE uses to touch the device — both the Rule
 * Engine (a fired automation) and the Mode Engine (activating/deactivating
 * a Mode's action bundle) execute [RuleAction]s through this one class, so
 * there is exactly one place that ever calls a system-mutating Android API.
 * Every method here uses only officially documented, public APIs — no
 * System UI binding, no reflection, no hidden APIs — per the spec's hard
 * requirement to stay inside allowed Android surface area. Each call
 * verifies its own prerequisite permission and returns [ActionOutcome]
 * rather than throwing, so a denied permission degrades one action instead
 * of crashing the whole rule.
 */
@Singleton
class ModeActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    suspend fun execute(action: RuleAction): ActionOutcome = when (action.type) {
        ActionType.SET_DND -> setDnd(action.params["filter"] ?: "priority")
        ActionType.SET_VOLUME -> setVolume(action.params["percent"]?.toIntOrNull() ?: 50)
        ActionType.SET_BRIGHTNESS -> setBrightness(action.params["percent"]?.toIntOrNull() ?: 50)
        ActionType.SET_SCREEN_TIMEOUT -> setScreenTimeout(action.params["seconds"]?.toIntOrNull() ?: 30)
        ActionType.VIBRATE -> vibrate()
        ActionType.LAUNCH_APP -> launchApp(action.params["packageName"].orEmpty())
        ActionType.SHOW_NOTIFICATION -> showNotification(
            action.params["title"].orEmpty(),
            action.params["message"].orEmpty(),
        )
        // These actions are orchestrated by higher-level engines that hold
        // the relevant repository (ModeEngine, RuleEngine, SmartNotificationEngine)
        // rather than performed here directly — they carry data, not a
        // single system call, so routing them through this shared actuator
        // would only add an indirection with no safety benefit.
        ActionType.SUGGEST_MODE, ActionType.ACTIVATE_MODE, ActionType.DEACTIVATE_MODE,
        ActionType.CREATE_REMINDER, ActionType.CALENDAR_ACTION, ActionType.DEFER_NOTIFICATIONS,
        ActionType.SEND_DIGEST, ActionType.SET_WALLPAPER_THEME, ActionType.SET_ALARM_TIMER,
        ActionType.MEDIA_CONTROL, ActionType.LAUNCH_SHORTCUT, ActionType.CUSTOM,
        -> ActionOutcome.UNSUPPORTED
    }

    private fun setDnd(filterName: String): ActionOutcome {
        if (!notificationManager.isNotificationPolicyAccessGranted) return ActionOutcome.MISSING_PERMISSION
        val filter = when (filterName) {
            "none" -> NotificationManager.INTERRUPTION_FILTER_NONE
            "alarms" -> NotificationManager.INTERRUPTION_FILTER_ALARMS
            "priority" -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else -> NotificationManager.INTERRUPTION_FILTER_ALL
        }
        return runCatching { notificationManager.setInterruptionFilter(filter) }
            .fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun setVolume(percent: Int): ActionOutcome {
        return runCatching {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
            val target = (max * percent.coerceIn(0, 100) / 100)
            audioManager.setStreamVolume(AudioManager.STREAM_RING, target, 0)
        }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun setBrightness(percent: Int): ActionOutcome {
        if (!Settings.System.canWrite(context)) return ActionOutcome.MISSING_PERMISSION
        val value = (255 * percent.coerceIn(0, 100) / 100)
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun setScreenTimeout(seconds: Int): ActionOutcome {
        if (!Settings.System.canWrite(context)) return ActionOutcome.MISSING_PERMISSION
        return runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, seconds * 1000)
        }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun vibrate(): ActionOutcome {
        return runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun launchApp(packageName: String): ActionOutcome {
        if (packageName.isBlank()) return ActionOutcome.UNSUPPORTED
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return ActionOutcome.FAILED
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.FAILED })
    }

    private fun showNotification(title: String, message: String): ActionOutcome {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title.ifBlank { "PULSE" })
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        return runCatching {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        }.fold({ ActionOutcome.SUCCESS }, { ActionOutcome.MISSING_PERMISSION })
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID, "PULSE Suggestions", NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "pulse_actions"
    }
}
