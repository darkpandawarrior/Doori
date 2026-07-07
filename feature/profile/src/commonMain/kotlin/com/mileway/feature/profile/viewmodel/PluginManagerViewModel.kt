package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.data.plugin.PersonaSummary
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.plugin.PluginValue
import com.mileway.core.data.plugin.ResolvedPlugin
import com.mileway.core.ui.mvi.BaseViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PluginManagerUiState(
    val query: String = "",
    val plugins: List<ResolvedPlugin> = emptyList(),
    val personas: List<PersonaSummary> = emptyList(),
    val experimentalUnlocked: Boolean = false,
    val restartPendingCount: Int = 0,
) {
    /** Search-filtered plugins, hiding experimental ones until unlocked. */
    val visiblePlugins: List<ResolvedPlugin>
        get() =
            plugins.filter { experimentalUnlocked || !it.descriptor.experimental }
                .filter { query.isBlank() || it.descriptor.id.contains(query, ignoreCase = true) }
}

sealed interface PluginManagerAction {
    data class Search(val query: String) : PluginManagerAction

    data class SetToggle(val id: String, val on: Boolean) : PluginManagerAction

    data class SetValue(val id: String, val value: PluginValue) : PluginManagerAction

    data class ClearOverride(val id: String) : PluginManagerAction

    data object ResetToPreset : PluginManagerAction

    data class ApplyPersona(val persona: PersonaSummary, val clearFirst: Boolean) : PluginManagerAction

    /** One tap on the version row; the 7th unlocks the experimental section. */
    data object VersionRowTap : PluginManagerAction
}

sealed interface PluginManagerEffect

/**
 * PLAN_V24 P0.3 — the Master Plugin page's ViewModel. Reads the resolved plugin set + source per
 * flag from [PluginRegistry] and the applicable personas from [PersonaPresetProvider]; writes
 * per-account USER overrides. Toggling a `requiresRestart` plugin bumps a "restart to apply"
 * counter (no live effect yet — none of P0.1's plugins require restart, but the wire is here).
 */
class PluginManagerViewModel(
    private val registry: PluginRegistry,
    private val presets: PersonaPresetProvider,
) : BaseViewModel<PluginManagerUiState, PluginManagerEffect, PluginManagerAction>(PluginManagerUiState()) {
    private var versionTaps = 0

    init {
        setState { copy(personas = presets.availablePersonas()) }
        viewModelScope.launch {
            combine(registry.observeResolved(), registry.observeExperimentalUnlocked()) { resolved, unlocked ->
                resolved to unlocked
            }.collect { (resolved, unlocked) ->
                setState { copy(plugins = resolved, experimentalUnlocked = unlocked) }
            }
        }
    }

    override fun onAction(action: PluginManagerAction) {
        when (action) {
            is PluginManagerAction.Search -> setState { copy(query = action.query) }

            is PluginManagerAction.SetToggle -> {
                bumpRestartIfNeeded(action.id)
                viewModelScope.launch { registry.setUserOverride(action.id, PluginValue.Bool(action.on)) }
            }

            is PluginManagerAction.SetValue -> {
                bumpRestartIfNeeded(action.id)
                viewModelScope.launch { registry.setUserOverride(action.id, action.value) }
            }

            is PluginManagerAction.ClearOverride -> {
                bumpRestartIfNeeded(action.id)
                viewModelScope.launch { registry.clearUserOverride(action.id) }
            }

            PluginManagerAction.ResetToPreset ->
                viewModelScope.launch { registry.resetActiveAccountToPreset() }

            is PluginManagerAction.ApplyPersona ->
                viewModelScope.launch {
                    if (action.clearFirst) registry.resetActiveAccountToPreset()
                    action.persona.overrides.forEach { (id, raw) ->
                        registry.setUserOverride(id, rawToValue(id, raw))
                    }
                }

            PluginManagerAction.VersionRowTap -> {
                versionTaps++
                if (versionTaps >= UNLOCK_TAPS) {
                    viewModelScope.launch { registry.unlockExperimental() }
                }
            }
        }
    }

    private fun bumpRestartIfNeeded(id: String) {
        val requiresRestart = currentState.plugins.firstOrNull { it.descriptor.id == id }?.descriptor?.requiresRestart == true
        if (requiresRestart) setState { copy(restartPendingCount = restartPendingCount + 1) }
    }

    /** Interpret a persona's raw override string against the descriptor kind (bool default). */
    private fun rawToValue(
        id: String,
        raw: String,
    ): PluginValue {
        val descriptor = currentState.plugins.firstOrNull { it.descriptor.id == id }?.descriptor
        return if (descriptor != null) PluginValue.parse(raw, descriptor) else PluginValue.Bool(raw.toBoolean())
    }

    private companion object {
        const val UNLOCK_TAPS = 7
    }
}
