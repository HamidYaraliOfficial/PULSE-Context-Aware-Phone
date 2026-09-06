package com.pulse.app.context

import com.pulse.app.domain.model.AppCategory
import com.pulse.app.domain.model.ContextType
import com.pulse.app.domain.model.DetectedActivityType
import com.pulse.app.domain.model.NormalizedContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextInferenceEngineTest {

    private val engine = ContextInferenceEngine()

    @Test
    fun `strong meeting signals produce MEETING with high confidence`() {
        val snapshot = NormalizedContext(
            timeOfDayMinutes = 10 * 60,
            dayOfWeek = 2,
            calendarEventIsMeetingLike = true,
            activeCalendarEventTitle = "Weekly Sync",
            headphonesConnected = true,
            isDeviceStill = true,
            foregroundAppCategory = AppCategory.MEETING,
        )
        val result = engine.infer(snapshot)
        assertEquals(ContextType.MEETING, result.type)
        assertTrue(result.confidence >= ContextInferenceEngine.BASE_MIN_CONFIDENCE)
        assertTrue(result.signals.isNotEmpty())
    }

    @Test
    fun `strong driving signal produces DRIVING`() {
        val snapshot = NormalizedContext(
            timeOfDayMinutes = 8 * 60,
            dayOfWeek = 3,
            detectedActivity = DetectedActivityType.DRIVING,
            activityConfidence = 95,
            isDeviceStill = false,
        )
        val result = engine.infer(snapshot)
        assertEquals(ContextType.DRIVING, result.type)
    }

    @Test
    fun `no strong signals falls back to AVAILABLE`() {
        val snapshot = NormalizedContext(timeOfDayMinutes = 14 * 60, dayOfWeek = 4)
        val result = engine.infer(snapshot)
        assertEquals(ContextType.AVAILABLE, result.type)
    }

    @Test
    fun `every returned signal contribution has a positive weight`() {
        val snapshot = NormalizedContext(
            timeOfDayMinutes = 23 * 60,
            dayOfWeek = 5,
            isCharging = true,
            isDeviceStill = true,
            ambientLightLux = 1f,
        )
        val result = engine.infer(snapshot)
        result.signals.forEach { assertTrue(it.weight > 0f) }
    }
}
