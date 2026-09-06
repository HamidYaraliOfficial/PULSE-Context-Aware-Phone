package com.pulse.app.core.di

import com.pulse.app.data.repository.*
import com.pulse.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindContextRepository(impl: ContextRepositoryImpl): ContextRepository
    @Binds @Singleton abstract fun bindRuleRepository(impl: RuleRepositoryImpl): RuleRepository
    @Binds @Singleton abstract fun bindModeRepository(impl: ModeRepositoryImpl): ModeRepository
    @Binds @Singleton abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository
    @Binds @Singleton abstract fun bindNotificationEngineRepository(impl: NotificationEngineRepositoryImpl): NotificationEngineRepository
    @Binds @Singleton abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository
    @Binds @Singleton abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
    @Binds @Singleton abstract fun bindRetentionRepository(impl: RetentionRepositoryImpl): RetentionRepository
    @Binds @Singleton abstract fun bindLearningRepository(impl: LearningRepositoryImpl): LearningRepository
    @Binds @Singleton abstract fun bindRecommendationRepository(impl: RecommendationRepositoryImpl): RecommendationRepository
}
