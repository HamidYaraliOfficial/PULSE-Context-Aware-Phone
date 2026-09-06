package com.pulse.app.domain.model

import java.time.Instant
import java.util.UUID

/**
 * The set of context states PULSE's Context Engine can recognize.
 * CUSTOM defers to [ContextState.customLabel] for the display name and is
 * backed by a user-authored [com.pulse.app.database.entities.CustomContextEntity].
 */
enum class ContextType {
    STUDY, WORK, MEETING, DRIVING, SLEEPING, EXERCISING, TRAVELING,
    RELAXING, GAMING, COMMUTING, FOCUS, BUSY, AVAILABLE, CUSTOM, UNKNOWN
}

/** One signal's contribution to a context inference decision — powers the Explainability Panel. */
data class SignalContribution(
    val signalKey: String,
    val humanReadable: String,
    val weight: Float,
    val value: String,
)

/**
 * A single detected context state with a confidence score and the signals
 * that produced it. This is the atomic unit the Context Inference Engine
 * emits, the Timeline records, and the Explainability Panel reads.
 */
data class ContextState(
    val id: String = UUID.randomUUID().toString(),
    val type: ContextType,
    val customLabel: String? = null,
    val confidence: Float, // 0f..1f
    val startedAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
    val signals: List<SignalContribution> = emptyList(),
    val feedback: ContextFeedback? = null,
) {
    val confidencePercent: Int get() = (confidence * 100).toInt().coerceIn(0, 100)
    val displayLabel: String get() = customLabel ?: type.name
}

enum class ContextFeedback { CONFIRMED, REJECTED, CORRECTED }

/**
 * Raw, normalized signal snapshot produced by every signal provider on each
 * evaluation tick. The Context Inference Engine consumes a [NormalizedContext]
 * (a bundle of these) rather than talking to Android APIs directly, which is
 * what makes Simulation Mode possible — a fake [NormalizedContext] is
 * indistinguishable from a real one to the inference layer.
 */
data class NormalizedContext(
    val timestamp: Instant = Instant.now(),
    val timeOfDayMinutes: Int, // minutes since local midnight
    val dayOfWeek: Int, // 1=Monday..7=Sunday (ISO-8601)
    val isCharging: Boolean = false,
    val batteryLevel: Int = 100,
    val isBatterySaver: Boolean = false,
    val detectedActivity: DetectedActivityType = DetectedActivityType.UNKNOWN,
    val activityConfidence: Int = 0,
    val stepsLastHour: Int = 0,
    val ambientLightLux: Float? = null,
    val isDeviceStill: Boolean = true,
    val proximityNear: Boolean = false,
    val headphonesConnected: Boolean = false,
    val knownBluetoothDevices: List<String> = emptyList(),
    val hasFixedLocationPattern: Boolean = false,
    val isAtKnownPlace: String? = null,
    val activeCalendarEventTitle: String? = null,
    val calendarEventIsMeetingLike: Boolean = false,
    val foregroundAppCategory: AppCategory = AppCategory.UNKNOWN,
    val recentAppCategories: List<AppCategory> = emptyList(),
    val notificationLoadLastHour: Int = 0,
    val isSimulated: Boolean = false,
)

enum class DetectedActivityType { STILL, WALKING, RUNNING, CYCLING, DRIVING, UNKNOWN }

enum class AppCategory { IDE_DEV, BROWSER, NOTES, COMMUNICATION, MEETING, MEDIA, GAME, SOCIAL, FITNESS, NAVIGATION, UNKNOWN }
