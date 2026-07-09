package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.DelegationSessionSource
import com.mileway.feature.profile.model.SeededReportees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A reportee a manager can act on behalf of — distinct identity so trips isolate to them (P7.3). */
data class Reportee(
    val name: String,
    val email: String,
    val code: String,
)

/**
 * PLAN_V24 P7.3 state for the "Act on behalf" section of
 * [DelegationScreen][com.mileway.feature.profile.ui.screens.DelegationScreen] and the app-wide
 * "Acting as <name>" banner. [enabled] gates the whole section on the `superDelegateMode` plugin
 * (a manager-only capability, off by default); [isActing]/[actingName] drive the banner.
 */
data class DelegateSessionUiState(
    val enabled: Boolean = false,
    val isActing: Boolean = false,
    val actingName: String? = null,
    val reportees: List<Reportee> = emptyList(),
)

/**
 * Drives session delegation ("act on behalf of a reportee"), separate from the approval-delegation
 * [DelegationViewModel]. The base identity is never touched; [DelegationSessionSource] layers the
 * acting identity on top and trips stamp against it (see `effectiveSignedInIdentity`).
 */
class DelegateSessionViewModel(
    private val delegation: DelegationSessionSource,
    private val pluginRegistry: PluginRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(DelegateSessionUiState(reportees = SeededReportees.all))
    val state: StateFlow<DelegateSessionUiState> = _state.asStateFlow()

    init {
        combine(
            pluginRegistry.observe("superDelegateMode"),
            delegation.delegationState,
        ) { enabled, d -> enabled to d }
            .onEach { (enabled, d) ->
                _state.update {
                    it.copy(enabled = enabled, isActing = d.isActing, actingName = d.actingName)
                }
            }
            .launchIn(viewModelScope)
    }

    /** Begin acting as [reportee] (no-op if already acting — nested delegation is blocked). */
    fun actAs(reportee: Reportee) {
        viewModelScope.launch {
            delegation.startDelegation(reportee.name, reportee.email, reportee.code)
        }
    }

    /** Restore the base identity. */
    fun endDelegation() {
        viewModelScope.launch { delegation.endDelegation() }
    }
}
