package com.miletracker.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.miletracker.core.common.UiText
import com.miletracker.core.data.model.network.ApprovedVehicle
import com.miletracker.core.data.model.network.CoordsV2
import com.miletracker.core.data.model.network.ExpenseSubmissionResponse
import com.miletracker.core.data.model.network.LogMilesService
import com.miletracker.core.data.model.network.LogMilesSubmitRequestV2
import com.miletracker.core.data.model.network.SubmissionStatus
import com.miletracker.core.ui.mvi.BaseViewModel
import com.miletracker.feature.logging.repository.LogMilesDraftRepository
import com.miletracker.feature.logging.repository.LogMilesServiceRepository
import com.miletracker.feature.logging.ui.model.LocationEntry
import com.miletracker.feature.logging.ui.model.LocationStop
import com.miletracker.feature.logging.ui.model.SubmittedVoucher
import com.miletracker.feature.logging.ui.model.SubmittedVoucherSamples
import com.miletracker.feature.logging.ui.model.totalRouteKm
import com.miletracker.feature.logging.usecase.LogMilesSubmitUseCase
import com.miletracker.feature.tracking.repository.VehiclePricingRepository
import kotlinx.coroutines.launch

/**
 * A draft the user saved at the end of Step 1 (or that came back from the draft
 * repository). Held in memory so the History → Drafts tab works even when no Room
 * draft DAO is wired. Mirrors the minimum a draft needs to be re-opened.
 */
data class LogMilesDraftUi(
    val id: String,
    val title: String,
    val stopCount: Int,
    val distanceKm: Double,
    val vehicleName: String?,
    val updatedAtMillis: Long,
)

/**
 * Single immutable UI state for the entire two-step Log Miles flow plus history.
 * The Step 1 and Step 2 screens, history screen and every sheet/dialog read from
 * this one object; the screens stay stateless and emit Actions back to the VM.
 */
data class LogMilesUiState(
    // ── Reference data ────────────────────────────────────────────────────────
    val vehicles: List<ApprovedVehicle> = emptyList(),
    val selectedVehicle: ApprovedVehicle? = null,
    val services: List<LogMilesService> = emptyList(),
    val selectedService: LogMilesService? = null,
    val isLoadingVehicles: Boolean = true,
    val isLoadingServices: Boolean = true,
    // ── Step 1: itinerary ─────────────────────────────────────────────────────
    val stops: List<LocationStop> = emptyList(),
    val isRoundTrip: Boolean = false,
    /** Great-circle distance computed from the current stops. */
    val calculatedDistanceKm: Double = 0.0,
    /** Distance actually used for pricing, equal to calculated unless overridden. */
    val distanceKm: Double = 0.0,
    val isDistanceOverridden: Boolean = false,
    val reimbursableAmount: Double = 0.0,
    val journeyDateMillis: Long? = null,
    val journeyTimeMinutes: Int? = null,
    val saveAsDraft: Boolean = false,
    val recentLocations: List<LocationEntry> = emptyList(),
    // ── Step 2: expense details ───────────────────────────────────────────────
    val invoiceDateMillis: Long? = null,
    val logMilesNote: String = "",
    val taggedEmployees: List<String> = emptyList(),
    val attachmentCount: Int = 0,
    // ── Submission ────────────────────────────────────────────────────────────
    val isSubmitting: Boolean = false,
    val submissionResult: ExpenseSubmissionResponse? = null,
    val showViolationDialog: Boolean = false,
    // ── History ───────────────────────────────────────────────────────────────
    val drafts: List<LogMilesDraftUi> = emptyList(),
    val submitted: List<SubmittedVoucher> =
        SubmittedVoucherSamples.sample(kotlin.time.Clock.System.now().toEpochMilliseconds()),
) {
    /** Step 1 → Step 2 gate: at least two stops and a chosen vehicle. */
    val canProceedToStep2: Boolean get() = stops.size >= 2 && selectedVehicle != null

    /** Per-km rate of the selected vehicle (0 when none chosen). */
    val pricePerKm: Double get() = selectedVehicle?.vehiclePricing ?: 0.0

    /** Number of still-required Step 2 fields (drives the "N remaining" header). */
    val step2Remaining: Int
        get() = listOf(invoiceDateMillis != null, logMilesNote.isNotBlank()).count { !it }
}

sealed interface LogMilesAction {
    data object Refresh : LogMilesAction

    data class SelectVehicle(val vehicle: ApprovedVehicle) : LogMilesAction

    data class SelectService(val service: LogMilesService) : LogMilesAction

    data class SetJourneyDate(val millis: Long?) : LogMilesAction

    data class SetJourneyTime(val hour: Int, val minute: Int) : LogMilesAction

    data class SetRoundTrip(val enabled: Boolean) : LogMilesAction

    data class SetSaveAsDraft(val enabled: Boolean) : LogMilesAction

    data class AddStop(val entry: LocationEntry) : LogMilesAction

    data class InsertStopAfter(val afterIndex: Int, val entry: LocationEntry) : LogMilesAction

    data class EditStop(val stopId: Long, val entry: LocationEntry) : LogMilesAction

    data class RemoveStop(val stopId: Long) : LogMilesAction

    data class MoveStopUp(val index: Int) : LogMilesAction

    data class MoveStopDown(val index: Int) : LogMilesAction

    data object ClearRecentLocations : LogMilesAction

    data class OverrideDistance(val km: Double) : LogMilesAction

    data class SetInvoiceDate(val millis: Long?) : LogMilesAction

    data class SetLogMilesNote(val text: String) : LogMilesAction

    data class SetTaggedEmployees(val names: List<String>) : LogMilesAction

    data object AddAttachment : LogMilesAction

    data object SaveDraft : LogMilesAction

    data class DeleteDraft(val draftId: String) : LogMilesAction

    data object Submit : LogMilesAction

    data object DismissViolationDialog : LogMilesAction

    data object ResetSubmission : LogMilesAction
}

sealed interface LogMilesEffect {
    data class ShowError(val message: UiText) : LogMilesEffect
}

/**
 * ViewModel for the standalone, offline Log Miles flow.
 *
 * Distance is computed locally via great-circle math over the offline city
 * catalogue. All itinerary edits, draft toggling, the verify-distance override,
 * the Step 2 form and the tagged-employee selection mutate the single
 * [LogMilesUiState]; the network submit is delegated to [LogMilesSubmitUseCase].
 */
class LogMilesViewModel(
    private val vehicleRepo: VehiclePricingRepository,
    private val serviceRepo: LogMilesServiceRepository,
    private val draftRepo: LogMilesDraftRepository,
    private val submitUseCase: LogMilesSubmitUseCase,
) : BaseViewModel<LogMilesUiState, LogMilesEffect, LogMilesAction>(LogMilesUiState()) {
    /** Backwards-compatible alias; screens read [state]. */
    val uiState = state

    /** Monotonic id source for stops added during this session. */
    private var stopIdCounter = 0L

    init {
        loadVehicles()
        loadServices()
    }

    override fun onAction(action: LogMilesAction) {
        when (action) {
            LogMilesAction.Refresh -> {
                loadVehicles()
                loadServices()
            }
            is LogMilesAction.SelectVehicle -> selectVehicle(action.vehicle)
            is LogMilesAction.SelectService -> setState { copy(selectedService = action.service) }
            is LogMilesAction.SetJourneyDate -> setState { copy(journeyDateMillis = action.millis) }
            is LogMilesAction.SetJourneyTime ->
                setState { copy(journeyTimeMinutes = action.hour * 60 + action.minute) }
            is LogMilesAction.SetRoundTrip -> {
                setState { copy(isRoundTrip = action.enabled) }
                recomputeDistance()
            }
            is LogMilesAction.SetSaveAsDraft -> setState { copy(saveAsDraft = action.enabled) }
            is LogMilesAction.AddStop -> addStop(action.entry)
            is LogMilesAction.InsertStopAfter -> insertStopAfter(action.afterIndex, action.entry)
            is LogMilesAction.EditStop -> editStop(action.stopId, action.entry)
            is LogMilesAction.RemoveStop -> {
                setState { copy(stops = stops.filterNot { it.id == action.stopId }) }
                recomputeDistance()
            }
            is LogMilesAction.MoveStopUp -> reorder(action.index, action.index - 1)
            is LogMilesAction.MoveStopDown -> reorder(action.index, action.index + 1)
            LogMilesAction.ClearRecentLocations -> setState { copy(recentLocations = emptyList()) }
            is LogMilesAction.OverrideDistance -> {
                setState { copy(distanceKm = action.km, isDistanceOverridden = true) }
                recomputePricing()
            }
            is LogMilesAction.SetInvoiceDate -> setState { copy(invoiceDateMillis = action.millis) }
            is LogMilesAction.SetLogMilesNote -> setState { copy(logMilesNote = action.text) }
            is LogMilesAction.SetTaggedEmployees -> setState { copy(taggedEmployees = action.names) }
            LogMilesAction.AddAttachment -> setState { copy(attachmentCount = attachmentCount + 1) }
            LogMilesAction.SaveDraft -> saveDraft()
            is LogMilesAction.DeleteDraft -> deleteDraft(action.draftId)
            LogMilesAction.Submit -> submit()
            LogMilesAction.DismissViolationDialog -> setState { copy(showViolationDialog = false) }
            LogMilesAction.ResetSubmission -> resetSubmission()
        }
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            runCatching { vehicleRepo.getVehicles(trackMiles = false) }
                .onSuccess { v -> setState { copy(vehicles = v, isLoadingVehicles = false) } }
                .onFailure { setState { copy(isLoadingVehicles = false) } }
        }
    }

    private fun loadServices() {
        viewModelScope.launch {
            runCatching { serviceRepo.getServices() }
                .onSuccess { s ->
                    setState { copy(services = s, selectedService = s.firstOrNull(), isLoadingServices = false) }
                }
                .onFailure { setState { copy(isLoadingServices = false) } }
        }
    }

    private fun selectVehicle(vehicle: ApprovedVehicle) {
        setState { copy(selectedVehicle = vehicle) }
        recomputePricing()
    }

    private fun addStop(entry: LocationEntry) {
        val stop = LocationStop(id = stopIdCounter++, entry = entry)
        setState {
            copy(
                stops = stops + stop,
                recentLocations = (listOf(entry) + recentLocations).distinctBy { it.name }.take(8),
            )
        }
        recomputeDistance()
    }

    private fun insertStopAfter(
        afterIndex: Int,
        entry: LocationEntry,
    ) {
        val stop = LocationStop(id = stopIdCounter++, entry = entry)
        setState {
            val list = stops.toMutableList()
            val target = (afterIndex + 1).coerceIn(0, list.size)
            list.add(target, stop)
            copy(
                stops = list,
                recentLocations = (listOf(entry) + recentLocations).distinctBy { it.name }.take(8),
            )
        }
        recomputeDistance()
    }

    private fun editStop(
        stopId: Long,
        entry: LocationEntry,
    ) {
        setState { copy(stops = stops.map { if (it.id == stopId) it.copy(entry = entry) else it }) }
        recomputeDistance()
    }

    private fun reorder(
        from: Int,
        to: Int,
    ) {
        setState {
            if (from !in stops.indices || to !in stops.indices) return@setState this
            val list = stops.toMutableList()
            val moved = list.removeAt(from)
            list.add(to, moved)
            copy(stops = list)
        }
        recomputeDistance()
    }

    /**
     * Recompute the great-circle distance from the current stops. The override is
     * cleared whenever the itinerary changes so a stale manual value can't outlive
     * the route it was based on.
     */
    private fun recomputeDistance() {
        setState {
            val calc = totalRouteKm(stops, isRoundTrip)
            copy(calculatedDistanceKm = calc, distanceKm = calc, isDistanceOverridden = false)
        }
        recomputePricing()
    }

    private fun recomputePricing() {
        setState { copy(reimbursableAmount = distanceKm * pricePerKm) }
    }

    private fun saveDraft() {
        val s = currentState
        if (s.stops.isEmpty()) return
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val draft =
            LogMilesDraftUi(
                id = "draft-$now",
                title = s.stops.firstOrNull()?.entry?.name?.substringBefore(",") ?: "Log Miles draft",
                stopCount = s.stops.size,
                distanceKm = s.distanceKm,
                vehicleName = s.selectedVehicle?.vehicleName,
                updatedAtMillis = now,
            )
        setState { copy(drafts = listOf(draft) + drafts) }
    }

    private fun deleteDraft(draftId: String) {
        setState { copy(drafts = drafts.filterNot { d -> d.id == draftId }) }
        viewModelScope.launch { runCatching { draftRepo.delete(draftId) } }
    }

    /**
     * Submit the journey via [LogMilesSubmitUseCase]. On success with policy
     * violations we surface a violation dialog; failures emit a [LogMilesEffect.ShowError].
     */
    private fun submit() {
        val s = currentState
        val vehicle = s.selectedVehicle ?: return
        if (s.stops.size < 2) return
        setState { copy(isSubmitting = true) }
        viewModelScope.launch {
            submitUseCase(
                LogMilesSubmitRequestV2(
                    vehicleType = vehicle.vehicleKey,
                    distance = s.distanceKm,
                    roundTrip = s.isRoundTrip,
                    date = s.journeyDateMillis,
                    origin = s.stops.first().entry.let { CoordsV2(it.lat, it.lng, it.name) },
                    destination = s.stops.last().entry.let { CoordsV2(it.lat, it.lng, it.name) },
                ),
            ).onSuccess { resp ->
                val hasViolations =
                    resp.submissionStatus == SubmissionStatus.POLICY_VIOLATION ||
                        resp.violations.isNotEmpty() ||
                        !resp.policyViolations.isNullOrEmpty()
                setState {
                    copy(isSubmitting = false, submissionResult = resp, showViolationDialog = hasViolations)
                }
            }.onFailure { e ->
                setState { copy(isSubmitting = false) }
                emitEffect(LogMilesEffect.ShowError(UiText.Static(e.message ?: "Submission failed")))
            }
        }
    }

    private fun resetSubmission() {
        stopIdCounter = 0L
        setState {
            LogMilesUiState(
                vehicles = vehicles,
                isLoadingVehicles = false,
                services = services,
                selectedService = services.firstOrNull(),
                isLoadingServices = false,
                drafts = drafts,
                submitted = submitted,
                recentLocations = recentLocations,
            )
        }
    }
}
