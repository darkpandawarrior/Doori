package com.mileway.feature.logging.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.network.LogMilesService
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.logging_add_receipt
import com.mileway.core.ui.resources.logging_additional_details_subtitle
import com.mileway.core.ui.resources.logging_additional_details_title
import com.mileway.core.ui.resources.logging_all_set
import com.mileway.core.ui.resources.logging_attachments_added
import com.mileway.core.ui.resources.logging_attachments_header
import com.mileway.core.ui.resources.logging_attachments_hint
import com.mileway.core.ui.resources.logging_back
import com.mileway.core.ui.resources.logging_back_cd
import com.mileway.core.ui.resources.logging_collapse_cd
import com.mileway.core.ui.resources.logging_complete_required_fields
import com.mileway.core.ui.resources.logging_cost_center_optional
import com.mileway.core.ui.resources.logging_expand_cd
import com.mileway.core.ui.resources.logging_expense_details_header
import com.mileway.core.ui.resources.logging_invoice_date
import com.mileway.core.ui.resources.logging_invoice_date_picker_title
import com.mileway.core.ui.resources.logging_invoice_date_required_error
import com.mileway.core.ui.resources.logging_invoice_date_required_label
import com.mileway.core.ui.resources.logging_log_miles_note_label
import com.mileway.core.ui.resources.logging_log_miles_subtitle
import com.mileway.core.ui.resources.logging_log_miles_title
import com.mileway.core.ui.resources.logging_purpose_of_travel
import com.mileway.core.ui.resources.logging_ready_to_submit
import com.mileway.core.ui.resources.logging_remaining
import com.mileway.core.ui.resources.logging_select_a_service
import com.mileway.core.ui.resources.logging_service_type
import com.mileway.core.ui.resources.logging_step2_title
import com.mileway.core.ui.resources.logging_submit
import com.mileway.core.ui.resources.logging_submit_review_hint
import com.mileway.core.ui.resources.logging_tab_additional_details
import com.mileway.core.ui.resources.logging_tab_attachments
import com.mileway.core.ui.resources.logging_tab_expense_details
import com.mileway.core.ui.resources.logging_tab_stops
import com.mileway.core.ui.resources.logging_tagged_count
import com.mileway.core.ui.resources.logging_tagged_employees
import com.mileway.core.ui.resources.logging_tagged_employees_subtitle
import com.mileway.core.ui.resources.logging_tap_to_add_employees
import com.mileway.core.ui.resources.logging_use_camera_or_gallery
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.DesignTokens.NavigationDepth
import com.mileway.feature.logging.ui.components.TravelledLocationsActions
import com.mileway.feature.logging.ui.components.TravelledLocationsCard
import com.mileway.feature.logging.ui.dialog.TaggedEmployeesDialog
import com.mileway.feature.logging.ui.dialog.ViolationDialog
import com.mileway.feature.logging.ui.model.SubmittedVoucherSamples
import com.mileway.feature.logging.viewmodel.LogMilesAction
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.mileway.feature.tracking.ui.components.SubmissionTabChips
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Step 2 of the Log Miles flow: expense details and submission.
 *
 * Shows a checklist progress header with the remaining-field count and
 * Locations/Vehicle/Odometer/Forms chips, a compact recap of the itinerary, the
 * "Additional Details" form (required invoice date + a note), Tagged Employees,
 * an Attachments tile, and a pinned Back/Submit footer with an info banner.
 *
 * Full-screen flow (no bubble bottom bar), so the pinned footer uses
 * [navigationBarsPadding].
 *
 * @param viewModel    shared flow ViewModel
 * @param onBack       pop back to Step 1
 * @param onSubmitted  navigate to the success route after a successful submit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMilesStep2Screen(
    viewModel: LogMilesViewModel = koinViewModel(),
    onBack: () -> Unit = {},
    onSubmitted: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    var showInvoiceDatePicker by remember { mutableStateOf(false) }
    var showEmployeesDialog by remember { mutableStateOf(false) }
    var additionalExpanded by remember { mutableStateOf(true) }
    val tabStops = stringResource(Res.string.logging_tab_stops)
    val tabExpenseDetails = stringResource(Res.string.logging_tab_expense_details)
    val tabAdditionalDetails = stringResource(Res.string.logging_tab_additional_details)
    val tabAttachments = stringResource(Res.string.logging_tab_attachments)
    var selectedStep2Tab by remember { mutableStateOf(tabStops) }
    var purposeText by remember { mutableStateOf("") }
    var costCenter by remember { mutableStateOf("") }

    // When a non-violation success result lands, advance to the success route once.
    val hasCleanResult = uiState.submissionResult != null && !uiState.showViolationDialog
    LaunchedEffect(hasCleanResult) {
        if (hasCleanResult) onSubmitted()
    }

    val remaining = uiState.step2Remaining
    val locationsDone = uiState.stops.size >= 2
    val vehicleDone = uiState.selectedVehicle != null
    val formsDone = remaining == 0
    val progress = listOf(locationsDone, vehicleDone, true, formsDone).count { it } / 4f

    Scaffold(
        topBar = {
            DepthAwareTopBar(
                title = stringResource(Res.string.logging_log_miles_title),
                subtitle = stringResource(Res.string.logging_log_miles_subtitle),
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.logging_back_cd))
                    }
                },
            )
        },
        bottomBar = {
            Step2Footer(
                isSubmitting = uiState.isSubmitting,
                onBack = onBack,
                onSubmit = { viewModel.onAction(LogMilesAction.Submit) },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(top = DesignTokens.Spacing.l, bottom = DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
        ) {
            // Checklist header.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(Res.string.logging_step2_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (remaining == 0) stringResource(Res.string.logging_all_set) else stringResource(Res.string.logging_remaining, remaining),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Surface(
                        shape = DesignTokens.Shape.chip,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            if (remaining == 0) {
                                stringResource(
                                    Res.string.logging_ready_to_submit,
                                )
                            } else {
                                stringResource(Res.string.logging_complete_required_fields)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier.padding(
                                    horizontal = DesignTokens.Spacing.m,
                                    vertical = DesignTokens.Spacing.s,
                                ),
                        )
                    }
                    Spacer(Modifier.size(DesignTokens.Spacing.m))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Tab navigation chips.
            SubmissionTabChips(
                tabs = listOf(tabStops, tabExpenseDetails, tabAdditionalDetails, tabAttachments),
                selected = selectedStep2Tab,
                onSelect = { selectedStep2Tab = it },
            )

            // ── Stops tab ────────────────────────────────────────────────────────
            if (selectedStep2Tab == tabStops) {
                TravelledLocationsCard(
                    stops = uiState.stops,
                    totalDistanceKm = uiState.distanceKm,
                    pricePerKm = uiState.pricePerKm,
                    amount = uiState.reimbursableAmount,
                    isRoundTrip = uiState.isRoundTrip,
                    compact = true,
                    actions =
                        TravelledLocationsActions(
                            onEdit = {},
                            onRemove = { viewModel.onAction(LogMilesAction.RemoveStop(it)) },
                            onMoveUp = { viewModel.onAction(LogMilesAction.MoveStopUp(it)) },
                            onMoveDown = { viewModel.onAction(LogMilesAction.MoveStopDown(it)) },
                            onInsertAfter = {},
                            onToggleRoundTrip = { viewModel.onAction(LogMilesAction.SetRoundTrip(it)) },
                            onAddLocation = {},
                            onUseCurrent = {},
                            onVerifyDistance = {},
                        ),
                )
            }

            // ── Expense Details tab ───────────────────────────────────────────────
            if (selectedStep2Tab == tabExpenseDetails) {
                ExpenseDetailsSection(
                    services = uiState.services,
                    selectedService = uiState.selectedService,
                    onServiceSelect = { viewModel.onAction(LogMilesAction.SelectService(it)) },
                    purposeText = purposeText,
                    onPurposeChange = { purposeText = it },
                    costCenter = costCenter,
                    onCostCenterChange = { costCenter = it },
                )
            }

            // ── Additional Details tab ────────────────────────────────────────────
            if (selectedStep2Tab == tabAdditionalDetails) {
                AdditionalDetailsCard(
                    expanded = additionalExpanded,
                    onToggle = { additionalExpanded = !additionalExpanded },
                    invoiceDateText = uiState.invoiceDateMillis?.let { DateUtils.epochToDisplayDate(it) },
                    onPickInvoiceDate = { showInvoiceDatePicker = true },
                    note = uiState.logMilesNote,
                    onNoteChange = { viewModel.onAction(LogMilesAction.SetLogMilesNote(it)) },
                )
                TaggedEmployeesCard(
                    taggedCount = uiState.taggedEmployees.size,
                    onTap = { showEmployeesDialog = true },
                )
            }

            // ── Attachments tab ───────────────────────────────────────────────────
            if (selectedStep2Tab == tabAttachments) {
                AttachmentsCard(
                    attachmentCount = uiState.attachmentCount,
                    onAdd = { viewModel.onAction(LogMilesAction.AddAttachment) },
                )
            }
        }
    }

    // ── Overlays ───────────────────────────────────────────────────────────────

    if (showInvoiceDatePicker) {
        WheelDatePickerDialog(
            initialDateMillis = uiState.invoiceDateMillis,
            title = stringResource(Res.string.logging_invoice_date_picker_title),
            onConfirm = {
                viewModel.onAction(LogMilesAction.SetInvoiceDate(it))
                showInvoiceDatePicker = false
            },
            onDismiss = { showInvoiceDatePicker = false },
        )
    }

    if (showEmployeesDialog) {
        TaggedEmployeesDialog(
            allEmployees = SubmittedVoucherSamples.taggableEmployees,
            initiallySelected = uiState.taggedEmployees,
            onConfirm = {
                viewModel.onAction(LogMilesAction.SetTaggedEmployees(it))
                showEmployeesDialog = false
            },
            onDismiss = { showEmployeesDialog = false },
        )
    }

    if (uiState.showViolationDialog) {
        uiState.submissionResult?.let { result ->
            ViolationDialog(
                response = result,
                onResubmit = { remarks ->
                    viewModel.onAction(LogMilesAction.DismissViolationDialog)
                    viewModel.onAction(LogMilesAction.ResubmitInPolicy(remarks))
                },
                onDismiss = {
                    viewModel.onAction(LogMilesAction.DismissViolationDialog)
                },
            )
        }
    }
}

@Composable
private fun ExpenseDetailsSection(
    services: List<LogMilesService>,
    selectedService: LogMilesService?,
    onServiceSelect: (LogMilesService) -> Unit,
    purposeText: String,
    onPurposeChange: (String) -> Unit,
    costCenter: String,
    onCostCenterChange: (String) -> Unit,
) {
    var serviceExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Text(
                stringResource(Res.string.logging_expense_details_header),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Service picker row.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = { serviceExpanded = true },
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.m),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.logging_service_type),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            selectedService?.getDisplayString() ?: stringResource(Res.string.logging_select_a_service),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color =
                                if (selectedService == null) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (serviceExpanded) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column {
                        services.forEach { svc ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color =
                                    if (svc.id == selectedService?.id) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                onClick = {
                                    onServiceSelect(svc)
                                    serviceExpanded = false
                                },
                            ) {
                                Text(
                                    svc.getDisplayString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(DesignTokens.Spacing.m),
                                )
                            }
                        }
                    }
                }
            }

            // Purpose text field.
            OutlinedTextField(
                value = purposeText,
                onValueChange = onPurposeChange,
                label = { Text(stringResource(Res.string.logging_purpose_of_travel)) },
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                singleLine = true,
            )

            // Cost center row.
            OutlinedTextField(
                value = costCenter,
                onValueChange = onCostCenterChange,
                label = { Text(stringResource(Res.string.logging_cost_center_optional)) },
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                singleLine = true,
            )
        }
    }
}

@Composable
private fun AdditionalDetailsCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    invoiceDateText: String?,
    onPickInvoiceDate: () -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = DesignTokens.Shape.button,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    ) {
                        Icon(
                            Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    Spacer(Modifier.size(DesignTokens.Spacing.m))
                    Column {
                        Text(
                            stringResource(Res.string.logging_additional_details_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(Res.string.logging_additional_details_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) stringResource(Res.string.logging_collapse_cd) else stringResource(Res.string.logging_expand_cd),
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.size(DesignTokens.Spacing.l))

                Text(
                    stringResource(Res.string.logging_invoice_date_required_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = DesignTokens.Shape.roundedMd,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    onClick = onPickInvoiceDate,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.l),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DesignTokens.IconSize.navigation),
                            )
                            Spacer(Modifier.size(DesignTokens.Spacing.m))
                            Text(
                                invoiceDateText ?: stringResource(Res.string.logging_invoice_date),
                                style = MaterialTheme.typography.bodyLarge,
                                color =
                                    if (invoiceDateText == null) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (invoiceDateText == null) {
                    Spacer(Modifier.size(DesignTokens.Spacing.xs))
                    Text(
                        stringResource(Res.string.logging_invoice_date_required_error),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.size(DesignTokens.Spacing.l))

                Text(
                    stringResource(Res.string.logging_log_miles_note_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.logging_log_miles_note_label)) },
                    minLines = 2,
                    shape = DesignTokens.Shape.roundedMd,
                )
            }
        }
    }
}

@Composable
private fun TaggedEmployeesCard(
    taggedCount: Int,
    onTap: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = DesignTokens.Shape.button,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                Column {
                    Text(
                        stringResource(Res.string.logging_tagged_employees),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(Res.string.logging_tagged_employees_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.size(DesignTokens.Spacing.m))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                onClick = onTap,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.l),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.navigation),
                    )
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text(
                        if (taggedCount == 0) {
                            stringResource(
                                Res.string.logging_tap_to_add_employees,
                            )
                        } else {
                            stringResource(Res.string.logging_tagged_count, taggedCount)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentsCard(
    attachmentCount: Int,
    onAdd: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = DesignTokens.Shape.button,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                Text(
                    stringResource(Res.string.logging_attachments_header),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                stringResource(Res.string.logging_attachments_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.m))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                onClick = onAdd,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = DesignTokens.Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.header),
                    )
                    Spacer(Modifier.size(DesignTokens.Spacing.xs))
                    Text(
                        if (attachmentCount == 0) {
                            stringResource(
                                Res.string.logging_add_receipt,
                            )
                        } else {
                            stringResource(Res.string.logging_attachments_added, attachmentCount)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(Res.string.logging_use_camera_or_gallery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Step2Footer(
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = DesignTokens.Shape.button,
            ) { Text(stringResource(Res.string.logging_back)) }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                    )
                    Spacer(Modifier.size(DesignTokens.Spacing.s))
                    Text(
                        stringResource(Res.string.logging_submit_review_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                shape = DesignTokens.Shape.button,
            ) {
                if (isSubmitting) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                } else {
                    Text(stringResource(Res.string.logging_submit))
                }
            }
        }
    }
}
