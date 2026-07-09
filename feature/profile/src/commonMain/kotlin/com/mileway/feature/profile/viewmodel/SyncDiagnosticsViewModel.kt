package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.SyncSessionOverride
import com.mileway.feature.profile.model.SyncConfig
import com.mileway.feature.profile.model.SyncMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Pre-enable connectivity self-test state (analogous to the reference app's sync-enable guard — all simulated). */
enum class SyncGuardState { Idle, Testing, Passed }

/**
 * PLAN_V24 P10.2 — the resolved mileage-sync settings shown on the card: the persisted defaults
 * (sync-settings plugins in the registry) layered under any current-journey override.
 */
data class SyncSettingsUiState(
    val locationEnabled: Boolean = true,
    val eventsEnabled: Boolean = true,
    val debugEventsEnabled: Boolean = false,
    val v2ApiEnabled: Boolean = false,
    val intervalMinutes: Int = 15,
    val applyToFutureJourneys: Boolean = true,
    val sessionOverrideActive: Boolean = false,
    val guard: SyncGuardState = SyncGuardState.Idle,
) {
    fun toSyncConfig(): SyncConfig = SyncConfig(locationEnabled, eventsEnabled, debugEventsEnabled, intervalMinutes)
}

private data class SyncDefaults(
    val location: Boolean,
    val events: Boolean,
    val debugEvents: Boolean,
    val v2Api: Boolean,
    val intervalMinutes: Int,
)

/**
 * PLAN_V22 P6.7 / PLAN_V24 P10.2: backs Settings' Mileage-Sync card. [metrics] is the staging/synced
 * counters; [uiState] resolves the sync toggles + interval from the [PluginRegistry] (persisted
 * default) layered under the [CurrentTrackDataSource] current-journey override. `applyToFutureJourneys`
 * chooses the write target: ON ⇒ update the persisted default (registry); OFF ⇒ this journey only
 * (the session override). Turning a sync toggle ON first runs a simulated connectivity self-test.
 */
class SyncDiagnosticsViewModel(
    private val repository: com.mileway.feature.profile.repository.SyncDiagnosticsRepository,
    private val pluginRegistry: PluginRegistry,
    private val currentTrack: CurrentTrackDataSource,
) : ViewModel() {
    val metrics: StateFlow<SyncMetrics> = repository.metrics

    private val applyToFutureFlow = MutableStateFlow(true)
    private val guardFlow = MutableStateFlow(SyncGuardState.Idle)

    private val defaultsFlow: Flow<SyncDefaults> =
        combine(
            pluginRegistry.observe("sync_location_enabled"),
            pluginRegistry.observe("sync_events_enabled"),
            pluginRegistry.observe("sync_debug_events_enabled"),
            pluginRegistry.observe("sync_v2_api_enabled"),
            pluginRegistry.observeValue("sync_interval_minutes"),
        ) { location, events, debug, v2, interval ->
            SyncDefaults(location, events, debug, v2, (interval as? PluginValue.IntVal)?.value ?: DEFAULT_INTERVAL_MIN)
        }

    val uiState: StateFlow<SyncSettingsUiState> =
        combine(
            defaultsFlow,
            currentTrack.syncSessionOverrideFlow,
            currentTrack.currentTrackFlow,
            applyToFutureFlow,
            guardFlow,
        ) { defaults, override, track, applyFuture, guard ->
            val active = track.isTracking && override != null
            if (active) {
                SyncSettingsUiState(
                    locationEnabled = override!!.locationEnabled,
                    eventsEnabled = override.eventsEnabled,
                    debugEventsEnabled = override.debugEventsEnabled,
                    v2ApiEnabled = override.v2ApiEnabled,
                    intervalMinutes = override.intervalMinutes,
                    applyToFutureJourneys = applyFuture,
                    sessionOverrideActive = true,
                    guard = guard,
                )
            } else {
                SyncSettingsUiState(
                    locationEnabled = defaults.location,
                    eventsEnabled = defaults.events,
                    debugEventsEnabled = defaults.debugEvents,
                    v2ApiEnabled = defaults.v2Api,
                    intervalMinutes = defaults.intervalMinutes,
                    applyToFutureJourneys = applyFuture,
                    sessionOverrideActive = false,
                    guard = guard,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SyncSettingsUiState())

    fun forceSync() {
        viewModelScope.launch { repository.forceSync(uiState.value.toSyncConfig()) }
    }

    fun setLocationSync(on: Boolean) = onToggle("sync_location_enabled", on) { it.copy(locationEnabled = on) }

    fun setEventSync(on: Boolean) = onToggle("sync_events_enabled", on) { it.copy(eventsEnabled = on) }

    fun setDebugEvents(on: Boolean) = onToggle("sync_debug_events_enabled", on) { it.copy(debugEventsEnabled = on) }

    fun setV2Api(on: Boolean) = onToggle("sync_v2_api_enabled", on) { it.copy(v2ApiEnabled = on) }

    fun setInterval(minutes: Int) {
        viewModelScope.launch { persist("sync_interval_minutes", PluginValue.IntVal(minutes)) { it.copy(intervalMinutes = minutes) } }
    }

    /**
     * `applyToFutureJourneys` = the reference app's apply-target switch. ON clears any current-journey override
     * (edits become the persisted default); OFF snapshots the current effective settings as the
     * starting current-journey override.
     */
    fun setApplyToFutureJourneys(on: Boolean) {
        applyToFutureFlow.value = on
        viewModelScope.launch {
            if (on) {
                currentTrack.setSyncSessionOverride(null)
            } else {
                val cur = uiState.value
                currentTrack.setSyncSessionOverride(
                    SyncSessionOverride(
                        locationEnabled = cur.locationEnabled,
                        eventsEnabled = cur.eventsEnabled,
                        debugEventsEnabled = cur.debugEventsEnabled,
                        v2ApiEnabled = cur.v2ApiEnabled,
                        intervalMinutes = cur.intervalMinutes,
                    ),
                )
            }
        }
    }

    /** Turning a toggle ON runs the simulated connectivity self-test first; OFF applies immediately. */
    private fun onToggle(
        key: String,
        on: Boolean,
        apply: (SyncSettingsUiState) -> SyncSettingsUiState,
    ) {
        viewModelScope.launch {
            if (on) {
                guardFlow.value = SyncGuardState.Testing
                delay(GUARD_TEST_DELAY_MS)
                guardFlow.value = SyncGuardState.Passed
            }
            persist(key, PluginValue.Bool(on), apply)
            if (on) {
                delay(GUARD_RESULT_LINGER_MS)
                guardFlow.value = SyncGuardState.Idle
            }
        }
    }

    /** Writes to the persisted default (registry) or the current-journey override per apply-target. */
    private suspend fun persist(
        key: String,
        value: PluginValue,
        apply: (SyncSettingsUiState) -> SyncSettingsUiState,
    ) {
        val cur = uiState.value
        if (cur.applyToFutureJourneys) {
            pluginRegistry.setUserOverride(key, value)
        } else {
            val next = apply(cur)
            currentTrack.setSyncSessionOverride(
                SyncSessionOverride(
                    locationEnabled = next.locationEnabled,
                    eventsEnabled = next.eventsEnabled,
                    debugEventsEnabled = next.debugEventsEnabled,
                    v2ApiEnabled = next.v2ApiEnabled,
                    intervalMinutes = next.intervalMinutes,
                ),
            )
        }
    }

    private companion object {
        const val DEFAULT_INTERVAL_MIN = 15
        const val STOP_TIMEOUT_MS = 5000L
        const val GUARD_TEST_DELAY_MS = 700L
        const val GUARD_RESULT_LINGER_MS = 500L
    }
}
