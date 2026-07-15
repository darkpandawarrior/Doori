package com.mileway.feature.tracking.debug

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginCategory
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.feature.tracking.export.TrackExportManager
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.ui.components.ExportFormat
import com.siddharth.kmp.mvi.BaseViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DebugMenuComposeUiState(
    val debugMenuUiState: DebugMenuUiState = DebugMenuUiState(),
    val searchQuery: String = "",
    val availableProfiles: List<DebugProfile> = DebugProfiles.allProfiles,
    val selectedProfile: DebugProfile? = null,
    val loginStatus: Boolean = false,
    val abnormalConfigValues: Map<String, String> = emptyMap(),
)

sealed interface DebugMenuComposeAction {
    data class UpdateSearchQuery(val query: String) : DebugMenuComposeAction

    data class ToggleSection(val section: DebugSection) : DebugMenuComposeAction

    data class ToggleCoreOption(val name: String) : DebugMenuComposeAction

    data class ToggleOriginOption(val name: String) : DebugMenuComposeAction

    data class ToggleAuthOption(val name: String) : DebugMenuComposeAction

    data class ToggleFeatureOption(val name: String) : DebugMenuComposeAction

    data class ToggleTrackingOption(val name: String) : DebugMenuComposeAction

    data class UpdateCustomValue(val key: String, val value: String) : DebugMenuComposeAction

    data class SelectProfile(val profile: DebugProfile) : DebugMenuComposeAction

    data object ClearAllDebugSettings : DebugMenuComposeAction

    data object RunGarbageCollection : DebugMenuComposeAction

    data object SaveDebugConfiguration : DebugMenuComposeAction

    data object RestoreDebugConfiguration : DebugMenuComposeAction

    data object RefreshLoginStatus : DebugMenuComposeAction

    data object CheckLoginStatus : DebugMenuComposeAction
}

sealed interface DebugMenuComposeEffect

class DebugMenuComposeViewModel(
    private val savedTrackRepository: SavedTrackRepository,
    private val locationRepository: LocationRepository,
    // PLAN_V24 P10.3: the location fine-tuning knobs are registry-backed VALUE plugins now. The
    // debug menu reads their resolved values for display (the editor itself lives on the Master
    // Plugin page). Nullable so graphs that omit core:data (e.g. the screenshot harness) still build.
    private val pluginRegistry: PluginRegistry? = null,
) : BaseViewModel<DebugMenuComposeUiState, DebugMenuComposeEffect, DebugMenuComposeAction>(DebugMenuComposeUiState()) {
    companion object {
        private const val TAG = "DebugMenuViewModel"
    }

    private val abnormalConfigEvents =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val loginEvents =
        MutableSharedFlow<Unit>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val mutableAbnormalConfig = MutableStateFlow<Map<String, String>>(emptyMap())

    init {
        loadDebugOptions()

        // P10.3: live-mirror the resolved fine-tuning values from the registry (was emptyMap()).
        pluginRegistry?.let { reg ->
            viewModelScope.launch {
                reg.observeResolved().collect { resolved ->
                    mutableAbnormalConfig.value =
                        resolved
                            .filter { it.descriptor.category == PluginCategory.TRACKING_TUNING }
                            .associate { it.descriptor.id to it.value.toRaw() }
                }
            }
        }
        viewModelScope.launch {
            abnormalConfigEvents.collect { loadAbnormalConfigValues() }
        }
        viewModelScope.launch {
            loginEvents.collect { refreshLoginStatus() }
        }
        viewModelScope.launch {
            loginEvents.emit(Unit)
        }
        viewModelScope.launch {
            mutableAbnormalConfig.collect { cfg ->
                setState { copy(abnormalConfigValues = cfg) }
            }
        }
    }

    override fun onAction(action: DebugMenuComposeAction) {
        when (action) {
            is DebugMenuComposeAction.UpdateSearchQuery ->
                setState { copy(searchQuery = action.query) }
            is DebugMenuComposeAction.ToggleSection -> {
                val sections = currentState.debugMenuUiState.expandedSections.toMutableMap()
                sections[action.section] = !(sections[action.section] ?: false)
                setDebugMenuState { copy(expandedSections = sections) }
            }
            is DebugMenuComposeAction.ToggleCoreOption ->
                toggleInMap(action.name, { debugMenuUiState.coreOptions }) { updated ->
                    setDebugMenuState { copy(coreOptions = updated) }
                }
            is DebugMenuComposeAction.ToggleOriginOption ->
                toggleInMap(action.name, { debugMenuUiState.originOptions }, mutuallyExclusive = true) { updated ->
                    setDebugMenuState { copy(originOptions = updated) }
                }
            is DebugMenuComposeAction.ToggleAuthOption ->
                toggleInMap(action.name, { debugMenuUiState.authOptions }) { updated ->
                    setDebugMenuState { copy(authOptions = updated) }
                }
            is DebugMenuComposeAction.ToggleFeatureOption ->
                toggleInMap(action.name, { debugMenuUiState.featureOptions }) { updated ->
                    setDebugMenuState { copy(featureOptions = updated) }
                }
            is DebugMenuComposeAction.ToggleTrackingOption ->
                toggleInMap(action.name, { debugMenuUiState.trackingOptions }) { updated ->
                    setDebugMenuState { copy(trackingOptions = updated) }
                }
            is DebugMenuComposeAction.UpdateCustomValue -> {
                val values = currentState.debugMenuUiState.customValues.toMutableMap()
                values[action.key] = action.value
                setDebugMenuState { copy(customValues = values) }
                setState { copy(selectedProfile = null) }
            }
            is DebugMenuComposeAction.SelectProfile -> {
                applyProfileToDebugState(action.profile)
                setState { copy(selectedProfile = action.profile) }
            }
            DebugMenuComposeAction.ClearAllDebugSettings -> {
                loadDebugOptions()
                setState { copy(selectedProfile = null) }
            }
            DebugMenuComposeAction.RunGarbageCollection ->
                viewModelScope.launch {
                    try {
                        val runtime = Runtime.getRuntime()
                        val before = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                        System.gc()
                        delay(100)
                        val after = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                        Napier.d("GC completed: freed ${before - after}MB, current: ${after}MB", tag = TAG)
                    } catch (e: Exception) {
                        Napier.e("Error running GC: ${e.message}", e, tag = TAG)
                    }
                }
            DebugMenuComposeAction.SaveDebugConfiguration ->
                setDebugMenuState { copy(hasSavedConfig = true, savedConfigSummary = "In-memory only") }
            DebugMenuComposeAction.RestoreDebugConfiguration -> loadDebugOptions()
            DebugMenuComposeAction.RefreshLoginStatus -> viewModelScope.launch { refreshLoginStatus() }
            DebugMenuComposeAction.CheckLoginStatus -> viewModelScope.launch { loginEvents.emit(Unit) }
        }
    }

    // ── Convenience helpers ───────────────────────────────────────────────────

    private fun setDebugMenuState(transform: DebugMenuUiState.() -> DebugMenuUiState) {
        setState { copy(debugMenuUiState = debugMenuUiState.transform().withUpdatedCount()) }
    }

    private fun toggleInMap(
        name: String,
        mapSelector: DebugMenuComposeUiState.() -> Map<String, Boolean>,
        mutuallyExclusive: Boolean = false,
        applyUpdated: (Map<String, Boolean>) -> Unit,
    ) {
        val current = currentState.mapSelector()
        val newValue = !(current[name] ?: false)
        val updated = current.toMutableMap()
        if (mutuallyExclusive && newValue) updated.keys.forEach { updated[it] = false }
        updated[name] = newValue
        applyUpdated(updated)
        setState { copy(selectedProfile = null) }
    }

    // ── Context-taking methods (kept as direct calls) ─────────────────────────

    fun updateAbnormalConfigEntry(
        key: String,
        value: String,
        @Suppress("UNUSED_PARAMETER") context: Context,
    ) {
        viewModelScope.launch {
            val updated = mutableAbnormalConfig.value.toMutableMap()
            updated[key] = value
            mutableAbnormalConfig.value = updated
        }
    }

    fun resetAbnormalConfigToDefaults(
        @Suppress("UNUSED_PARAMETER") context: Context,
    ) {
        viewModelScope.launch { loadAbnormalConfigValues() }
    }

    fun performAppRestart(context: Context) {
        AppRestartUtils.performAppRestart(context)
    }

    fun clearAppCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.cacheDir.deleteRecursively()
                context.cacheDir.mkdirs()
                Napier.d("App cache cleared", tag = TAG)
            } catch (e: Exception) {
                Napier.e("Error clearing cache: ${e.message}", e, tag = TAG)
            }
        }
    }

    fun exportMostRecentTrack(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (savedTrackRepository.count() == 0L) {
                    Napier.w("No saved tracks to export", tag = TAG)
                    return@launch
                }
                val track =
                    savedTrackRepository.getActiveTrack()
                        ?: return@launch Unit.also { Napier.w("No active track found", tag = TAG) }
                val locations = locationRepository.getForToken(track.routeId)
                val content =
                    TrackExportManager.buildContent(
                        format = ExportFormat.GPX,
                        track = track,
                        locations = locations,
                        events = emptyList(),
                    )
                val intent =
                    TrackExportManager.buildShareIntent(
                        context = context,
                        format = ExportFormat.GPX,
                        trackName = track.routeId,
                        content = content,
                    )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                withContext(Dispatchers.Main) { context.startActivity(intent) }
            } catch (e: Exception) {
                Napier.e("Error exporting track: ${e.message}", e, tag = TAG)
            }
        }
    }

    // ── Return-value methods (kept as direct calls) ───────────────────────────

    fun applyChanges(): ApplyResult {
        val count = currentState.debugMenuUiState.enabledOptionsCount
        return ApplyResult(changesApplied = count > 0, restartRequired = true, changeCount = count)
    }

    fun getUserSessionInfo(): Map<String, String> =
        mapOf(
            "Login Status" to "Logged Out",
            "API Origin" to "Not set",
            "Debug Mode" to "Enabled",
            "Save Config" to "Disabled",
        )

    fun performApiHealthCheck() {
        Napier.d("API health check requested (stub in Mileway)", tag = TAG)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private suspend fun loadAbnormalConfigValues() {
        val registry = pluginRegistry ?: return
        mutableAbnormalConfig.value =
            registry.observeResolved().first()
                .filter { it.descriptor.category == PluginCategory.TRACKING_TUNING }
                .associate { it.descriptor.id to it.value.toRaw() }
    }

    private fun loadDebugOptions() {
        viewModelScope.launch {
            try {
                setDebugMenuState {
                    copy(
                        coreOptions = loadCoreOptions(),
                        originOptions = loadOriginOptions(),
                        authOptions = loadAuthOptions(),
                        featureOptions = loadFeatureOptions(),
                        trackingOptions = loadTrackingOptions(),
                        customValues = loadCustomValues(),
                        expandedSections =
                            mapOf(
                                DebugSection.CORE to false,
                                DebugSection.ORIGIN to false,
                                DebugSection.AUTH to false,
                                DebugSection.FEATURES to false,
                                DebugSection.TRACKING to false,
                                DebugSection.PROFILES to false,
                            ),
                        apiOrigin = "Not set",
                        referralParams = getReferralParams(),
                        isLoggedIn = false,
                        hasSavedConfig = false,
                        savedConfigSummary = "",
                    )
                }
            } catch (e: Exception) {
                Napier.e("Error loading debug options: ${e.message}", e, tag = TAG)
            }
        }
    }

    private fun refreshLoginStatus() {
        val loggedIn = false
        setState {
            copy(
                loginStatus = loggedIn,
                debugMenuUiState =
                    debugMenuUiState.copy(
                        isLoggedIn = loggedIn,
                        apiOrigin = if (loggedIn) "Not set" else "Not logged in",
                    ).withUpdatedCount(),
            )
        }
    }

    private fun applyProfileToDebugState(profile: DebugProfile) {
        val dm = currentState.debugMenuUiState
        val core = dm.coreOptions.toMutableMap().also { it.keys.forEach { k -> it[k] = false } }
        val origin = dm.originOptions.toMutableMap().also { it.keys.forEach { k -> it[k] = false } }
        val auth = dm.authOptions.toMutableMap().also { it.keys.forEach { k -> it[k] = false } }
        val feature = dm.featureOptions.toMutableMap().also { it.keys.forEach { k -> it[k] = false } }
        val tracking = dm.trackingOptions.toMutableMap().also { it.keys.forEach { k -> it[k] = false } }
        val custom = dm.customValues.toMutableMap()

        profile.options.forEach { (key, value) ->
            when {
                core.containsKey(key) -> core[key] = value
                origin.containsKey(key) -> origin[key] = value
                auth.containsKey(key) -> auth[key] = value
                feature.containsKey(key) -> feature[key] = value
                tracking.containsKey(key) -> tracking[key] = value
            }
        }
        profile.customValues.forEach { (k, v) -> custom[k] = v }

        setDebugMenuState {
            copy(
                coreOptions = core,
                originOptions = origin,
                authOptions = auth,
                featureOptions = feature,
                trackingOptions = tracking,
                customValues = custom,
            )
        }
    }

    private fun countEnabledOptions(state: DebugMenuUiState): Int =
        with(state) {
            coreOptions.count { it.value } +
                originOptions.count { it.value } +
                authOptions.count { it.value } +
                featureOptions.count { it.value } +
                trackingOptions.count { it.value }
        }

    private fun DebugMenuUiState.withUpdatedCount(): DebugMenuUiState = copy(enabledOptionsCount = countEnabledOptions(this))

    private fun getReferralParams() = "client_code=-, login_mode=-, region_code=-"

    private fun loadCoreOptions(): Map<String, Boolean> =
        mapOf(
            "Enable Logging For Release" to false,
            "Force Logout Enabled" to false,
            "Force Super Delegate Mode" to false,
            "Enable Tracking Overlay" to false,
            "Save Debug Configuration" to false,
        )

    private fun loadOriginOptions(): Map<String, Boolean> =
        mapOf(
            "Force UAT" to false,
            "Force Prod" to false,
            "Force Dev Environment" to false,
            "Force Custom Origin" to false,
            "Force MENA Login" to false,
            "Force File Prod" to false,
        )

    private fun loadAuthOptions(): Map<String, Boolean> =
        mapOf(
            "Use Custom OTP" to false,
            "Skip OTP Pin Screen" to false,
        )

    private fun loadFeatureOptions(): Map<String, Boolean> =
        mapOf(
            "Enable Custom Banner" to false,
            "Hide Debug Banner" to false,
            "Enable Trip Deactivation" to false,
            "Force Editable Form" to false,
            "Force Expense" to false,
            "Force Corporate" to false,
            "Force Travel" to false,
            "Force Travel Old UI" to false,
            "Force Track Miles V2 UI" to false,
            "Force Single Invoice" to false,
            "Force Multi Invoice" to false,
            "Force PR" to false,
            "Force Events" to false,
            "Force Cards" to false,
            "Force Cards V2 UI" to false,
            "Force Club" to false,
            "Force Fave App Redirection" to false,
            "Enable Draft Log Miles" to false,
            "Enable Track Miles to Log Miles Draft" to false,
        )

    private fun loadTrackingOptions(): Map<String, Boolean> =
        mapOf(
            "Skip Odometer" to false,
            "Toggle Odometer OCR" to false,
            "Allow Mock Locations" to false,
            "Enable Location Dump Creation" to false,
            "Force BE Distance Calculation" to false,
            "Bypass Battery Level Check" to false,
            "Bypass Battery Optimization Check" to false,
            "Use V2 Location Sync API" to false,
        )

    private fun loadCustomValues(): Map<String, String> =
        mapOf(
            "Custom Origin" to "",
            "Custom Banner Text" to "",
            "Custom OTP" to "",
        )
}

data class DebugMenuUiState(
    val coreOptions: Map<String, Boolean> = emptyMap(),
    val originOptions: Map<String, Boolean> = emptyMap(),
    val authOptions: Map<String, Boolean> = emptyMap(),
    val featureOptions: Map<String, Boolean> = emptyMap(),
    val trackingOptions: Map<String, Boolean> = emptyMap(),
    val customValues: Map<String, String> = emptyMap(),
    val expandedSections: Map<DebugSection, Boolean> = emptyMap(),
    val apiOrigin: String = "",
    val referralParams: String = "",
    val isLoggedIn: Boolean = false,
    val enabledOptionsCount: Int = 0,
    val hasSavedConfig: Boolean = false,
    val savedConfigSummary: String = "",
)

enum class DebugSection { CORE, ORIGIN, AUTH, FEATURES, TRACKING, PROFILES }

data class ApplyResult(
    val changesApplied: Boolean,
    val restartRequired: Boolean,
    val changeCount: Int,
)

data class DebugScreenState(
    val uiState: DebugMenuUiState,
    val searchQuery: String,
    val availableProfiles: List<DebugProfile>,
    val selectedProfile: DebugProfile?,
    val isLoggedIn: Boolean,
)
