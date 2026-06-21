package com.miletracker.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSTemporaryDirectory

data class DemoSettings(
    val simulateRoot: Boolean = false,
    val simulateOffline: Boolean = false,
    val biometricGuardEnabled: Boolean = false,
    val simulateGpsDrift: Boolean = false,
    val autoDiscardEnabled: Boolean = false,
    // G6: persisted opt-in for the live Kalman fix smoother (default off preserves tracking math).
    val enableKalman: Boolean = false,
    // G7: last completed trip's end-odometer reading; -1 = none recorded yet.
    val lastOdometerEndReading: Int = LAST_ODOMETER_NONE,
)

/** Sentinel for "no prior odometer reading" so the start capture falls back to its own default. */
const val LAST_ODOMETER_NONE: Int = -1

class DemoSettingsRepository {
    private val simulateRootKey = booleanPreferencesKey("demo_simulate_root")
    private val simulateOfflineKey = booleanPreferencesKey("demo_simulate_offline")
    private val biometricGuardKey = booleanPreferencesKey("demo_biometric_guard")
    private val gpsDriftKey = booleanPreferencesKey("demo_simulate_gps_drift")
    private val autoDiscardKey = booleanPreferencesKey("demo_auto_discard")
    private val enableKalmanKey = booleanPreferencesKey("track_enable_kalman")
    private val lastOdometerEndKey = intPreferencesKey("track_last_odometer_end")

    private val store: DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { (NSTemporaryDirectory() + "demo_settings.preferences_pb").toPath() },
        )

    val settings: Flow<DemoSettings> =
        store.data.map { prefs ->
            DemoSettings(
                simulateRoot = prefs[simulateRootKey] ?: false,
                simulateOffline = prefs[simulateOfflineKey] ?: false,
                biometricGuardEnabled = prefs[biometricGuardKey] ?: false,
                simulateGpsDrift = prefs[gpsDriftKey] ?: false,
                autoDiscardEnabled = prefs[autoDiscardKey] ?: false,
                enableKalman = prefs[enableKalmanKey] ?: false,
                lastOdometerEndReading = prefs[lastOdometerEndKey] ?: LAST_ODOMETER_NONE,
            )
        }

    suspend fun toggleSimulateRoot() = toggle(simulateRootKey)

    suspend fun toggleSimulateOffline() = toggle(simulateOfflineKey)

    suspend fun toggleBiometricGuard() = toggle(biometricGuardKey)

    suspend fun toggleGpsDrift() = toggle(gpsDriftKey)

    suspend fun toggleAutoDiscard() = toggle(autoDiscardKey)

    /** G6: explicit set (the customization Switch sets a value rather than toggling). */
    suspend fun setEnableKalman(enabled: Boolean) {
        store.edit { prefs -> prefs[enableKalmanKey] = enabled }
    }

    /** G7: record the latest completed trip's end-odometer reading for the next trip's start. */
    suspend fun setLastOdometerEndReading(reading: Int) {
        store.edit { prefs -> prefs[lastOdometerEndKey] = reading }
    }

    private suspend fun toggle(key: Preferences.Key<Boolean>) {
        store.edit { prefs -> prefs[key] = !(prefs[key] ?: false) }
    }
}
