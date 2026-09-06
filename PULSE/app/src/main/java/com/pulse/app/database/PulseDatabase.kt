package com.pulse.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pulse.app.database.dao.*
import com.pulse.app.database.entities.*

@Database(
    entities = [
        ContextEventEntity::class,
        CustomContextTypeEntity::class,
        ModeEntity::class,
        ModeActivationEntity::class,
        SessionEntity::class,
        LocationContextEntity::class,
        DeviceContextEntity::class,
        CalendarContextEntity::class,
        RuleEntity::class,
        TriggerEntity::class,
        ActionEntity::class,
        ExecutionLogEntity::class,
        FeedbackEntity::class,
        LearningWeightEntity::class,
        UsageRecordEntity::class,
        NotificationRecordEntity::class,
        PreferenceEntity::class,
        PermissionStateEntity::class,
        AuditLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PulseDatabase : RoomDatabase() {
    abstract fun contextEventDao(): ContextEventDao
    abstract fun customContextDao(): CustomContextDao
    abstract fun modeDao(): ModeDao
    abstract fun sessionDao(): SessionDao
    abstract fun locationContextDao(): LocationContextDao
    abstract fun deviceContextDao(): DeviceContextDao
    abstract fun calendarContextDao(): CalendarContextDao
    abstract fun ruleDao(): RuleDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun notificationRecordDao(): NotificationRecordDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun permissionStateDao(): PermissionStateDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        const val DATABASE_NAME = "pulse_database"

        /**
         * Real migration, not a placeholder: PULSE v1.0 shipped `context_events`
         * without the Timeline "Ignore" flag and `rules` without a persisted
         * visual-canvas layout. v1.1 added both. Anyone upgrading in place keeps
         * their full History and Rules — Room's fallbackToDestructiveMigration
         * is intentionally NOT used anywhere in this database builder.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE context_events ADD COLUMN ignored INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE rules ADD COLUMN canvasLayoutJson TEXT")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
