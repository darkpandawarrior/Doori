package com.mileway.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.feature.profile.model.ManagerReportees
import com.mileway.feature.profile.model.ReporteeTrip
import com.mileway.feature.profile.model.ReporteeTripSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * PLAN_V24 P10.6 state for the manager tracking view. [enabled] gates the whole surface on the
 * `trackMileageManagerView` plugin (a manager-only capability, off by default; the Corporate
 * Commuter persona turns it on) — mirrors [DelegateSessionViewModel]'s plugin gate.
 */
data class ManagerReporteesUiState(
    val enabled: Boolean = false,
    val summaries: List<ReporteeTripSummary> = emptyList(),
)

/**
 * Drives the manager's reportee list + per-reportee drill-in. The tile that reaches this screen is
 * itself plugin-gated (see `ProfileScreen`), so [enabled] is a defensive gate for the screen when
 * reached directly. Summaries and trip lists are deterministic seeded mock (see [ManagerReportees]).
 */
class ManagerReporteesViewModel(
    private val pluginRegistry: PluginRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(ManagerReporteesUiState(summaries = ManagerReportees.summaries()))
    val state: StateFlow<ManagerReporteesUiState> = _state.asStateFlow()

    init {
        pluginRegistry.observe("trackMileageManagerView")
            .onEach { enabled -> _state.update { it.copy(enabled = enabled) } }
            .launchIn(viewModelScope)
    }

    /** Seeded trip list for one reportee — passthrough for the detail screen. */
    fun tripsFor(code: String): List<ReporteeTrip> = ManagerReportees.tripsFor(code)
}
