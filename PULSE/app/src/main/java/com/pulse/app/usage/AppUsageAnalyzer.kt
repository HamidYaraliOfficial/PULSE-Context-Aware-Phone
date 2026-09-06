package com.pulse.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.pulse.app.domain.model.AppCategory
import com.pulse.app.domain.model.AppUsageRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart App Usage Engine. Reads foreground-app windows from
 * [UsageStatsManager] (requires the user-granted "Usage Access" special
 * permission — never a runtime prompt) and classifies each package into a
 * coarse [AppCategory] using an editable local keyword map, entirely
 * on-device. This category stream is what lets the Context Inference Engine
 * notice "IDE + Browser + Notes repeatedly in the same hour → suggest Work".
 */
@Singleton
class AppUsageAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    /** Package-name substrings → category. User-extensible in Settings → Context Detection (not shown here). */
    private val categoryKeywords: Map<AppCategory, List<String>> = mapOf(
        AppCategory.IDE_DEV to listOf("studio", "code", "intellij", "github", "gitlab", "termux", "pydroid"),
        AppCategory.BROWSER to listOf("chrome", "firefox", "browser", "edge", "opera", "brave"),
        AppCategory.NOTES to listOf("notes", "keep", "notion", "obsidian", "evernote", "onenote"),
        AppCategory.COMMUNICATION to listOf("gmail", "outlook", "mail", "slack", "teams"),
        AppCategory.MEETING to listOf("zoom", "meet", "webex", "teams"),
        AppCategory.MEDIA to listOf("spotify", "music", "youtube", "podcast"),
        AppCategory.GAME to listOf("game", "unity", "pubg", "clash"),
        AppCategory.SOCIAL to listOf("instagram", "twitter", "facebook", "tiktok", "reddit", "telegram", "whatsapp"),
        AppCategory.FITNESS to listOf("fit", "strava", "health", "workout"),
        AppCategory.NAVIGATION to listOf("maps", "waze", "navigation"),
    )

    fun categorize(packageName: String): AppCategory {
        val lower = packageName.lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return AppCategory.UNKNOWN
    }

    fun hasUsageAccess(): Boolean = runCatching {
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        stats.isNotEmpty()
    }.getOrDefault(false)

    /** Reconstructs discrete foreground windows between [startMs] and [endMs] from the raw event stream. */
    fun foregroundWindows(startMs: Long, endMs: Long): List<AppUsageRecord> {
        if (!hasUsageAccess()) return emptyList()
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val windows = mutableListOf<AppUsageRecord>()
        var currentPackage: String? = null
        var currentStart: Long = startMs
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND, UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentPackage = event.packageName
                    currentStart = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND, UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val pkg = currentPackage
                    if (pkg != null && event.timeStamp > currentStart) {
                        windows += buildRecord(pkg, currentStart, event.timeStamp)
                    }
                    currentPackage = null
                }
            }
        }
        return windows
    }

    private fun buildRecord(packageName: String, start: Long, end: Long): AppUsageRecord {
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        return AppUsageRecord(
            packageName = packageName,
            appLabel = label,
            category = categorize(packageName),
            windowStart = Instant.ofEpochMilli(start),
            windowEnd = Instant.ofEpochMilli(end),
            foregroundMillis = end - start,
        )
    }

    fun isMissingUsageAccessSettingsIntent() = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
