package com.mileway.feature.tracking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_back
import com.mileway.core.ui.resources.tracking_submission_entity
import com.mileway.core.ui.resources.tracking_submission_field_required
import com.mileway.core.ui.resources.tracking_submission_info
import com.mileway.core.ui.resources.tracking_submission_loading_address
import com.mileway.core.ui.resources.tracking_submission_office
import com.mileway.core.ui.resources.tracking_submission_required_field
import com.mileway.core.ui.resources.tracking_submission_select_entity
import com.mileway.core.ui.resources.tracking_submission_select_office
import com.mileway.core.ui.resources.tracking_submission_subtitle
import com.mileway.core.ui.resources.tracking_submission_title
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.tracking.ui.components.AdditionalDetailsForm
import com.mileway.feature.tracking.ui.components.AttachmentsSection
import com.mileway.feature.tracking.ui.components.FormField
import com.mileway.feature.tracking.ui.components.FormFieldType
import com.mileway.feature.tracking.ui.components.JourneySummaryCard
import com.mileway.feature.tracking.ui.components.LocationDetailsCard
import com.mileway.feature.tracking.ui.components.MileageDraftBottomBar
import com.mileway.feature.tracking.ui.components.OdometerReadingsCard
import com.mileway.feature.tracking.ui.components.OfficeEntitySelectRow
import com.mileway.feature.tracking.ui.components.PendingDataSyncCard
import com.mileway.feature.tracking.ui.components.StaticPolylineThumbnail
import com.mileway.feature.tracking.ui.components.SubmissionChecklistHeader
import com.mileway.feature.tracking.ui.components.SubmissionTabChips
import com.mileway.feature.tracking.ui.components.VehicleSummaryCard
import com.mileway.feature.tracking.ui.navigation.SubmissionResult
import com.mileway.feature.tracking.ui.sheets.EntityPickerSheet
import com.mileway.feature.tracking.ui.sheets.OfficePickerSheet
import com.mileway.feature.tracking.ui.sheets.PolicyViolationSheet
import com.mileway.feature.tracking.ui.sheets.SmartDistanceSheet
import com.mileway.feature.tracking.ui.sheets.SubmitConfirmSheet
import com.mileway.feature.tracking.viewmodel.MileageSubmissionAction
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.SubmissionFieldType
import com.mileway.feature.tracking.viewmodel.SubmissionSheet
import com.mileway.feature.tracking.viewmodel.SubmissionUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SUBMISSION_TABS = listOf("Journey", "Vehicle", "Odometer", "Forms", "Attachments")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSubmissionScreen(
    routeId: String,
    distanceKm: Double,
    vehicleKey: String,
    startTime: Long,
    endTime: Long,
    onSuccess: (SubmissionResult) -> Unit,
    onBack: () -> Unit,
    onNavigateToOdometerStart: () -> Unit = {},
    onNavigateToOdometerEnd: () -> Unit = {},
    viewModel: MileageSubmissionViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val state = ui.submissionState
    val form = ui.form

    var selectedTab by remember { mutableStateOf("Journey") }
    var smartDistanceVerified by remember { mutableStateOf(false) }
    var smartDistanceExplanation by remember { mutableStateOf("") }

    val durationMs = (endTime - startTime).coerceAtLeast(0L)
    val startOdo = form.simulatedStartOdo
    val endOdo = form.simulatedEndOdo
    val odometerDistanceKm =
        if (startOdo != null && endOdo != null) {
            (endOdo - startOdo).toDouble()
        } else {
            null
        }

    // Load start/end addresses and vehicle info from Room on first composition.
    LaunchedEffect(routeId) {
        viewModel.onAction(MileageSubmissionAction.LoadTrackInfo(routeId, vehicleKey, distanceKm))
    }

    // Trigger SmartDistanceSheet automatically when odometer discrepancy exceeds 15%.
    LaunchedEffect(odometerDistanceKm) {
        val odometer = odometerDistanceKm ?: return@LaunchedEffect
        val discrepancy = if (distanceKm > 0.0) kotlin.math.abs(odometer - distanceKm) / distanceKm else 0.0
        if (discrepancy > 0.15) viewModel.onAction(MileageSubmissionAction.OpenSmartDistanceSheet(distanceKm, odometer))
    }

    // On a finalized submission, hand the full result to the success screen.
    LaunchedEffect(ui) {
        val s = ui.submissionState
        if (s is SubmissionUiState.Success) {
            val r = s.response
            onSuccess(
                SubmissionResult(
                    distanceKm = distanceKm,
                    reimbursableAmount = r.reimbursableAmount ?: 0.0,
                    vehicleKey = vehicleKey,
                    vehicleName = form.vehicleName.ifBlank { vehicleKey },
                    startTime = startTime,
                    endTime = endTime,
                    transactionId = r.transId ?: r.transaction?.id,
                    submissionStatus = r.submissionStatus.name,
                    violationCount = r.violations.size,
                    violationMessage = r.violations.firstOrNull()?.message,
                    voucherNumber = r.issuedVoucher?.number,
                    voucherAmount = r.issuedVoucher?.amount ?: 0.0,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.tracking_submission_title),
                subtitle = stringResource(Res.string.tracking_submission_subtitle),
                titleIcon = Icons.AutoMirrored.Filled.Send,
                depth = NavigationDepth.LEVEL_2,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tracking_cd_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            MileageDraftBottomBar(
                saveAsDraft = form.saveAsDraft,
                onToggleDraft = { viewModel.onAction(MileageSubmissionAction.ToggleDraft(it)) },
                submitEnabled = form.canSubmit && state !is SubmissionUiState.Submitting,
                onDiscard = onBack,
                onSubmit = { viewModel.onAction(MileageSubmissionAction.OpenSubmitConfirm) },
                infoText = stringResource(Res.string.tracking_submission_info),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(DesignTokens.Spacing.l),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
            ) {
                item {
                    SubmissionChecklistHeader(
                        remaining = form.remainingRequirements.size,
                        requirements = form.remainingRequirements,
                    )
                }

                item {
                    SubmissionTabChips(
                        tabs = SUBMISSION_TABS,
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                    )
                }

                item {
                    PendingDataSyncCard(pendingPoints = 1, totalPoints = 7)
                }

                // ── Journey section ──────────────────────────────────────────────
                if (selectedTab == "Journey" || selectedTab == "Vehicle") {
                    item {
                        // Route polyline thumbnail using the mock location points.
                        val demoPoints = buildDemoRoutePoints(distanceKm)
                        StaticPolylineThumbnail(latLngs = demoPoints)
                    }
                }

                item {
                    JourneySummaryCard(
                        distanceText = "${(distanceKm * 100).toLong() / 100.0} km",
                        durationText = formatDuration(durationMs),
                        maxSpeedText = "—",
                        avgSpeedText = "—",
                    )
                }

                item {
                    LocationDetailsCard(
                        startAddress = form.startAddress.ifEmpty { stringResource(Res.string.tracking_submission_loading_address) },
                        endAddress = form.endAddress.ifEmpty { stringResource(Res.string.tracking_submission_loading_address) },
                        startTime = DateUtils.epochToTime12h(startTime),
                        endTime = DateUtils.epochToTime12h(endTime),
                    )
                }

                // ── Vehicle section ──────────────────────────────────────────────
                item {
                    VehicleSummaryCard(
                        vehicleName = form.vehicleName.ifBlank { vehicleKey },
                        ratePerKm = form.vehicleRatePerKm,
                    )
                }

                // ── Odometer section ─────────────────────────────────────────────
                item {
                    OdometerReadingsCard(
                        startReading = startOdo,
                        endReading = endOdo,
                        isManualStart = form.isManualStartOdo,
                        isManualEnd = form.isManualEndOdo,
                        odometerDistanceKm = odometerDistanceKm,
                        onCaptureStart = onNavigateToOdometerStart,
                        onCaptureEnd = onNavigateToOdometerEnd,
                        startImageUri = form.odometerStartImageUri,
                        endImageUri = form.odometerEndImageUri,
                    )
                }

                // ── Forms section ────────────────────────────────────────────────
                item {
                    AdditionalDetailsForm(
                        fields =
                            form.fields.map { f ->
                                FormField(
                                    id = f.id,
                                    label = f.label,
                                    type = if (f.type == SubmissionFieldType.DROPDOWN) FormFieldType.DROPDOWN else FormFieldType.TEXT,
                                    value = form.values[f.id].orEmpty(),
                                    required = f.required,
                                    options = f.options,
                                    errorText =
                                        if (f.required && form.values[f.id].isNullOrBlank()) {
                                            stringResource(
                                                Res.string.tracking_submission_field_required,
                                                f.label,
                                            )
                                        } else {
                                            null
                                        },
                                )
                            },
                        onValueChange = { id, value -> viewModel.onAction(MileageSubmissionAction.SetFormValue(id, value)) },
                    )
                }

                // Always show office/entity rows (demo always has these visible).
                item {
                    OfficeEntitySelectRow(
                        label = stringResource(Res.string.tracking_submission_office),
                        value = form.selectedOffice?.let { "${it.code} - ${it.name}" },
                        requiredHint =
                            if (form.officeRequired) {
                                stringResource(
                                    Res.string.tracking_submission_required_field,
                                )
                            } else {
                                stringResource(Res.string.tracking_submission_select_office)
                            },
                        onClick = { viewModel.onAction(MileageSubmissionAction.OpenOfficePicker) },
                    )
                }
                item {
                    OfficeEntitySelectRow(
                        label = stringResource(Res.string.tracking_submission_entity),
                        value = form.selectedEntity?.name,
                        requiredHint =
                            if (form.officeRequired) {
                                stringResource(
                                    Res.string.tracking_submission_required_field,
                                )
                            } else {
                                stringResource(Res.string.tracking_submission_select_entity)
                            },
                        onClick = { viewModel.onAction(MileageSubmissionAction.OpenEntityPicker) },
                    )
                }

                // ── Attachments section ──────────────────────────────────────────
                item {
                    AttachmentsSection(
                        attachments = ui.pendingReceipts,
                        onAdd = { /* navigate to attachment picker: wired at nav level */ },
                        onRemove = { viewModel.onAction(MileageSubmissionAction.RemoveReceipt(it)) },
                    )
                }
            }

            if (state is SubmissionUiState.Submitting) {
                Box(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // ── Submission sheets (single source of truth: form.sheet) ──────────────────
    when (form.sheet) {
        SubmissionSheet.SUBMIT_CONFIRM ->
            SubmitConfirmSheet(
                onConfirm = { viewModel.onAction(MileageSubmissionAction.Submit(routeId, distanceKm, vehicleKey, startTime, endTime)) },
                onCancel = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
                onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
            )

        SubmissionSheet.POLICY_VIOLATION ->
            PolicyViolationSheet(
                violations = form.violations,
                askAuthoritiesSelected = form.askAuthorities,
                note = form.violationNote,
                onToggleAskAuthorities = { viewModel.onAction(MileageSubmissionAction.SetAskAuthorities(!form.askAuthorities)) },
                onNoteChange = { viewModel.onAction(MileageSubmissionAction.SetViolationNote(it)) },
                onSubmit = { viewModel.onAction(MileageSubmissionAction.ResolvePolicyAndFinalize) },
                onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
            )

        SubmissionSheet.OFFICE_PICKER ->
            OfficePickerSheet(
                offices = form.offices,
                query = form.officeQuery,
                onQueryChange = { viewModel.onAction(MileageSubmissionAction.SetOfficeQuery(it)) },
                onSelect = { viewModel.onAction(MileageSubmissionAction.SelectOffice(it)) },
                onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
            )

        SubmissionSheet.ENTITY_PICKER ->
            EntityPickerSheet(
                entities = form.entities,
                query = form.entityQuery,
                onQueryChange = { viewModel.onAction(MileageSubmissionAction.SetEntityQuery(it)) },
                onSelect = { viewModel.onAction(MileageSubmissionAction.SelectEntity(it)) },
                onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
            )

        SubmissionSheet.SMART_DISTANCE ->
            SmartDistanceSheet(
                trackedKm = form.smartDistanceTrackedKm,
                odometerKm = form.smartDistanceOdometerKm,
                verified = smartDistanceVerified,
                explanation = smartDistanceExplanation,
                onVerifiedChange = { smartDistanceVerified = it },
                onExplanationChange = { smartDistanceExplanation = it },
                onStop = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
                onContinue = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
                onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
            )

        SubmissionSheet.NONE -> Unit
    }
}

/** Builds a simple arc of demo lat/lng points to represent the route in the thumbnail. */
private fun buildDemoRoutePoints(distanceKm: Double): List<Pair<Double, Double>> {
    val baseLat = 18.5204
    val baseLng = 73.8567
    val steps = 8
    val latDelta = distanceKm * 0.005
    return (0..steps).map { i ->
        val t = i.toDouble() / steps
        Pair(
            baseLat + t * latDelta,
            baseLng + t * latDelta * 0.7 + kotlin.math.sin(t * kotlin.math.PI) * 0.01,
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "$h hr $m min" else "$m min"
}
