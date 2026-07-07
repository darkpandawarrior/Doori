package com.mileway.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.common.UiText
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerPurpose
import com.mileway.core.data.model.network.ApprovedVehicle
import com.mileway.core.data.model.network.CoordsV2
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.LogMilesService
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.validator.OdometerError
import com.mileway.core.data.model.validator.OdometerValidation
import com.mileway.core.data.model.validator.OdometerValidator
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.logging.repository.LogMilesDraftRepository
import com.mileway.feature.logging.repository.LogMilesDraftRepository.Companion.toDraftEntity
import com.mileway.feature.logging.repository.LogMilesFrequentRoute
import com.mileway.feature.logging.repository.LogMilesFrequentRouteRepository
import com.mileway.feature.logging.repository.LogMilesServiceRepository
import com.mileway.feature.logging.ui.model.LocationEntry
import com.mileway.feature.logging.ui.model.LocationStop
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import com.mileway.feature.logging.ui.model.SubmittedVoucherSamples
import com.mileway.feature.logging.ui.model.totalRouteKm
import com.mileway.feature.logging.usecase.LogMilesSubmitUseCase
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A draft the user saved at the end of Step 1, backed by [LogMilesDraftRepository]'s Room store
 * (P5.1). Mirrors the minimum a draft needs to be shown in the History → Drafts tab; the full
 * form state is rehydrated on demand via [LogMilesAction.LoadDraft].
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
    /** Wave 3: most-used routes, for one-tap retrace (see [LogMilesAction.RetraceRoute]). */
    val frequentRoutes: List<LogMilesFrequentRoute> = emptyList(),
    /**
     * Step 1: odometer capture (P5.3). Gates [canProceedToStep2] on odometer capture, sourced from
     * [com.mileway.core.data.settings.DemoSettingsRepository] — a local per-tenant-persona flag,
     * not a server fetch (this demo is offline-first/stub-backed everywhere).
     */
    val odometerCaptureEnabled: Boolean = false,
    val odometerStart: OdometerCaptureResult? = null,
    val odometerEnd: OdometerCaptureResult? = null,
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
    /**
     * Validation error for the current odometer capture, or null when capture is disabled,
     * incomplete-but-not-yet-invalid, or complete and valid. Non-null only once both readings
     * are present and the end reading doesn't exceed the start (P5.3 acceptance case).
     */
    val odometerValidationError: String?
        get() {
            val start = odometerStart ?: return null
            val end = odometerEnd ?: return null
            if (end.reading == start.reading) return "End odometer reading must be greater than start"
            return when (
                val result = OdometerValidator.validate(start.reading, end.reading, end.source)
            ) {
                is OdometerValidation.Valid -> null
                is OdometerValidation.Invalid ->
                    when (result.reason) {
                        OdometerError.DECREMENT -> "End odometer reading must be greater than start"
                        OdometerError.BELOW_BOUNDS -> "Odometer reading cannot be negative"
                        OdometerError.ABOVE_BOUNDS -> "Odometer reading exceeds maximum (${OdometerValidator.MAX_ODOMETER})"
                        OdometerError.IMPLAUSIBLE_JUMP -> "Odometer distance is implausibly large"
                    }
            }
        }

    /** True once both start and end readings are captured with no validation error. */
    private val odometerCaptureComplete: Boolean
        get() = odometerStart != null && odometerEnd != null && odometerValidationError == null

    /**
     * Step 1 → Step 2 gate: at least two stops and a chosen vehicle, plus a complete, valid
     * odometer capture whenever [odometerCaptureEnabled] is on (P5.3), plus an explicit service
     * selection whenever more than one service is available (P5.5) — with a single service (or
     * none loaded yet) there's nothing to choose between, so the gate stays unaffected. Behavior
     * is unchanged when neither condition applies.
     */
    val canProceedToStep2: Boolean
        get() =
            stops.size >= 2 && selectedVehicle != null &&
                (!odometerCaptureEnabled || odometerCaptureComplete) &&
                (services.size <= 1 || selectedService != null)

    /** Per-km rate of the selected vehicle (0 when none chosen). */
    val pricePerKm: Double get() = selectedVehicle?.vehiclePricing ?: 0.0

    /** Number of still-required Step 2 fields (drives the "N remaining" header). */
    val step2Remaining: Int
        get() = listOf(invoiceDateMillis != null, logMilesNote.isNotBlank()).count { !it }
}

/**
 * Builds the network payload from the current form state (P5.2). Extracted as a pure mapper
 * (mirrors [LogMilesDraftRepository.toDraftEntity]) so the submit request shape is unit-testable
 * without standing up the whole ViewModel.
 *
 * @param force            set on a resubmit after the user resolved a policy violation (P5.4)
 * @param violationRemarks the resolution note carried alongside a forced resubmit (P5.4)
 */
fun LogMilesUiState.toSubmitRequest(
    force: Boolean = false,
    violationRemarks: String? = null,
): LogMilesSubmitRequestV2 =
    LogMilesSubmitRequestV2(
        vehicleType = selectedVehicle?.vehicleKey,
        distance = distanceKm,
        roundTrip = isRoundTrip,
        date = journeyDateMillis,
        origin = stops.firstOrNull()?.entry?.let { CoordsV2(it.lat, it.lng, it.name) },
        destination = stops.lastOrNull()?.entry?.let { CoordsV2(it.lat, it.lng, it.name) },
        employees = taggedEmployees,
        notes = logMilesNote.ifBlank { null },
        serviceId = selectedService?.id,
        invoiceDate = invoiceDateMillis,
        odometerDistance = odometerEnd?.reading?.let { end -> odometerStart?.reading?.let { start -> end - start } },
        force = force.takeIf { it },
        violationRemarks = violationRemarks?.ifBlank { null },
    )

/**
 * Whether [response] needs the severity-branched [com.mileway.feature.logging.ui.dialog
 * .ViolationDialog] (P5.4) rather than a clean pass-through to the success route. Extracted as a
 * pure function so the three-tier branch is unit-testable without standing up the ViewModel.
 */
fun ExpenseSubmissionResponse.needsViolationDialog(): Boolean =
    submissionStatus == SubmissionStatus.POLICY_VIOLATION ||
        submissionStatus == SubmissionStatus.REIMBURSABLE_ADJUSTED ||
        submissionStatus == SubmissionStatus.HARD_STOP ||
        violations.isNotEmpty() ||
        !policyViolations.isNullOrEmpty()

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

    /** Wave 3: pre-fills the form's stops from a cached [LogMilesFrequentRoute] (one-tap retrace). */
    data class RetraceRoute(val routeKey: String) : LogMilesAction

    data class OverrideDistance(val km: Double) : LogMilesAction

    /** Reflects the persisted `DemoSettingsRepository` flag into state (P5.3); read by the screen. */
    data class SetOdometerCaptureEnabled(val enabled: Boolean) : LogMilesAction

    /** Records a start or end odometer reading captured via [com.mileway.feature.logging.ui.sheets
     * .OdometerCaptureSheet] (P5.3). */
    data class CaptureOdometerReading(val result: OdometerCaptureResult) : LogMilesAction

    data class SetInvoiceDate(val millis: Long?) : LogMilesAction

    data class SetLogMilesNote(val text: String) : LogMilesAction

    data class SetTaggedEmployees(val names: List<String>) : LogMilesAction

    data object AddAttachment : LogMilesAction

    data object SaveDraft : LogMilesAction

    data class DeleteDraft(val draftId: String) : LogMilesAction

    /** Rehydrates [LogMilesUiState] from a previously saved draft (P5.1). */
    data class LoadDraft(val draftId: String) : LogMilesAction

    data object Submit : LogMilesAction

    data object DismissViolationDialog : LogMilesAction

    /**
     * Resubmits after the user resolved a policy violation from [com.mileway.feature.logging.ui
     * .dialog.ViolationDialog] (P5.4). [notes] is blank for a `REIMBURSABLE_ADJUSTED` accept and
     * required (validated by the dialog) for a `POLICY_VIOLATION` resubmit.
     */
    data class ResubmitInPolicy(val notes: String) : LogMilesAction

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
    private val frequentRouteRepo: LogMilesFrequentRouteRepository,
) : BaseViewModel<LogMilesUiState, LogMilesEffect, LogMilesAction>(LogMilesUiState()) {
    /** Backwards-compatible alias; screens read [state]. */
    val uiState = state

    /** Monotonic id source for stops added during this session. */
    private var stopIdCounter = 0L

    /** The draft id currently loaded into [LogMilesUiState], if the form was opened from a draft. */
    private var activeDraftId: String? = null

    /** [LogMilesDraftEntity.createdAt] per draft id, so re-saving preserves the original creation time. */
    private val draftCreatedAt = mutableMapOf<String, Long>()

    init {
        loadVehicles()
        loadServices()
        observeDrafts()
        observeFrequentRoutes()
    }

    /** Keeps [LogMilesUiState.frequentRoutes] in sync with the Room-backed cache (Wave 3). */
    private fun observeFrequentRoutes() {
        viewModelScope.launch {
            frequentRouteRepo.topRoutes().collectLatest { routes ->
                setState { copy(frequentRoutes = routes) }
            }
        }
    }

    /** Keeps [LogMilesUiState.drafts] in sync with the Room-backed [draftRepo] (P5.1). */
    private fun observeDrafts() {
        viewModelScope.launch {
            draftRepo.allDrafts().collectLatest { entities ->
                entities.forEach { draftCreatedAt[it.draftId] = it.createdAt }
                val drafts =
                    entities.map { e ->
                        LogMilesDraftUi(
                            id = e.draftId,
                            title =
                                LogMilesDraftRepository.decodeStops(e.locationsJson)
                                    .firstOrNull()?.entry?.name?.substringBefore(",")
                                    ?: "Log Miles draft",
                            stopCount = LogMilesDraftRepository.decodeStops(e.locationsJson).size,
                            distanceKm = e.totalDistance,
                            vehicleName = e.selectedVehicleDisplayName,
                            updatedAtMillis = e.updatedAt,
                        )
                    }
                setState { copy(drafts = drafts) }
            }
        }
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
            is LogMilesAction.RetraceRoute -> retraceRoute(action.routeKey)
            is LogMilesAction.OverrideDistance -> {
                setState { copy(distanceKm = action.km, isDistanceOverridden = true) }
                recomputePricing()
            }
            is LogMilesAction.SetOdometerCaptureEnabled ->
                setState { copy(odometerCaptureEnabled = action.enabled) }
            is LogMilesAction.CaptureOdometerReading -> captureOdometerReading(action.result)
            is LogMilesAction.SetInvoiceDate -> setState { copy(invoiceDateMillis = action.millis) }
            is LogMilesAction.SetLogMilesNote -> setState { copy(logMilesNote = action.text) }
            is LogMilesAction.SetTaggedEmployees -> setState { copy(taggedEmployees = action.names) }
            LogMilesAction.AddAttachment -> setState { copy(attachmentCount = attachmentCount + 1) }
            LogMilesAction.SaveDraft -> saveDraft()
            is LogMilesAction.DeleteDraft -> deleteDraft(action.draftId)
            is LogMilesAction.LoadDraft -> loadDraft(action.draftId)
            LogMilesAction.Submit -> submit()
            LogMilesAction.DismissViolationDialog -> setState { copy(showViolationDialog = false) }
            is LogMilesAction.ResubmitInPolicy -> submit(force = true, violationRemarks = action.notes)
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
        setState { copy(stops = stops + stop) }
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
            copy(stops = list)
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
     * Wave 3 one-tap retrace: replaces the current itinerary with a cached [LogMilesFrequentRoute]'s
     * stops (re-keyed with fresh [stopIdCounter] ids, since the cached copy carries none of its own).
     * No-op if the route isn't in the currently loaded cache.
     */
    private fun retraceRoute(routeKey: String) {
        val route = currentState.frequentRoutes.firstOrNull { it.routeKey == routeKey } ?: return
        setState { copy(stops = route.stops.map { LocationStop(id = stopIdCounter++, entry = it.entry) }) }
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

    /** Stores a captured odometer reading (P5.3) into the slot matching its [OdometerPurpose]. */
    private fun captureOdometerReading(result: OdometerCaptureResult) {
        setState {
            when (result.purpose) {
                OdometerPurpose.START -> copy(odometerStart = result)
                OdometerPurpose.END -> copy(odometerEnd = result)
            }
        }
    }

    /**
     * P5.1: persists the current form state via [draftRepo] instead of just appending an
     * in-memory summary. Re-saves onto [activeDraftId] when the form was opened from an existing
     * draft (so "save draft" from a reopened draft updates it, not create a duplicate row).
     */
    private fun saveDraft() {
        val s = currentState
        if (s.stops.isEmpty()) return
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val draftId = activeDraftId ?: "draft-$now"
        activeDraftId = draftId
        val createdAt = draftCreatedAt.getOrPut(draftId) { now }
        viewModelScope.launch {
            runCatching { draftRepo.save(s.toDraftEntity(draftId = draftId, createdAtMillis = createdAt, nowMillis = now)) }
        }
    }

    private fun deleteDraft(draftId: String) {
        if (activeDraftId == draftId) activeDraftId = null
        draftCreatedAt.remove(draftId)
        viewModelScope.launch { runCatching { draftRepo.delete(draftId) } }
    }

    /**
     * P5.1: rehydrates [LogMilesUiState] from a saved draft — locations JSON back into
     * [LocationStop]s, distance recomputed via [recomputeDistance]/[totalRouteKm] rather than
     * trusting the stored value blindly, vehicle re-resolved from the currently loaded
     * [LogMilesUiState.vehicles] by key (the draft persists only the key/display name, not the
     * live [com.mileway.core.data.model.network.ApprovedVehicle] with current pricing).
     */
    private fun loadDraft(draftId: String) {
        viewModelScope.launch {
            val loaded = runCatching { draftRepo.loadDraft(draftId) }.getOrNull() ?: return@launch
            val draft = loaded.uiState
            activeDraftId = draftId
            // Reseed the stop id counter so subsequently-added stops don't collide with restored ids.
            stopIdCounter = (draft.stops.maxOfOrNull { it.id } ?: -1L) + 1
            setState {
                copy(
                    stops = draft.stops,
                    isRoundTrip = draft.isRoundTrip,
                    journeyDateMillis = draft.journeyDateMillis,
                    invoiceDateMillis = draft.invoiceDateMillis,
                    logMilesNote = draft.logMilesNote,
                    taggedEmployees = draft.taggedEmployees,
                    selectedVehicle = vehicles.firstOrNull { it.vehicleKey == loaded.vehicleKey } ?: selectedVehicle,
                )
            }
            recomputeDistance()
        }
    }

    /**
     * Submit the journey via [LogMilesSubmitUseCase]. On success with policy violations we
     * surface the severity-branched [com.mileway.feature.logging.ui.dialog.ViolationDialog]
     * (P5.4); failures emit a [LogMilesEffect.ShowError].
     *
     * @param force            resubmit after resolving a violation (P5.4); see [LogMilesAction
     *                         .ResubmitInPolicy]
     * @param violationRemarks resolution note carried alongside a forced resubmit
     */
    private fun submit(
        force: Boolean = false,
        violationRemarks: String? = null,
    ) {
        val s = currentState
        if (s.selectedVehicle == null) return
        if (s.stops.size < 2) return
        setState { copy(isSubmitting = true) }
        viewModelScope.launch {
            submitUseCase(
                s.toSubmitRequest(force = force, violationRemarks = violationRemarks),
            ).onSuccess { resp ->
                setState {
                    copy(isSubmitting = false, submissionResult = resp, showViolationDialog = resp.needsViolationDialog())
                }
                frequentRouteRepo.recordSubmission(
                    stops = s.stops,
                    distanceKm = s.distanceKm,
                    nowMillis = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                )
            }.onFailure { e ->
                setState { copy(isSubmitting = false) }
                emitEffect(LogMilesEffect.ShowError(UiText.Static(e.message ?: "Submission failed")))
            }
        }
    }

    private fun resetSubmission() {
        stopIdCounter = 0L
        activeDraftId = null
        setState {
            LogMilesUiState(
                vehicles = vehicles,
                isLoadingVehicles = false,
                services = services,
                selectedService = services.firstOrNull(),
                isLoadingServices = false,
                drafts = drafts,
                submitted = submitted,
                // The capture flag mirrors a persisted setting, not per-submission form state.
                odometerCaptureEnabled = odometerCaptureEnabled,
            )
        }
    }
}
