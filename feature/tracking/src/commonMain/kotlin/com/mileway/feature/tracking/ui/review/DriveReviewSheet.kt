@file:Suppress("ktlint:standard:max-line-length")

package com.mileway.feature.tracking.ui.review

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.DistanceLedgerBar
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.tracking.ui.components.AttachmentsSection
import com.mileway.feature.tracking.ui.components.OdometerReadingsCard
import com.mileway.feature.tracking.ui.components.TogglePill
import com.mileway.feature.tracking.ui.components.VehicleSummaryCard
import com.mileway.feature.tracking.ui.navigation.SubmissionResult
import com.mileway.feature.tracking.ui.screens.TrackLoadingScreen
import com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen
import com.mileway.feature.tracking.ui.sheets.PolicyViolationSheet
import com.mileway.feature.tracking.viewmodel.MileageSubmissionAction
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi
import com.mileway.feature.tracking.viewmodel.SubmissionSheet
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Wave-2 happy path: Start, Stop, **Confirm** — replacing both `TrackSubmissionScreen` and
 * `TrackingSuccessScreen` with one bottom sheet over the still-live map. See [DriveReviewPhase]
 * for the five states this renders; [derivePhase] is the single place they're computed.
 *
 * Reuses [MileageSubmissionViewModel] as-is (same submission pipeline, same
 * [SubmissionFormUi][com.mileway.feature.tracking.viewmodel.SubmissionFormUi]) — this file adds a
 * sheet UI over it, never a second submission path. SUBMITTING renders the existing
 * [TrackLoadingScreen] content; SUCCESS renders the existing [TrackingSuccessScreen] verbatim (it
 * was already a stateless, embeddable composable).
 *
 * Dismissing (drag-down / scrim tap / the close button) never touches the ViewModel — it only
 * flips this composable's own visibility, then shows [ReopenReviewPill] so the same review can be
 * reopened. The drive itself is already durably saved by the tracking pipeline before this sheet
 * ever appears; nothing here can lose it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveReviewSheet(
    routeId: String,
    distanceKm: Double,
    vehicleKey: String,
    startTime: Long,
    endTime: Long,
    onTrackNewJourney: () -> Unit,
    onViewExpense: (SubmissionResult) -> Unit,
    onCreateVoucher: () -> Unit,
    onAddExpense: () -> Unit = {},
    onNavigateToOdometerStart: () -> Unit = {},
    onNavigateToOdometerEnd: () -> Unit = {},
    onAddAttachment: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MileageSubmissionViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()
    val form = ui.form
    var local by remember { mutableStateOf(DriveReviewLocalState()) }

    // Same load call TrackSubmissionScreen made — populates start/end address, vehicle name/rate
    // and the auto-detected round-trip flag. Idempotent: the ViewModel no-ops once already loaded.
    LaunchedEffect(routeId) {
        viewModel.onAction(MileageSubmissionAction.LoadTrackInfo(routeId, vehicleKey, distanceKm))
    }

    if (!local.visible) {
        ReopenReviewPill(modifier = modifier, onClick = { local = local.reopen() })
        return
    }

    val phase =
        derivePhase(ui.submissionState, local) { response ->
            response.toSubmissionResult(distanceKm, vehicleKey, form.vehicleName, startTime, endTime)
        }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { local = local.dismiss() },
        sheetState = sheetState,
        modifier = modifier,
    ) {
        DriveReviewSheetContent(
            phase = phase,
            form = form,
            distanceKm = distanceKm,
            local = local,
            pendingReceipts = ui.pendingReceipts,
            onLocalChange = { local = it },
            onAddAttachment = onAddAttachment,
            onRemoveAttachment = { viewModel.onAction(MileageSubmissionAction.RemoveReceipt(it)) },
            onNavigateToOdometerStart = onNavigateToOdometerStart,
            onNavigateToOdometerEnd = onNavigateToOdometerEnd,
            onSetPurpose = { viewModel.onAction(MileageSubmissionAction.SetFormValue("purpose", it)) },
            onSetClassification = { viewModel.onAction(MileageSubmissionAction.SetFormValue("classification", it.name)) },
            onDismiss = { local = local.dismiss() },
            onConfirm = { viewModel.onAction(buildSubmitAction(routeId, distanceKm, vehicleKey, startTime, endTime)) },
            onRetry = { viewModel.onAction(buildSubmitAction(routeId, distanceKm, vehicleKey, startTime, endTime)) },
            onEditDetails = { viewModel.onAction(MileageSubmissionAction.Reset) },
            onTrackNewJourney = onTrackNewJourney,
            onViewExpense = onViewExpense,
            onCreateVoucher = onCreateVoucher,
            onAddExpense = onAddExpense,
        )
    }

    // A policy violation raised mid-submit must be resolvable without leaving this flow, even
    // though it isn't one of DriveReviewSheet's five named phases — it's a nested sheet on top of
    // whichever phase is currently showing (REVIEW, once handleSubmitResponse drops submissionState
    // back to Idle pending resolution).
    if (form.sheet == SubmissionSheet.POLICY_VIOLATION) {
        PolicyViolationSheet(
            violations = form.violations,
            askAuthoritiesSelected = form.askAuthorities,
            note = form.violationNote,
            onToggleAskAuthorities = { viewModel.onAction(MileageSubmissionAction.SetAskAuthorities(!form.askAuthorities)) },
            onNoteChange = { viewModel.onAction(MileageSubmissionAction.SetViolationNote(it)) },
            onSubmit = { viewModel.onAction(MileageSubmissionAction.ResolvePolicyAndFinalize) },
            onDismiss = { viewModel.onAction(MileageSubmissionAction.DismissSheet) },
        )
    }
}

/** Same [MileageSubmissionAction.Submit] shape for both the REVIEW confirm tap and a FAILED retry. */
private fun buildSubmitAction(
    routeId: String,
    distanceKm: Double,
    vehicleKey: String,
    startTime: Long,
    endTime: Long,
) = MileageSubmissionAction.Submit(routeId, distanceKm, vehicleKey, startTime, endTime)

/**
 * Phase → content mapping for [DriveReviewSheet], pulled out as its own stateless composable so it
 * is callable without [MileageSubmissionViewModel] at all — every parameter here is plain
 * data/callbacks. [DriveReviewSheet] (the real, VM-backed sheet) and [DriveReviewSheetPreview]
 * (capture/preview) both feed it; there is exactly one place phase renders to UI, not two drifting
 * copies. Callbacks default to no-ops so a caller that only wants to *look* at one phase — a
 * screenshot — doesn't have to wire five callbacks it will never fire.
 */
@Composable
fun DriveReviewSheetContent(
    phase: DriveReviewPhase,
    form: SubmissionFormUi,
    distanceKm: Double,
    local: DriveReviewLocalState,
    pendingReceipts: List<String> = emptyList(),
    onLocalChange: (DriveReviewLocalState) -> Unit = {},
    onAddAttachment: () -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    onNavigateToOdometerStart: () -> Unit = {},
    onNavigateToOdometerEnd: () -> Unit = {},
    onSetPurpose: (String) -> Unit = {},
    onSetClassification: (TripClassification) -> Unit = {},
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onTrackNewJourney: () -> Unit = {},
    onViewExpense: (SubmissionResult) -> Unit = {},
    onCreateVoucher: () -> Unit = {},
    onAddExpense: () -> Unit = {},
) {
    when (phase) {
        DriveReviewPhase.Review, is DriveReviewPhase.Editing ->
            ReviewContent(
                form = form,
                distanceKm = distanceKm,
                local = local,
                onLocalChange = onLocalChange,
                pendingReceipts = pendingReceipts,
                onAddAttachment = onAddAttachment,
                onRemoveAttachment = onRemoveAttachment,
                onNavigateToOdometerStart = onNavigateToOdometerStart,
                onNavigateToOdometerEnd = onNavigateToOdometerEnd,
                onSetPurpose = onSetPurpose,
                onSetClassification = onSetClassification,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
            )

        DriveReviewPhase.Submitting ->
            Box(Modifier.fillMaxWidth().height(360.dp)) { TrackLoadingScreen() }

        is DriveReviewPhase.Success ->
            Box(Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
                TrackingSuccessScreen(
                    distanceKm = phase.result.distanceKm,
                    reimbursableAmount = phase.result.reimbursableAmount,
                    vehicleName = phase.result.vehicleName,
                    startTime = phase.result.startTime,
                    endTime = phase.result.endTime,
                    transactionId = phase.result.transactionId,
                    submissionStatus = phase.result.submissionStatus,
                    violationCount = phase.result.violationCount,
                    violationMessage = phase.result.violationMessage,
                    voucherNumber = phase.result.voucherNumber,
                    voucherAmount = phase.result.voucherAmount,
                    onTrackNewJourney = onTrackNewJourney,
                    onViewExpense = { onViewExpense(phase.result) },
                    onCreateVoucher = onCreateVoucher,
                    onAddExpense = onAddExpense,
                )
            }

        is DriveReviewPhase.Failed ->
            FailedContent(
                phase = phase,
                onRetry = onRetry,
                onEditDetails = onEditDetails,
            )
    }
}

/**
 * Stateless, ViewModel-free entry point for capturing [DriveReviewSheet] in a single phase — the
 * gallery's hook for REVIEW / EDITING / SUBMITTING / SUCCESS / FAILED without wiring
 * [MileageSubmissionViewModel]. Wraps [DriveReviewSheetContent] in a real [ModalBottomSheet] so a
 * capture matches what a user actually sees — the same standalone-`ModalBottomSheet`-in-a-
 * screenshot-test pattern already proven by [WhatsNewSheet][com.mileway.ui.home.WhatsNewSheet].
 *
 * For EDITING, pass a [local] whose `editingField` matches `phase`'s field — [ReviewContent] reads
 * the expanded row from `local.editingField`, not from `phase`, so the two must agree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveReviewSheetPreview(
    phase: DriveReviewPhase,
    form: SubmissionFormUi = DriveReviewPreviewData.sampleForm(),
    distanceKm: Double = DriveReviewPreviewData.DISTANCE_KM,
    local: DriveReviewLocalState = DriveReviewLocalState(),
    pendingReceipts: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        DriveReviewSheetContent(
            phase = phase,
            form = form,
            distanceKm = distanceKm,
            local = local,
            pendingReceipts = pendingReceipts,
        )
    }
}

/** Small bottom-anchored pill shown once the sheet has been dismissed — the drive is still saved. */
@Composable
private fun ReopenReviewPill(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().padding(DesignTokens.Spacing.l)) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Filled.Route, contentDescription = null) },
            text = { Text("Review journey") },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun ReviewContent(
    form: SubmissionFormUi,
    distanceKm: Double,
    local: DriveReviewLocalState,
    onLocalChange: (DriveReviewLocalState) -> Unit,
    pendingReceipts: List<String>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onNavigateToOdometerStart: () -> Unit,
    onNavigateToOdometerEnd: () -> Unit,
    onSetPurpose: (String) -> Unit,
    onSetClassification: (TripClassification) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val ledger = remember(form, distanceKm) { form.toDistanceLedger(distanceKm) }
    val purpose = form.values["purpose"].orEmpty()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.l)
                .padding(bottom = DesignTokens.Spacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Review journey",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        Text(
            text = "Distance",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.s))
        DistanceLedgerBar(ledger = ledger)

        Spacer(Modifier.height(DesignTokens.Spacing.l))
        HorizontalDivider()

        ExpandableReviewRow(
            field = ReviewField.CLASSIFICATION,
            label = "Trip type",
            valueText = local.classification.name.lowercase().replaceFirstChar { it.uppercase() },
            local = local,
            onLocalChange = onLocalChange,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                TogglePill(
                    label = "Business",
                    selected = local.classification == TripClassification.BUSINESS,
                    onClick = { onSetClassification(TripClassification.BUSINESS) },
                )
                TogglePill(
                    label = "Personal",
                    selected = local.classification == TripClassification.PERSONAL,
                    onClick = { onSetClassification(TripClassification.PERSONAL) },
                )
            }
        }
        HorizontalDivider()

        VehicleSummaryCard(vehicleName = form.vehicleName, ratePerKm = form.vehicleRatePerKm)

        ExpandableReviewRow(
            field = ReviewField.PURPOSE,
            label = "Purpose",
            valueText = purpose.ifBlank { "Add purpose" },
            local = local,
            onLocalChange = onLocalChange,
        ) {
            OutlinedTextField(
                value = purpose,
                onValueChange = onSetPurpose,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What was this journey for?") },
                shape = DesignTokens.Shape.roundedSm,
                singleLine = true,
            )
        }
        HorizontalDivider()

        ExpandableReviewRow(
            field = ReviewField.ATTACHMENTS,
            label = "Attachments",
            valueText = if (pendingReceipts.isEmpty()) "None yet" else "${pendingReceipts.size} attached",
            local = local,
            onLocalChange = onLocalChange,
        ) {
            AttachmentsSection(attachments = pendingReceipts, onAdd = onAddAttachment, onRemove = onRemoveAttachment)
        }
        HorizontalDivider()

        ExpandableReviewRow(
            field = ReviewField.ODOMETER,
            label = "Odometer",
            valueText = odometerSummary(form),
            local = local,
            onLocalChange = onLocalChange,
        ) {
            OdometerReadingsCard(
                startReading = form.simulatedStartOdo,
                endReading = form.simulatedEndOdo,
                isManualStart = form.isManualStartOdo,
                isManualEnd = form.isManualEndOdo,
                odometerDistanceKm = form.odometerDistanceKm(),
                onCaptureStart = onNavigateToOdometerStart,
                onCaptureEnd = onNavigateToOdometerEnd,
                startImageUri = form.odometerStartImageUri,
                endImageUri = form.odometerEndImageUri,
            )
        }

        Spacer(Modifier.height(DesignTokens.Spacing.l))

        // One tap from REVIEW when nothing needs correcting — disabled while a row is expanded
        // (finish that correction first) or a requirement is still outstanding.
        Button(
            onClick = onConfirm,
            enabled = form.canSubmit && local.editingField == null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = DesignTokens.Shape.roundedMd,
        ) {
            Text("Confirm", fontWeight = FontWeight.Bold)
        }
        if (form.remainingRequirements.isNotEmpty()) {
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            Text(
                text = form.remainingRequirements.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun odometerSummary(form: SubmissionFormUi): String {
    val start = form.simulatedStartOdo
    val end = form.simulatedEndOdo
    return if (start != null && end != null) "$start → $end" else "Not captured"
}

/**
 * One REVIEW row that expands in place into its editor when tapped — the EDITING phase, rendered
 * inline rather than as a separate screen. Tapping an already-expanded row collapses it back to
 * REVIEW. Uses [DesignTokens.Motion] for the expand/collapse, never an invented duration.
 */
@Composable
private fun ExpandableReviewRow(
    field: ReviewField,
    label: String,
    valueText: String,
    local: DriveReviewLocalState,
    onLocalChange: (DriveReviewLocalState) -> Unit,
    content: @Composable () -> Unit,
) {
    val expanded = local.editingField == field
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(DesignTokens.Motion.STANDARD)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onLocalChange(if (expanded) local.finishEditing() else local.edit(field)) }
                    .padding(vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Box(Modifier.padding(bottom = DesignTokens.Spacing.m)) { content() }
        }
    }
}

/**
 * FAILED: always actionable. A queued (offline) submission reads as "saved, will sync" with a
 * "Sync now" action, never as an error; a real error offers Retry plus a way back to REVIEW.
 */
@Composable
private fun FailedContent(
    phase: DriveReviewPhase.Failed,
    onRetry: () -> Unit,
    onEditDetails: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.l)
                .padding(bottom = DesignTokens.Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (phase.queued) Icons.Filled.CloudOff else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (phase.queued) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(DesignTokens.Spacing.m))
        Text(
            text = if (phase.queued) "Saved for later" else "Submission failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.s))
        Text(
            text = phase.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.l))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = DesignTokens.Shape.roundedMd,
        ) {
            Text(if (phase.queued) "Sync now" else "Retry", fontWeight = FontWeight.SemiBold)
        }
        if (!phase.queued) {
            Spacer(Modifier.height(DesignTokens.Spacing.s))
            OutlinedButton(
                onClick = onEditDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
            ) {
                Text("Edit details")
            }
        }
    }
}
