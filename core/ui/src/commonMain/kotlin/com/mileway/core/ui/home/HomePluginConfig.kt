package com.mileway.core.ui.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * P25.A5.3 — MASTER_GAP Area-1's "server-driven feature-flag plugin config gating Home sections",
 * scoped to what a local/offline demo needs: whether each togglable Home section renders at all.
 * All-`true` defaults mean a fresh install renders exactly like today (no goldens move) until a
 * debug screen (V29 P29.H.1) flips a flag.
 */
data class HomePluginConfig(
    val showTrackMiles: Boolean = true,
    val showMyCards: Boolean = true,
    val showCheckIn: Boolean = true,
    val showMarketingStrip: Boolean = true,
)

private object HomePluginConfigKeys {
    val SHOW_TRACK_MILES = booleanPreferencesKey("home_show_track_miles")
    val SHOW_MY_CARDS = booleanPreferencesKey("home_show_my_cards")
    val SHOW_CHECK_IN = booleanPreferencesKey("home_show_check_in")
    val SHOW_MARKETING_STRIP = booleanPreferencesKey("home_show_marketing_strip")
}

/**
 * DataStore-backed read/write seam for [HomePluginConfig] — mirrors [com.mileway.core.ui.theme.ThemeController]'s
 * pattern exactly (same injected `DataStore<Preferences>` singleton, same load-once-then-persist-
 * per-write shape) so it's a Koin `single` the debug screen and `HomeViewModel` both share. A `null`
 * store (unit tests) keeps this purely in-memory.
 */
class HomePluginConfigController(
    private val prefs: DataStore<Preferences>? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _config = MutableStateFlow(HomePluginConfig())
    val config: StateFlow<HomePluginConfig> = _config.asStateFlow()

    init {
        prefs?.let { store ->
            scope.launch {
                // A corrupt or unavailable store must never crash the app, fall back to defaults.
                val snap =
                    try {
                        store.data.first()
                    } catch (_: Throwable) {
                        return@launch
                    }
                _config.value =
                    HomePluginConfig(
                        showTrackMiles = snap[HomePluginConfigKeys.SHOW_TRACK_MILES] ?: true,
                        showMyCards = snap[HomePluginConfigKeys.SHOW_MY_CARDS] ?: true,
                        showCheckIn = snap[HomePluginConfigKeys.SHOW_CHECK_IN] ?: true,
                        showMarketingStrip = snap[HomePluginConfigKeys.SHOW_MARKETING_STRIP] ?: true,
                    )
            }
        }
    }

    /** Applies [transform] to the current config and persists the result (best-effort). */
    fun update(transform: (HomePluginConfig) -> HomePluginConfig) {
        val next = transform(_config.value)
        _config.value = next
        prefs?.let { store ->
            scope.launch {
                try {
                    store.edit { p -> p.writeAll(next) }
                } catch (_: Throwable) {
                    // Persistence is best-effort; the in-memory state is already updated.
                }
            }
        }
    }

    private fun MutablePreferences.writeAll(value: HomePluginConfig) {
        this[HomePluginConfigKeys.SHOW_TRACK_MILES] = value.showTrackMiles
        this[HomePluginConfigKeys.SHOW_MY_CARDS] = value.showMyCards
        this[HomePluginConfigKeys.SHOW_CHECK_IN] = value.showCheckIn
        this[HomePluginConfigKeys.SHOW_MARKETING_STRIP] = value.showMarketingStrip
    }
}
