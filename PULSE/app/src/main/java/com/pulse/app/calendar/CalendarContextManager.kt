package com.pulse.app.calendar

import android.content.Context
import android.database.ContentObserver
import android.provider.CalendarContract
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEventSnapshot(
    val title: String,
    val calendarDisplayName: String?,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val isMeetingLike: Boolean,
    val attendeeCount: Int,
)

/**
 * Calendar Context Manager. Reads only what is required to power Meeting
 * detection: the current/next event's title, calendar, time window and a
 * coarse attendee count — never attendee identities/emails, which never
 * leave [CalendarEventSnapshot] into any persisted log (only the count is
 * cached, see [com.pulse.app.database.entities.CalendarContextEntity]).
 */
@Singleton
class CalendarContextManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val meetingKeywords = listOf(
        "meeting", "call", "sync", "standup", "interview", "session",
        "جلسه", "会议", "同步会", "面试",
    )

    @RequiresPermission(android.Manifest.permission.READ_CALENDAR)
    fun currentOrNextEvent(): CalendarEventSnapshot? {
        val now = System.currentTimeMillis()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.EVENT_ID,
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, now - ONE_HOUR_MS)
        android.content.ContentUris.appendId(builder, now + THREE_HOURS_MS)

        val cursor = runCatching {
            context.contentResolver.query(builder.build(), projection, null, null, "begin ASC")
        }.getOrNull() ?: return null

        cursor.use {
            while (it.moveToNext()) {
                val title = it.getString(0) ?: continue
                val begin = it.getLong(1)
                val end = it.getLong(2)
                val calendarName = it.getString(3)
                val eventId = it.getLong(4)
                if (now in begin..end || begin > now) {
                    val attendeeCount = attendeeCountFor(eventId)
                    return CalendarEventSnapshot(
                        title = title,
                        calendarDisplayName = calendarName,
                        startTimeEpochMs = begin,
                        endTimeEpochMs = end,
                        isMeetingLike = looksLikeMeeting(title, attendeeCount),
                        attendeeCount = attendeeCount,
                    )
                }
            }
        }
        return null
    }

    @RequiresPermission(android.Manifest.permission.READ_CALENDAR)
    private fun attendeeCountFor(eventId: Long): Int {
        val uri = CalendarContract.Attendees.CONTENT_URI
        val projection = arrayOf(CalendarContract.Attendees.ATTENDEE_EMAIL)
        val selection = "${CalendarContract.Attendees.EVENT_ID} = ?"
        val cursor = runCatching {
            context.contentResolver.query(uri, projection, selection, arrayOf(eventId.toString()), null)
        }.getOrNull() ?: return 0
        return cursor.use { it.count }
    }

    private fun looksLikeMeeting(title: String, attendeeCount: Int): Boolean {
        val lower = title.lowercase(Locale.getDefault())
        return attendeeCount > 1 || meetingKeywords.any { lower.contains(it) }
    }

    /** Observes calendar content changes so the Context Engine re-evaluates promptly on edits. */
    fun observeChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(CalendarContract.Instances.CONTENT_URI, true, observer)
        trySend(Unit)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    private companion object {
        const val ONE_HOUR_MS = 60L * 60 * 1000
        const val THREE_HOURS_MS = 3L * 60 * 60 * 1000
    }
}
