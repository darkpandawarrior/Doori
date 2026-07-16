package com.mileway.feature.tracking.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.display.OdometerReadingSource
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.forms.FieldId
import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormSchema
import com.mileway.core.forms.validationErrors
import com.mileway.core.network.NetworkMonitor
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.model.BusinessEntity
import com.mileway.core.network.model.Office
import com.mileway.feature.tracking.checkin.RoundTripClassifier
import com.mileway.feature.tracking.insights.DistanceQualityAnalyzer
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.OfflinePlacesRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import com.mileway.feature.tracking.service.MilesSubmitSyncer
import com.mileway.feature.tracking.service.SubmissionNotificationMapper
import com.mileway.feature.tracking.service.SubmissionNotificationThrottler
import com.mileway.feature.tracking.service.SubmitOutcome
import com.mileway.feature.tracking.submission.SubmitMilesRequestBuilder
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.ReviewTracker
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed class SubmissionUiState {
    object Idle : SubmissionUiState()

    object Submitting : SubmissionUiState()

    // PLAN_V33 A5: submitted while offline — durably queued, will drain automatically once the
    // connectivity offline->online edge fires (or the next time this trip is submitted while online).
    object Queued : SubmissionUiState()

    data class Success(val response: ExpenseSubmissionResponse) : SubmissionUiState()

    data class Error(val message: String) : SubmissionUiState()
}

/**
 * A custom-form field rendered in the "Additional Details" section — [type] is [core:forms
 * FormFieldType][FormFieldType] directly (only [FormFieldType.TEXT]/[FormFieldType.SELECT] are
 * used today) so [SubmissionFormUi.formSchema]/[SubmissionFormUi.formValues] can map straight into
 * `core:forms`' [MockFormSchema]/[FormFieldValue] with no parallel enum to keep in sync (V27
 * P27.F.6 — the screen renders this section through the shared `FormRenderer`, one validation path).
 */
data class SubmissionField(
    val id: String,
    val label: String,
    val type: FormFieldType,
    val required: Boolean = true,
    val options: List<String> = emptyList(),
)

/** Which submission sheet (if any) is presented. */
enum class SubmissionSheet { NONE, SUBMIT_CONFIRM, POLICY_VIOLATION, OFFICE_PICKER, ENTITY_PICKER, SMART_DISTANCE }

/** All form + policy + sheet state for the submission screen (single source of truth). */
@Stable
data class SubmissionFormUi(
    val offices: List<Office> = emptyList(),
    val entities: List<BusinessEntity> = emptyList(),
    val officeRequired: Boolean = false,
    val selectedOffice: Office? = null,
    val selectedEntity: BusinessEntity? = null,
    val fields: List<SubmissionField> = emptyList(),
    val values: Map<String, String> = emptyMap(),
    val saveAsDraft: Boolean = false,
    val sheet: SubmissionSheet = SubmissionSheet.NONE,
    val officeQuery: String = "",
    val entityQuery: String = "",
    val violations: List<PolicyViolation> = emptyList(),
    val askAuthorities: Boolean = false,
    val violationNote: String = "",
    val smartDistanceTrackedKm: Double = 0.0,
    val smartDistanceOdometerKm: Double = 0.0,
    val startAddress: String = "",
    val endAddress: String = "",
    val vehicleName: String = "",
    val vehicleRatePerKm: Double = 0.0,
    val simulatedStartOdo: Int? = null,
    val simulatedEndOdo: Int? = null,
    val isManualStartOdo: Boolean = false,
    val isManualEndOdo: Boolean = false,
    val odometerStartImageUri: String? = null,
    val odometerEndImageUri: String? = null,
    val odometerStartCaptureMs: Long? = null,
    val odometerEndCaptureMs: Long? = null,
    val config: TrackMilesPluginConfig = TrackMilesPluginConfig(),
    // P6.1: user-declared "odometer isn't working" fallback. Only meaningful (and only bypasses
    // the odometer requirement) when config.calculateExpenseViaOdometer is true.
    val odometerNotWorking: Boolean = false,
    // P6.2: auto-detected via RoundTripClassifier once start/end coords are loaded from
    // SavedTrackRepository in loadTrackInfo(); threaded into SubmitMilesRequestK on submit.
    val roundTrip: Boolean = false,
) {
    private val odometerCaptured: Boolean get() = simulatedStartOdo != null && simulatedEndOdo != null

    private val odometerFallbackActive: Boolean get() = config.calculateExpenseViaOdometer && odometerNotWorking

    /** [fields] mapped to `core:forms`' schema shape — the single input [FormRenderer] renders. */
    val formSchema: List<MockFormSchema>
        get() = fields.map { f -> MockFormSchema(id = f.id, fieldKey = f.id, label = f.label, type = f.type, required = f.required, options = f.options) }

    /** [values] mapped to `core:forms`' value shape, keyed the same way as [formSchema]. */
    val formValues: Map<FieldId, FormFieldValue>
        get() =
            fields.associate { f ->
                f.id to if (f.type == FormFieldType.SELECT) FormFieldValue.Select(values[f.id]) else FormFieldValue.Text(values[f.id].orEmpty())
            }

    val remainingRequirements: List<String>
        get() =
            buildList {
                if (officeRequired && selectedOffice == null) add("Office selection")
                if (officeRequired && selectedEntity == null) add("Entity selection")
                // V27 P27.F.6: the one validationErrors() source — no more separately re-deriving
                // "is this required field blank" in TrackSubmissionScreen (the two-sources bug).
                if (validationErrors(formSchema, formValues).isNotEmpty()) add("Complete required fields")
                if (config.isOdometerMandatory && !odometerCaptured && !odometerFallbackActive) add("Odometer reading")
            }

    val canSubmit: Boolean get() = remainingRequirements.isEmpty()
}

/**
 * Wave-4 §2.4b: a not-yet-persisted odometer capture, carrying the raw OCR text (back-compat) plus
 * the real typed [reading]/[source] when the capture path provided one — threaded through to
 * [TripAttachmentRepository.setOdometerStart]/[TripAttachmentRepository.setOdometerEnd] instead of
 * being re-derived from [ocrText] alone.
 */
data class PendingOdometerCapture(
    val uri: String,
    val ocrText: String?,
    val reading: Int? = null,
    val source: OdometerReadingSource? = null,
)

data class MileageSubmissionUiState(
    val submissionState: SubmissionUiState = SubmissionUiState.Idle,
    val form: SubmissionFormUi = SubmissionFormUi(),
    val lastResponse: ExpenseSubmissionResponse? = null,
    val pendingReceipts: List<String> = emptyList(),
    val pendingOdoStart: PendingOdometerCapture? = null,
    val pendingOdoEnd: PendingOdometerCapture? = null,
)

sealed interface MileageSubmissionAction {
    data class AddReceipt(val uri: String) : MileageSubmissionAction

    data class RemoveReceipt(val uri: String) : MileageSubmissionAction

    data class SetOdometerStart(val uri: String, val ocrText: String?) : MileageSubmissionAction

    data class SetOdometerEnd(val uri: String, val ocrText: String?) : MileageSubmissionAction

    data class SetFormValue(val id: String, val value: String) : MileageSubmissionAction

    data class ToggleDraft(val enabled: Boolean) : MileageSubmissionAction

    // P6.1: user declares the odometer is unreadable/broken — when config allows it, submission
    // proceeds without an odometer capture and sources distance from GPS instead.
    data class SetOdometerNotWorking(val enabled: Boolean) : MileageSubmissionAction

    data object OpenOfficePicker : MileageSubmissionAction

    data object OpenEntityPicker : MileageSubmissionAction

    data class SetOfficeQuery(val q: String) : MileageSubmissionAction

    data class SetEntityQuery(val q: String) : MileageSubmissionAction

    data class SelectOffice(val code: String) : MileageSubmissionAction

    data class SelectEntity(val name: String) : MileageSubmissionAction

    data object OpenSubmitConfirm : MileageSubmissionAction

    data class OpenSmartDistanceSheet(val trackedKm: Double, val odometerKm: Double) : MileageSubmissionAction

    data object DismissSheet : MileageSubmissionAction

    data class SetAskAuthorities(val enabled: Boolean) : MileageSubmissionAction

    data class SetViolationNote(val note: String) : MileageSubmissionAction

    data class LoadTrackInfo(val routeId: String, val vehicleKey: String, val distanceKm: Double) : MileageSubmissionAction

    data class SimulateCaptureStartOdo(val distanceKm: Double) : MileageSubmissionAction

    data class SimulateCaptureEndOdo(val distanceKm: Double) : MileageSubmissionAction

    data class CaptureOdometerStart(val result: OdometerCaptureResult) : MileageSubmissionAction

    data class CaptureOdometerEnd(val result: OdometerCaptureResult) : MileageSubmissionAction

    data class Submit(
        val routeId: String,
        val distanceKm: Double,
        val vehicleKey: String,
        val startTime: Long,
        val endTime: Long,
    ) : MileageSubmissionAction

    data object ResolvePolicyAndFinalize : MileageSubmissionAction

    data object Reset : MileageSubmissionAction
}

sealed interface MileageSubmissionEffect

class MileageSubmissionViewModel(
    private val api: MilewayNetworkApi,
    private val trackRepository: SavedTrackRepository,
    private val attachmentRepository: TripAttachmentRepository,
    private val configManager: TrackingConfigManager,
    private val notificationScheduler: NotificationScheduler,
    private val notificationThrottler: SubmissionNotificationThrottler =
        SubmissionNotificationThrottler(now = { Clock.System.now().toEpochMilliseconds() }),
    // PLAN_V24 P12.3: a submitted mileage claim is a meaningful engagement signal for the review gate.
    // Nullable-defaulted so direct-construction tests need no change; Koin supplies the real single.
    private val reviewTracker: ReviewTracker? = null,
    // PLAN_V33 A5: durable submit outbox — nullable-defaulted for the same reason as reviewTracker
    // above (direct-construction tests keep working unchanged); when absent, submit() falls back to
    // calling the network directly (the pre-A5 behavior) instead of silently dropping the request.
    private val milesSyncer: MilesSubmitSyncer? = null,
    // PLAN_V33 C5: source for the trip's GPS trail (used to fill SubmitMilesRequestK.origin/
    // destination) — nullable-defaulted for the same reason as the params above (direct-
    // construction tests keep working unchanged; Koin supplies the real singleton).
    private val locationRepository: LocationRepository? = null,
) : BaseViewModel<MileageSubmissionUiState, MileageSubmissionEffect, MileageSubmissionAction>(
        MileageSubmissionUiState(
            form =
                SubmissionFormUi(
                    offices = emptyList(),
                    entities = emptyList(),
                    officeRequired = false,
                    fields =
                        listOf(
                            SubmissionField("purpose", "Purpose of travel", FormFieldType.TEXT),
                            SubmissionField(
                                "gender",
                                "Gender",
                                FormFieldType.SELECT,
                                options = listOf("Male", "Female", "Others"),
                            ),
                        ),
                ),
        ),
    ) {
    private var pendingFinalize: PendingFinalize? = null

    private data class PendingFinalize(val routeId: String, val response: ExpenseSubmissionResponse)

    init {
        setState {
            copy(
                form =
                    form.copy(
                        offices = configManager.getOffices(),
                        entities = configManager.getBusinessEntities(),
                        officeRequired = configManager.isOfficeSelectionRequired(),
                        config = configManager.getTrackMilesConfig(),
                    ),
            )
        }
    }

    override fun onAction(action: MileageSubmissionAction) {
        when (action) {
            is MileageSubmissionAction.AddReceipt ->
                setState { copy(pendingReceipts = pendingReceipts + action.uri) }
            is MileageSubmissionAction.RemoveReceipt ->
                setState { copy(pendingReceipts = pendingReceipts - action.uri) }
            is MileageSubmissionAction.SetOdometerStart ->
                setState { copy(pendingOdoStart = PendingOdometerCapture(action.uri, action.ocrText)) }
            is MileageSubmissionAction.SetOdometerEnd ->
                setState { copy(pendingOdoEnd = PendingOdometerCapture(action.uri, action.ocrText)) }
            is MileageSubmissionAction.SetFormValue ->
                setState { copy(form = form.copy(values = form.values + (action.id to action.value))) }
            is MileageSubmissionAction.ToggleDraft ->
                setState { copy(form = form.copy(saveAsDraft = action.enabled)) }
            is MileageSubmissionAction.SetOdometerNotWorking ->
                setState { copy(form = form.copy(odometerNotWorking = action.enabled)) }
            MileageSubmissionAction.OpenOfficePicker ->
                setState { copy(form = form.copy(sheet = SubmissionSheet.OFFICE_PICKER)) }
            MileageSubmissionAction.OpenEntityPicker ->
                setState { copy(form = form.copy(sheet = SubmissionSheet.ENTITY_PICKER)) }
            is MileageSubmissionAction.SetOfficeQuery ->
                setState { copy(form = form.copy(officeQuery = action.q)) }
            is MileageSubmissionAction.SetEntityQuery ->
                setState { copy(form = form.copy(entityQuery = action.q)) }
            is MileageSubmissionAction.SelectOffice ->
                setState {
                    copy(form = form.copy(selectedOffice = form.offices.firstOrNull { it.code == action.code }, sheet = SubmissionSheet.NONE))
                }
            is MileageSubmissionAction.SelectEntity ->
                setState {
                    copy(form = form.copy(selectedEntity = form.entities.firstOrNull { it.name == action.name }, sheet = SubmissionSheet.NONE))
                }
            MileageSubmissionAction.OpenSubmitConfirm ->
                setState { copy(form = form.copy(sheet = SubmissionSheet.SUBMIT_CONFIRM)) }
            is MileageSubmissionAction.OpenSmartDistanceSheet ->
                setState {
                    copy(
                        form =
                            form.copy(
                                sheet = SubmissionSheet.SMART_DISTANCE,
                                smartDistanceTrackedKm = action.trackedKm,
                                smartDistanceOdometerKm = action.odometerKm,
                            ),
                    )
                }
            MileageSubmissionAction.DismissSheet ->
                setState { copy(form = form.copy(sheet = SubmissionSheet.NONE)) }
            is MileageSubmissionAction.SetAskAuthorities ->
                setState { copy(form = form.copy(askAuthorities = action.enabled)) }
            is MileageSubmissionAction.SetViolationNote ->
                setState { copy(form = form.copy(violationNote = action.note)) }
            is MileageSubmissionAction.LoadTrackInfo -> loadTrackInfo(action.routeId, action.vehicleKey, action.distanceKm)
            is MileageSubmissionAction.SimulateCaptureStartOdo ->
                setState { copy(form = form.copy(simulatedStartOdo = 45_000, isManualStartOdo = false)) }
            is MileageSubmissionAction.SimulateCaptureEndOdo -> {
                val start = currentState.form.simulatedStartOdo ?: 45_000
                setState {
                    copy(form = form.copy(simulatedEndOdo = start + action.distanceKm.toInt().coerceAtLeast(1), isManualEndOdo = false))
                }
            }
            is MileageSubmissionAction.CaptureOdometerStart ->
                setState {
                    copy(
                        form =
                            form.copy(
                                simulatedStartOdo = action.result.reading,
                                isManualStartOdo = action.result.isManual,
                                odometerStartImageUri = action.result.imageUri,
                                odometerStartCaptureMs = action.result.captureTimeMs,
                            ),
                        // Wave-4 §2.4b: carry the real typed reading/source through to persistence,
                        // instead of leaving pendingOdoStart unset and losing this capture entirely.
                        pendingOdoStart =
                            PendingOdometerCapture(
                                uri = action.result.imageUri,
                                ocrText = action.result.reading.toString(),
                                reading = action.result.reading,
                                source = action.result.source,
                            ),
                    )
                }
            is MileageSubmissionAction.CaptureOdometerEnd -> {
                val startReading = currentState.form.simulatedStartOdo ?: 45_000
                val distKm = (action.result.reading - startReading).coerceAtLeast(0).toDouble()
                setState {
                    copy(
                        form =
                            form.copy(
                                simulatedEndOdo = action.result.reading,
                                isManualEndOdo = action.result.isManual,
                                odometerEndImageUri = action.result.imageUri,
                                odometerEndCaptureMs = action.result.captureTimeMs,
                                smartDistanceOdometerKm = distKm,
                            ),
                        pendingOdoEnd =
                            PendingOdometerCapture(
                                uri = action.result.imageUri,
                                ocrText = action.result.reading.toString(),
                                reading = action.result.reading,
                                source = action.result.source,
                            ),
                    )
                }
            }
            is MileageSubmissionAction.Submit -> submit(action.routeId, action.distanceKm, action.vehicleKey, action.startTime, action.endTime)
            MileageSubmissionAction.ResolvePolicyAndFinalize -> resolvePolicyAndFinalize()
            MileageSubmissionAction.Reset ->
                setState {
                    copy(
                        submissionState = SubmissionUiState.Idle,
                        pendingReceipts = emptyList(),
                        pendingOdoStart = null,
                        pendingOdoEnd = null,
                    )
                }
        }
    }

    val policyResolved: Boolean
        get() = currentState.form.askAuthorities && currentState.form.violationNote.isNotBlank()

    private fun loadTrackInfo(
        routeId: String,
        vehicleKey: String,
        distanceKm: Double,
    ) {
        if (currentState.form.startAddress.isNotEmpty()) return
        viewModelScope.launch {
            val track = trackRepository.getByRouteId(routeId)
            val startAddr = if (track != null) OfflinePlacesRepository.addressFor(track.startLatitude, track.startLongitude) else "Start Location"
            val endAddr = if (track != null) OfflinePlacesRepository.addressFor(track.endLatitude, track.endLongitude) else "End Location"
            val vehicles = runCatching { api.vehicles(trackMiles = true).vehicles }.getOrElse { emptyList() }
            val vehicle = vehicles.firstOrNull { it.vehicleKey == vehicleKey }
            // P6.2: auto-detect a round trip from the saved track's start/end coordinates and the
            // already-tracked distance, so SubmitMilesRequestK.roundTrip stops being always-false.
            val roundTrip =
                track != null &&
                    RoundTripClassifier.isRoundTrip(
                        startLat = track.startLatitude,
                        startLng = track.startLongitude,
                        endLat = track.endLatitude,
                        endLng = track.endLongitude,
                        totalDistanceKm = distanceKm,
                    )
            setState {
                copy(
                    form =
                        form.copy(
                            startAddress = startAddr,
                            endAddress = endAddr,
                            vehicleName = vehicle?.vehicleName ?: vehicleKey,
                            vehicleRatePerKm = vehicle?.vehiclePricing ?: 0.0,
                            roundTrip = roundTrip,
                        ),
                )
            }
        }
    }

    private fun submit(
        routeId: String,
        distanceKm: Double,
        vehicleKey: String,
        startTime: Long,
        endTime: Long,
    ) {
        setState { copy(form = form.copy(sheet = SubmissionSheet.NONE), submissionState = SubmissionUiState.Submitting) }
        val odometerFallbackActive = currentState.form.config.calculateExpenseViaOdometer && currentState.form.odometerNotWorking
        viewModelScope.launch {
            persistPendingAttachments(routeId)
            if (odometerFallbackActive) {
                trackRepository.markOdometerNotWorking(routeId)
            }
            // PLAN_V33 gap-fix (flagged by C5): persist the office/entity picked on this screen so
            // SubmitMilesRequestBuilder's track?.officeId/entityId reads stop always seeing null.
            // Office.code is a numeric string (see PolicyMockData); BusinessEntity carries its own
            // id (added alongside this fix — it had none before).
            val selectedOffice = currentState.form.selectedOffice
            val selectedEntity = currentState.form.selectedEntity
            if (selectedOffice != null || selectedEntity != null) {
                trackRepository.setOfficeAndEntity(
                    routeId = routeId,
                    officeId = selectedOffice?.code?.toLongOrNull(),
                    entityId = selectedEntity?.id,
                )
            }
            // PLAN_V33 C5: trip/office/entity/tripV2 id resolution + the GPS first/last point come
            // from the persisted trip row and its location trail — both optional lookups (a fresh
            // draft may have neither yet), the builder tolerates either being absent/empty.
            val track = trackRepository.getByRouteId(routeId)
            val routePoints = locationRepository?.getForToken(routeId) ?: emptyList()
            val request =
                SubmitMilesRequestBuilder.build(
                    routeId = routeId,
                    vehicleKey = vehicleKey,
                    // P6.1: distance always sourced from the already-computed GPS distanceKm
                    // param here; when the odometer fallback is active this is the rate
                    // source (rather than an odometer-reading delta), tagged for audit in the
                    // builder (violationRemarks/milesAmountByOdometer/odometer readings).
                    distanceKm = distanceKm,
                    startTime = startTime,
                    endTime = endTime,
                    submissionTime = Clock.System.now().toEpochMilliseconds(),
                    form = currentState.form,
                    track = track,
                    routePoints = routePoints,
                )

            val syncer = milesSyncer
            if (syncer == null) {
                // No durable outbox wired (e.g. a direct-construction test) — pre-A5 behavior.
                runCatching { api.submitMiles(request) }
                    .onSuccess { response -> handleSubmitResponse(routeId, response) }
                    .onFailure { e -> setState { copy(submissionState = SubmissionUiState.Error(e.message ?: "Submission failed")) } }
                return@launch
            }

            // PLAN_V33 A5: durable submit — enqueue before the network call so the request survives
            // process death between here and the response, mirroring A4's location outbox.
            syncer.enqueue(TripDraft(routeId, request))
            if (NetworkMonitor.isConnected()) {
                when (val outcome = syncer.drain(routeId)) {
                    is SubmitOutcome.Success -> handleSubmitResponse(routeId, outcome.response)
                    SubmitOutcome.RetryableFailure, SubmitOutcome.PermanentFailure, SubmitOutcome.NotQueued ->
                        setState { copy(submissionState = SubmissionUiState.Error("Submission failed")) }
                }
            } else {
                // Queued durably — SyncStatusViewModel's connectivity-edge trigger (or the next
                // online submit for this route) drains it; see MilesSubmitSyncer's reconciliation doc.
                setState { copy(submissionState = SubmissionUiState.Queued) }
            }
        }
    }

    private fun handleSubmitResponse(
        routeId: String,
        response: ExpenseSubmissionResponse,
    ) {
        setState { copy(lastResponse = response) }
        val needsResolution =
            response.submissionStatus == SubmissionStatus.POLICY_VIOLATION ||
                response.submissionStatus == SubmissionStatus.HARD_STOP
        if (needsResolution && response.violations.isNotEmpty()) {
            pendingFinalize = PendingFinalize(routeId, response)
            setState {
                copy(
                    form = form.copy(violations = response.violations, sheet = SubmissionSheet.POLICY_VIOLATION),
                    submissionState = SubmissionUiState.Idle,
                )
            }
        } else {
            finalize(routeId, response)
        }
    }

    private fun resolvePolicyAndFinalize() {
        val pending = pendingFinalize ?: return
        setState { copy(form = form.copy(sheet = SubmissionSheet.NONE)) }
        finalize(pending.routeId, pending.response)
        pendingFinalize = null
    }

    private fun finalize(
        routeId: String,
        response: ExpenseSubmissionResponse,
    ) {
        viewModelScope.launch {
            val transId = response.transId ?: "DEMO-${Clock.System.now().toEpochMilliseconds()}"
            trackRepository.markSubmitted(routeId, transId, response.reimbursableAmount ?: 0.0)
            reviewTracker?.recordInteraction()

            // Wave-3 (parity §3): quality score reused from DistanceQualityAnalyzer against the
            // persisted track's distance buckets — point-level mock/abnormal counts aren't loaded
            // here, so problemPointPct falls back to 0; the distance-pct terms still dominate.
            val track = trackRepository.getByRouteId(routeId)
            val score =
                track?.let {
                    DistanceQualityAnalyzer.computeScore(
                        mockDistance = it.mockDistance,
                        abnormalDistance = it.abnormalDistance,
                        totalDistance = it.originalDistance.takeIf { d -> d > 0 } ?: it.distance,
                        mockCount = 0,
                        abnormalCount = 0,
                        totalCount = 1,
                    )
                } ?: 100

            val summaryId = routeId.hashCode()
            if (notificationThrottler.allow(summaryId)) {
                val summary =
                    SubmissionNotificationMapper.completionSummary(
                        distanceKm = response.distance,
                        reimbursableAmount = response.reimbursableAmount ?: 0.0,
                        score = score,
                    )
                notificationScheduler.notify(id = summaryId, title = summary.title, body = summary.body)
            }

            SubmissionNotificationMapper.violationContent(response.violations)?.let { (content, fixIssue) ->
                val violationId = (routeId + fixIssue.violationId).hashCode()
                if (notificationThrottler.allow(violationId)) {
                    notificationScheduler.notify(id = violationId, title = content.title, body = content.body)
                }
            }

            setState { copy(submissionState = SubmissionUiState.Success(response)) }
        }
    }

    private suspend fun persistPendingAttachments(routeId: String) {
        currentState.pendingReceipts.forEach { uri ->
            attachmentRepository.addReceipt(routeId, uri)
        }
        currentState.pendingOdoStart?.let { capture ->
            attachmentRepository.setOdometerStart(
                trackToken = routeId,
                uri = capture.uri,
                ocrText = capture.ocrText,
                typedReading = capture.reading,
                typedSource = capture.source,
            )
        }
        currentState.pendingOdoEnd?.let { capture ->
            attachmentRepository.setOdometerEnd(
                trackToken = routeId,
                uri = capture.uri,
                ocrText = capture.ocrText,
                typedReading = capture.reading,
                typedSource = capture.source,
            )
        }
    }
}
