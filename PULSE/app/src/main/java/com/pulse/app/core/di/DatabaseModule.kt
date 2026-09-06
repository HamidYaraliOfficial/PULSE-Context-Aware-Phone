package com.pulse.app.core.di

import android.content.Context
import androidx.room.Room
import com.pulse.app.database.PulseDatabase
import com.pulse.app.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulseDatabase =
        Room.databaseBuilder(context, PulseDatabase::class.java, PulseDatabase.DATABASE_NAME)
            .addMigrations(*PulseDatabase.ALL_MIGRATIONS)
            .build()

    @Provides fun provideContextEventDao(db: PulseDatabase): ContextEventDao = db.contextEventDao()
    @Provides fun provideCustomContextDao(db: PulseDatabase): CustomContextDao = db.customContextDao()
    @Provides fun provideModeDao(db: PulseDatabase): ModeDao = db.modeDao()
    @Provides fun provideSessionDao(db: PulseDatabase): SessionDao = db.sessionDao()
    @Provides fun provideLocationContextDao(db: PulseDatabase): LocationContextDao = db.locationContextDao()
    @Provides fun provideDeviceContextDao(db: PulseDatabase): DeviceContextDao = db.deviceContextDao()
    @Provides fun provideCalendarContextDao(db: PulseDatabase): CalendarContextDao = db.calendarContextDao()
    @Provides fun provideRuleDao(db: PulseDatabase): RuleDao = db.ruleDao()
    @Provides fun provideExecutionLogDao(db: PulseDatabase): ExecutionLogDao = db.executionLogDao()
    @Provides fun provideFeedbackDao(db: PulseDatabase): FeedbackDao = db.feedbackDao()
    @Provides fun provideUsageRecordDao(db: PulseDatabase): UsageRecordDao = db.usageRecordDao()
    @Provides fun provideNotificationRecordDao(db: PulseDatabase): NotificationRecordDao = db.notificationRecordDao()
    @Provides fun providePreferenceDao(db: PulseDatabase): PreferenceDao = db.preferenceDao()
    @Provides fun providePermissionStateDao(db: PulseDatabase): PermissionStateDao = db.permissionStateDao()
    @Provides fun provideAuditLogDao(db: PulseDatabase): AuditLogDao = db.auditLogDao()
}
