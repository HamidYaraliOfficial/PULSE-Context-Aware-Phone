package com.pulse.app.context

import com.pulse.app.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private data class ScoredContext(
    val type: ContextType,
    val confidence: Float,
    val signals: List<SignalContribution>,
)

/**
 * Combines a [NormalizedContext] snapshot from many independent signal
 * providers into a single, confidence-scored, explainable [ContextState] —
 * the "several signals raise the probability of Meeting Mode together"
 * behaviour described in the spec. Each `score*` function is a small,
 * auditable weighted-sum heuristic; every weight that contributed is
 * returned as a [SignalContribution] so the Explainability Panel can show
 * exactly why a context was chosen. This is intentionally NOT a black-box
 * ML model — on-device explainability was a hard requirement.
 *
 * [minConfidenceOverrides] comes from the Learning System: confirming a
 * context repeatedly lowers its effective bar slightly, rejecting it raises
 * it — see [com.pulse.app.context.LearningEngine].
 */
@Singleton
class ContextInferenceEngine @Inject constructor() {

    fun infer(
        snapshot: NormalizedContext,
        minConfidenceOverrides: Map<ContextType, Float> = emptyMap(),
    ): ContextState {
        val candidates = listOf(
            scoreMeeting(snapshot),
            scoreDriving(snapshot),
            scoreSleeping(snapshot),
            scoreExercising(snapshot),
            scoreStudy(snapshot),
            scoreWork(snapshot),
            scoreGaming(snapshot),
            scoreRelaxing(snapshot),
            scoreCommuting(snapshot),
            scoreFocus(snapshot),
            scoreBusy(snapshot),
        )

        val best = candidates
            .filter { it.confidence >= (minConfidenceOverrides[it.type] ?: BASE_MIN_CONFIDENCE) }
            .maxByOrNull { it.confidence }

        return if (best != null) {
            ContextState(type = best.type, confidence = best.confidence, signals = best.signals)
        } else {
            ContextState(
                type = ContextType.AVAILABLE,
                confidence = 1f - (candidates.maxOfOrNull { it.confidence } ?: 0f),
                signals = listOf(SignalContribution("fallback", "No strong signal combination matched", 1f, "n/a")),
            )
        }
    }

    // -------------------------------------------------------------
    private fun scoreMeeting(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.calendarEventIsMeetingLike) {
            score += 0.45f
            sig += SignalContribution("calendar", "Active calendar event looks like a meeting", 0.45f, s.activeCalendarEventTitle.orEmpty())
        }
        if (s.headphonesConnected) {
            score += 0.20f
            sig += SignalContribution("bluetooth", "Headphones connected", 0.20f, "true")
        }
        if (s.isDeviceStill) {
            score += 0.15f
            sig += SignalContribution("motion", "Device has been still", 0.15f, "still")
        }
        if (s.foregroundAppCategory == AppCategory.MEETING || s.recentAppCategories.contains(AppCategory.MEETING)) {
            score += 0.20f
            sig += SignalContribution("app_usage", "A meeting app is/was in the foreground", 0.20f, s.foregroundAppCategory.name)
        }
        return ScoredContext(ContextType.MEETING, score.coerceAtMost(1f), sig)
    }

    private fun scoreDriving(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.detectedActivity == DetectedActivityType.DRIVING) {
            val weight = 0.6f * (s.activityConfidence / 100f)
            score += weight
            sig += SignalContribution("activity_recognition", "Activity Recognition reports Driving", weight, "${s.activityConfidence}%")
        }
        if (s.knownBluetoothDevices.any { it.contains("car", ignoreCase = true) }) {
            score += 0.25f
            sig += SignalContribution("bluetooth", "Car audio system connected", 0.25f, "car_audio")
        }
        if (!s.isDeviceStill && s.detectedActivity != DetectedActivityType.WALKING && s.detectedActivity != DetectedActivityType.RUNNING) {
            score += 0.15f
            sig += SignalContribution("motion", "Sustained non-pedestrian motion pattern", 0.15f, "moving")
        }
        return ScoredContext(ContextType.DRIVING, score.coerceAtMost(1f), sig)
    }

    private fun scoreSleeping(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        val minutes = s.timeOfDayMinutes
        val isNight = minutes >= 22 * 60 || minutes < 6 * 60
        if (isNight) {
            score += 0.35f
            sig += SignalContribution("time", "It is within typical sleep hours", 0.35f, "${minutes / 60}:${minutes % 60}")
        }
        if (s.isCharging) {
            score += 0.2f
            sig += SignalContribution("battery", "Device is charging", 0.2f, "charging")
        }
        if (s.isDeviceStill) {
            score += 0.25f
            sig += SignalContribution("motion", "Device completely still", 0.25f, "still")
        }
        if (s.ambientLightLux != null && s.ambientLightLux < 5f) {
            score += 0.2f
            sig += SignalContribution("light", "Ambient light is very low", 0.2f, "${s.ambientLightLux} lux")
        }
        return ScoredContext(ContextType.SLEEPING, score.coerceAtMost(1f), sig)
    }

    private fun scoreExercising(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.detectedActivity == DetectedActivityType.RUNNING) {
            score += 0.55f * (s.activityConfidence / 100f)
            sig += SignalContribution("activity_recognition", "Activity Recognition reports Running", 0.55f, "${s.activityConfidence}%")
        } else if (s.detectedActivity == DetectedActivityType.CYCLING) {
            score += 0.5f * (s.activityConfidence / 100f)
            sig += SignalContribution("activity_recognition", "Activity Recognition reports Cycling", 0.5f, "${s.activityConfidence}%")
        }
        if (s.stepsLastHour > 800) {
            score += 0.3f
            sig += SignalContribution("step_counter", "High step count in the last hour", 0.3f, "${s.stepsLastHour} steps")
        }
        if (s.recentAppCategories.contains(AppCategory.FITNESS)) {
            score += 0.15f
            sig += SignalContribution("app_usage", "A fitness app was recently used", 0.15f, "FITNESS")
        }
        return ScoredContext(ContextType.EXERCISING, score.coerceAtMost(1f), sig)
    }

    private fun scoreStudy(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.recentAppCategories.contains(AppCategory.NOTES)) {
            score += 0.3f
            sig += SignalContribution("app_usage", "Notes app recently used", 0.3f, "NOTES")
        }
        if (s.isDeviceStill) {
            score += 0.2f
            sig += SignalContribution("motion", "Device has been still", 0.2f, "still")
        }
        if (s.isAtKnownPlace != null) {
            score += 0.25f
            sig += SignalContribution("location", "At a known study location: ${s.isAtKnownPlace}", 0.25f, s.isAtKnownPlace)
        }
        if (s.notificationLoadLastHour < 3) {
            score += 0.15f
            sig += SignalContribution("notifications", "Low notification interruption rate", 0.15f, "${s.notificationLoadLastHour}/hr")
        }
        return ScoredContext(ContextType.STUDY, score.coerceAtMost(1f), sig)
    }

    private fun scoreWork(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        val workAppsUsed = s.recentAppCategories.count {
            it == AppCategory.IDE_DEV || it == AppCategory.BROWSER || it == AppCategory.COMMUNICATION
        }
        if (workAppsUsed >= 2) {
            score += 0.4f
            sig += SignalContribution("app_usage", "Multiple work-related apps used recently", 0.4f, "$workAppsUsed categories")
        }
        val minutes = s.timeOfDayMinutes
        val isWeekday = s.dayOfWeek in 1..5
        if (isWeekday && minutes in (9 * 60)..(18 * 60)) {
            score += 0.3f
            sig += SignalContribution("time", "Within typical weekday working hours", 0.3f, "weekday business hours")
        }
        if (s.isDeviceStill) {
            score += 0.15f
            sig += SignalContribution("motion", "Device has been still", 0.15f, "still")
        }
        return ScoredContext(ContextType.WORK, score.coerceAtMost(1f), sig)
    }

    private fun scoreGaming(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.foregroundAppCategory == AppCategory.GAME) {
            score += 0.7f
            sig += SignalContribution("app_usage", "A game is in the foreground", 0.7f, "GAME")
        }
        return ScoredContext(ContextType.GAMING, score.coerceAtMost(1f), sig)
    }

    private fun scoreRelaxing(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        val minutes = s.timeOfDayMinutes
        if (minutes in (19 * 60)..(23 * 60)) {
            score += 0.25f
            sig += SignalContribution("time", "Evening hours", 0.25f, "evening")
        }
        if (s.foregroundAppCategory == AppCategory.MEDIA || s.foregroundAppCategory == AppCategory.SOCIAL) {
            score += 0.4f
            sig += SignalContribution("app_usage", "Media/social app in the foreground", 0.4f, s.foregroundAppCategory.name)
        }
        if (s.isDeviceStill) {
            score += 0.15f
            sig += SignalContribution("motion", "Device has been still", 0.15f, "still")
        }
        return ScoredContext(ContextType.RELAXING, score.coerceAtMost(1f), sig)
    }

    private fun scoreCommuting(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        val minutes = s.timeOfDayMinutes
        val isCommuteWindow = minutes in (7 * 60)..(9 * 60 + 30) || minutes in (17 * 60)..(19 * 60 + 30)
        val moving = s.detectedActivity == DetectedActivityType.DRIVING || s.detectedActivity == DetectedActivityType.WALKING
        if (isCommuteWindow && moving) {
            score += 0.5f
            sig += SignalContribution("time_activity", "Movement during typical commute hours", 0.5f, s.detectedActivity.name)
        }
        if (s.recentAppCategories.contains(AppCategory.NAVIGATION)) {
            score += 0.3f
            sig += SignalContribution("app_usage", "Navigation app recently used", 0.3f, "NAVIGATION")
        }
        return ScoredContext(ContextType.COMMUTING, score.coerceAtMost(1f), sig)
    }

    private fun scoreFocus(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.isDeviceStill) {
            score += 0.25f
            sig += SignalContribution("motion", "Device has been still", 0.25f, "still")
        }
        if (s.notificationLoadLastHour <= 1) {
            score += 0.3f
            sig += SignalContribution("notifications", "Very low interruption rate", 0.3f, "${s.notificationLoadLastHour}/hr")
        }
        if (s.recentAppCategories.isNotEmpty() && s.recentAppCategories.distinct().size == 1 &&
            s.recentAppCategories.first() in setOf(AppCategory.IDE_DEV, AppCategory.NOTES)
        ) {
            score += 0.3f
            sig += SignalContribution("app_usage", "Sustained single-category app usage", 0.3f, s.recentAppCategories.first().name)
        }
        return ScoredContext(ContextType.FOCUS, min(score, 1f), sig)
    }

    private fun scoreBusy(s: NormalizedContext): ScoredContext {
        val sig = mutableListOf<SignalContribution>()
        var score = 0f
        if (s.notificationLoadLastHour >= 8) {
            score += 0.5f
            sig += SignalContribution("notifications", "High notification load", 0.5f, "${s.notificationLoadLastHour}/hr")
        }
        if (s.activeCalendarEventTitle != null && !s.calendarEventIsMeetingLike) {
            score += 0.3f
            sig += SignalContribution("calendar", "Non-meeting calendar event active", 0.3f, s.activeCalendarEventTitle)
        }
        return ScoredContext(ContextType.BUSY, score.coerceAtMost(1f), sig)
    }

    companion object {
        const val BASE_MIN_CONFIDENCE = 0.55f
    }
}
