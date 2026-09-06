package com.pulse.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import com.pulse.app.database.dao.LocationContextDao
import com.pulse.app.database.entities.LocationContextEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class LocationSignal(
    val latitude: Double,
    val longitude: Double,
    val knownPlaceLabel: String?,
    val isMoving: Boolean,
)

/**
 * Location is opt-in at three independent levels: the OS runtime permission,
 * the PULSE Permission Center toggle, and — per feature — whether a
 * specific Rule/Mode is allowed to read it at all (checked upstream by
 * PermissionManager before this class is ever instantiated with real
 * updates started). Update interval is intentionally coarse; battery
 * priority is BALANCED_POWER_ACCURACY, never HIGH_ACCURACY, since PULSE
 * needs "which known place am I near", not turn-by-turn precision.
 */
@Singleton
class LocationContextManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationContextDao: LocationContextDao,
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // caller verifies permission via PermissionManager first
    fun observe(intervalMs: Long = DEFAULT_INTERVAL_MS): Flow<LocationSignal> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        var lastLocation: Location? = null

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val moving = lastLocation?.let { loc.distanceTo(it) > MOVING_THRESHOLD_METERS } ?: false
                lastLocation = loc
                trySend(LocationSignal(loc.latitude, loc.longitude, null, moving))
                resolveKnownPlace(loc.latitude, loc.longitude) { label ->
                    if (label != null) {
                        trySend(LocationSignal(loc.latitude, loc.longitude, label, moving))
                    }
                }
            }
        }

        runCatching {
            fusedClient.requestLocationUpdates(request, callback, context.mainLooper)
        }.onFailure { close(it) }

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    private fun resolveKnownPlace(lat: Double, lng: Double, onResult: (String?) -> Unit) {
        // Fire-and-forget lookup against user-labeled places (Home / University /
        // Gym…) created in Settings — never a reverse-geocoding network call;
        // everything stays on-device.
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            val known = runCatching { locationContextDao.all() }.getOrDefault(emptyList())
            val match = known.firstOrNull { place ->
                haversineMeters(lat, lng, place.latitude, place.longitude) <= place.radiusMeters
            }
            onResult(match?.label)
        }
    }

    suspend fun saveKnownPlace(entity: LocationContextEntity) = locationContextDao.upsert(entity)
    suspend fun knownPlaces(): List<LocationContextEntity> = locationContextDao.all()

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 5 * 60_000L
        const val MOVING_THRESHOLD_METERS = 40f
    }
}
