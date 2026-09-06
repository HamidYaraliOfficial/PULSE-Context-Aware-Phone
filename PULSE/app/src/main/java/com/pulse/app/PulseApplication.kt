package com.pulse.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * PULSE — Context-Aware Phone
 *
 * Application entry point. PULSE is local-first: the [HiltWorkerFactory] wires
 * dependency injection into WorkManager so every background worker (context
 * evaluation, battery-aware scheduling, retention cleanup) gets its
 * repositories injected without a singleton service locator.
 *
 * No analytics SDK, ad SDK, or crash-reporting SDK is initialized here —
 * PULSE ships with zero third-party telemetry by design.
 */
@HiltAndroidApp
class PulseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Context Engine + battery-aware scheduling are bootstrapped lazily on
        // first Activity start (see MainActivity) rather than here, so a cold
        // process spun up only to handle e.g. a BOOT_COMPLETED broadcast does
        // not pay the cost of building the full dependency graph eagerly.
    }
}
