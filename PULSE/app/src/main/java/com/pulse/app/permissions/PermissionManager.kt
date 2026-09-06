package com.pulse.app.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.pulse.app.domain.model.PulsePermission
import com.pulse.app.domain.model.PermissionStatus
import com.pulse.app.usage.AppUsageAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Which OS permission string(s) back each [PulsePermission], for the Permission Center UI. */
object PermissionDefinitions {
    fun manifestPermissionsFor(permission: PulsePermission): List<String> = when (permission) {
        PulsePermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
        PulsePermission.LOCATION -> listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        PulsePermission.ACTIVITY_RECOGNITION -> listOf(Manifest.permission.ACTIVITY_RECOGNITION)
        PulsePermission.SENSORS -> listOf(Manifest.permission.BODY_SENSORS)
        PulsePermission.CALENDAR -> listOf(Manifest.permission.READ_CALENDAR)
        PulsePermission.BLUETOOTH -> if (Build.VERSION.SDK_INT >= 31) listOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyList()
        // USAGE_ACCESS and NOTIFICATION_ACCESS / DND_ACCESS are special app-ops /
        // settings-page grants, not runtime Manifest permissions.
        PulsePermission.USAGE_ACCESS, PulsePermission.NOTIFICATION_ACCESS, PulsePermission.DND_ACCESS -> emptyList()
    }
}

/**
 * Single source of truth for "is this OS permission actually granted right
 * now". Every signal collector (Activity Recognition, Location, Bluetooth,
 * Calendar, Sensors) must check the matching feature toggle here — not just
 * the OS grant — before it is allowed to start, which is what makes every
 * Feature's Privacy Toggle in the spec actually independent from the OS
 * permission dialog.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUsageAnalyzer: AppUsageAnalyzer,
) {
    fun osStatus(permission: PulsePermission): PermissionStatus {
        val manifestPerms = PermissionDefinitions.manifestPermissionsFor(permission)
        return when (permission) {
            PulsePermission.USAGE_ACCESS ->
                if (appUsageAnalyzer.hasUsageAccess()) PermissionStatus.GRANTED else PermissionStatus.DENIED
            PulsePermission.NOTIFICATION_ACCESS -> {
                val enabled = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.packageName)
                if (enabled) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
            PulsePermission.DND_ACCESS -> {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.isNotificationPolicyAccessGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
            else -> {
                if (manifestPerms.isEmpty()) return PermissionStatus.NOT_REQUESTED
                val allGranted = manifestPerms.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED
            }
        }
    }

    fun allPermissions(): List<PulsePermission> = PulsePermission.entries.toList()
}
