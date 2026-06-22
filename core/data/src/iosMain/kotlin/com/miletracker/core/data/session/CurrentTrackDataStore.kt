package com.miletracker.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miletracker.core.data.model.db.CurrentTrackData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

class CurrentTrackDataStore : CurrentTrackDataSource {
    companion object {
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_IS_TRACKING = booleanPreferencesKey("is_tracking")
        val KEY_IS_PAUSED = booleanPreferencesKey("is_paused")
        val KEY_START_LAT = doublePreferencesKey("start_lat")
        val KEY_START_LNG = doublePreferencesKey("start_lng")
        val KEY_END_LAT = doublePreferencesKey("end_lat")
        val KEY_END_LNG = doublePreferencesKey("end_lng")
        val KEY_START_TIME = longPreferencesKey("start_time")
        val KEY_END_TIME = longPreferencesKey("end_time")
        val KEY_DISTANCE = doublePreferencesKey("distance")
        val KEY_VEHICLE_TYPE = stringPreferencesKey("vehicle_type")
        val KEY_VEHICLE_PRICING = doublePreferencesKey("vehicle_pricing")
        val KEY_SERVICE = stringPreferencesKey("service")
        val KEY_SPEED = doublePreferencesKey("speed")
        val KEY_AVG_SPEED = doublePreferencesKey("avg_speed")
        val KEY_MAX_SPEED = doublePreferencesKey("max_speed")
        val KEY_TOTAL_POINTS = longPreferencesKey("total_points")
        val KEY_UNSYNCED_POINTS = longPreferencesKey("unsynced_points")
        val KEY_LAST_HW_EVENT = stringPreferencesKey("last_hw_event")
        val KEY_LAST_HW_EVENT_TIME = longPreferencesKey("last_hw_event_time")
        val KEY_WAS_PAUSED = booleanPreferencesKey("was_paused")
        val KEY_STARTED_AT = longPreferencesKey("started_at")
    }

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "current_track_session.preferences_pb").toPath() },
        )

    override val currentTrackFlow: Flow<CurrentTrackData> =
        store.data.map { prefs ->
            CurrentTrackData(
                token = prefs[KEY_TOKEN] ?: "",
                isTracking = prefs[KEY_IS_TRACKING] ?: false,
                isPaused = prefs[KEY_IS_PAUSED] ?: false,
                startLatitude = prefs[KEY_START_LAT] ?: 0.0,
                startLongitude = prefs[KEY_START_LNG] ?: 0.0,
                endLatitude = prefs[KEY_END_LAT] ?: 0.0,
                endLongitude = prefs[KEY_END_LNG] ?: 0.0,
                startTime = prefs[KEY_START_TIME] ?: 0L,
                endTime = prefs[KEY_END_TIME] ?: 0L,
                distance = prefs[KEY_DISTANCE] ?: 0.0,
                selectedVehicleType = prefs[KEY_VEHICLE_TYPE] ?: "",
                vehiclePricing = prefs[KEY_VEHICLE_PRICING] ?: 0.0,
                service = prefs[KEY_SERVICE] ?: "",
                speed = prefs[KEY_SPEED] ?: 0.0,
                avgSpeed = prefs[KEY_AVG_SPEED] ?: 0.0,
                maxSpeed = prefs[KEY_MAX_SPEED] ?: 0.0,
                totalLocationPoints = prefs[KEY_TOTAL_POINTS] ?: 0L,
                unsyncedLocationPoints = prefs[KEY_UNSYNCED_POINTS] ?: 0L,
                lastHardwareEventText = prefs[KEY_LAST_HW_EVENT] ?: "",
                lastHardwareEventTime = prefs[KEY_LAST_HW_EVENT_TIME] ?: -1L,
                wasEverPaused = prefs[KEY_WAS_PAUSED] ?: false,
                startedAtTimestamp = prefs[KEY_STARTED_AT] ?: 0L,
            )
        }

    override suspend fun saveSession(data: CurrentTrackData) {
        store.edit { prefs ->
            prefs[KEY_TOKEN] = data.token
            prefs[KEY_IS_TRACKING] = data.isTracking
            prefs[KEY_IS_PAUSED] = data.isPaused
            prefs[KEY_START_LAT] = data.startLatitude
            prefs[KEY_START_LNG] = data.startLongitude
            prefs[KEY_END_LAT] = data.endLatitude
            prefs[KEY_END_LNG] = data.endLongitude
            prefs[KEY_START_TIME] = data.startTime
            prefs[KEY_END_TIME] = data.endTime
            prefs[KEY_DISTANCE] = data.distance
            prefs[KEY_VEHICLE_TYPE] = data.selectedVehicleType
            prefs[KEY_VEHICLE_PRICING] = data.vehiclePricing
            prefs[KEY_SERVICE] = data.service
            prefs[KEY_SPEED] = data.speed
            prefs[KEY_AVG_SPEED] = data.avgSpeed
            prefs[KEY_MAX_SPEED] = data.maxSpeed
            prefs[KEY_TOTAL_POINTS] = data.totalLocationPoints
            prefs[KEY_UNSYNCED_POINTS] = data.unsyncedLocationPoints
            prefs[KEY_LAST_HW_EVENT] = data.lastHardwareEventText
            prefs[KEY_LAST_HW_EVENT_TIME] = data.lastHardwareEventTime
            prefs[KEY_WAS_PAUSED] = data.wasEverPaused
            prefs[KEY_STARTED_AT] = data.startedAtTimestamp
        }
    }

    override suspend fun updateDistance(
        token: String,
        distanceMeters: Double,
        speed: Double,
        avgSpeed: Double,
    ) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) {
                prefs[KEY_DISTANCE] = distanceMeters
                prefs[KEY_SPEED] = speed
                prefs[KEY_AVG_SPEED] = avgSpeed
                if (speed > (prefs[KEY_MAX_SPEED] ?: 0.0)) prefs[KEY_MAX_SPEED] = speed
            }
        }
    }

    override suspend fun updateLocationCount(
        token: String,
        total: Long,
        unsynced: Long,
    ) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) {
                prefs[KEY_TOTAL_POINTS] = total
                prefs[KEY_UNSYNCED_POINTS] = unsynced
            }
        }
    }

    override suspend fun markPaused(
        token: String,
        lat: Double,
        lng: Double,
    ) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) {
                prefs[KEY_IS_PAUSED] = true
                prefs[KEY_WAS_PAUSED] = true
            }
        }
    }

    override suspend fun markResumed(token: String) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) prefs[KEY_IS_PAUSED] = false
        }
    }

    override suspend fun markStopped(
        token: String,
        endLat: Double,
        endLng: Double,
    ) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) {
                prefs[KEY_IS_TRACKING] = false
                prefs[KEY_IS_PAUSED] = false
                prefs[KEY_END_LAT] = endLat
                prefs[KEY_END_LNG] = endLng
                prefs[KEY_END_TIME] = kotlin.time.Clock.System.now().toEpochMilliseconds()
            }
        }
    }

    override suspend fun clearSession() {
        store.edit { it.clear() }
    }

    override suspend fun updateLastHardwareEvent(
        token: String,
        eventText: String,
    ) {
        store.edit { prefs ->
            if (prefs[KEY_TOKEN] == token) {
                prefs[KEY_LAST_HW_EVENT] = eventText
                prefs[KEY_LAST_HW_EVENT_TIME] = kotlin.time.Clock.System.now().toEpochMilliseconds()
            }
        }
    }
}
