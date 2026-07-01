package com.mileway.feature.tracking.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.display.OdometerCaptureResult
import com.mileway.core.data.model.network.ExpenseSubmissionResponse
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.SubmissionStatus
import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.mileway.core.network.api.MilewayNetworkApi
import com.mileway.core.network.model.BusinessEntity
import com.mileway.core.network.model.Office
import com.mileway.core.platform.NotificationScheduler
import com.mileway.core.ui.mvi.BaseViewModel
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.repository.OfflinePlacesRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import kotlinx.coroutines.launch
import kotlin.time.Clock

sealed class SubmissionUiState {
    object Idle : SubmissionUiState()

    object Submitting : SubmissionUiState()

    data class Success(val response: ExpenseSubmissionResponse) : SubmissionUiState()

    data class Error(val message: String) : SubmissionUiState()
}

/** A required custom-form field rendered in the "Additional Details" section. */
enum class SubmissionFieldType { TEXT, DROPDOWN }

data class SubmissionField(
    val id: String,
    val label: String,
    val type: SubmissionFieldType,
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
) {
    val remainingRequirements: List<String>
        get() =
            buildList {
                if (officeRequired && selectedOffice == null) add("Office selection")
                if (officeRequired && selectedEntity == null) add("Entity selection")
                val missingFields = fields.count { it.required && values[it.id].isNullOrBlank() }
                if (missingFields > 0) add("Complete required fields")
            }

    val canSubmit: Boolean get() = remainingRequirements.isEmpty()
}

data class MileageSubmissionUiState(
    val submissionState: SubmissionUiState = SubmissionUiState.Idle,
    val form: SubmissionFormUi = SubmissionFormUi(),
    val lastResponse: ExpenseSubmissionResponse? = null,
    val pendingReceipts: List<String> = emptyList(),
    val pendingOdoStart: Pair<String, String?>? = null,
    val pendingOdoEnd: Pair<String, String?>? = null,
)

sealed interface MileageSubmissionAction {
    data class AddReceipt(val uri: String) : MileageSubmissionAction

    data class RemoveReceipt(val uri: String) : MileageSubmissionAction

    data class SetOdometerStart(val uri: String, val ocrText: String?) : MileageSubmissionAction

    data class SetOdometerEnd(val uri: String, val ocrText: String?) : MileageSubmissionAction

    data class SetFormValue(val id: String, val value: String) : MileageSubmissionAction

    data class ToggleDraft(val enabled: Boolean) : MileageSubmissionAction

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
) : BaseViewModel<MileageSubmissionUiState, MileageSubmissionEffect, MileageSubmissionAction>(
        MileageSubmissionUiState(
            form =
                SubmissionFormUi(
                    offices = emptyList(),
                    entities = emptyList(),
                    officeRequired = false,
                    fields =
                        listOf(
                            SubmissionField("purpose", "Purpose of travel", SubmissionFieldType.TEXT),
                            SubmissionField(
                                "gender",
                                "Gender",
                                SubmissionFieldType.DROPDOWN,
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
                setState { copy(pendingOdoStart = action.uri to action.ocrText) }
            is MileageSubmissionAction.SetOdometerEnd ->
                setState { copy(pendingOdoEnd = action.uri to action.ocrText) }
            is MileageSubmissionAction.SetFormValue ->
                setState { copy(form = form.copy(values = form.values + (action.id to action.value))) }
            is MileageSubmissionAction.ToggleDraft ->
                setState { copy(form = form.copy(saveAsDraft = action.enabled)) }
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
            setState {
                copy(
                    form =
                        form.copy(
                            startAddress = startAddr,
                            endAddress = endAddr,
                            vehicleName = vehicle?.vehicleName ?: vehicleKey,
                            vehicleRatePerKm = vehicle?.vehiclePricing ?: 0.0,
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
        viewModelScope.launch {
            persistPendingAttachments(routeId)
            runCatching {
                api.submitMiles(
                    SubmitMilesRequestK(
                        token = routeId,
                        vehicleType = vehicleKey,
                        distance = distanceKm,
                        startTime = startTime,
                        endTime = endTime,
                        submissionTime = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            }.onSuccess { response ->
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
            }.onFailure { e ->
                setState { copy(submissionState = SubmissionUiState.Error(e.message ?: "Submission failed")) }
            }
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
            notificationScheduler.notify(
                id = routeId.hashCode(),
                title = "Trip submitted",
                body = "₹${(response.reimbursableAmount ?: 0.0).toLong()} pending reimbursement",
            )
            setState { copy(submissionState = SubmissionUiState.Success(response)) }
        }
    }

    private suspend fun persistPendingAttachments(routeId: String) {
        currentState.pendingReceipts.forEach { uri ->
            attachmentRepository.addReceipt(routeId, uri)
        }
        currentState.pendingOdoStart?.let { (uri, ocr) ->
            attachmentRepository.setOdometerStart(routeId, uri, ocr)
        }
        currentState.pendingOdoEnd?.let { (uri, ocr) ->
            attachmentRepository.setOdometerEnd(routeId, uri, ocr)
        }
    }
}
