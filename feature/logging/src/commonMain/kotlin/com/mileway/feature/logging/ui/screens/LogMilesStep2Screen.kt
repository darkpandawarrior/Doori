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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.components.topbar.DepthAwareTopBar
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
    var selectedStep2Tab by remember { mutableStateOf("Stops") }
    var selectedService by remember { mutableStateOf("Client Visit") }
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
                title = "Log Miles",
                subtitle = "Fill out the details given below",
                depth = NavigationDepth.LEVEL_1,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            "Step 2 of 2: Expense Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (remaining == 0) "All set" else "$remaining remaining",
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
                            if (remaining == 0) "Ready to submit" else "Complete required fields",
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
                tabs = listOf("Stops", "Expense Details", "Additional Details", "Attachments"),
                selected = selectedStep2Tab,
                onSelect = { selectedStep2Tab = it },
            )

            // ── Stops tab ────────────────────────────────────────────────────────
            if (selectedStep2Tab == "Stops") {
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
            if (selectedStep2Tab == "Expense Details") {
                ExpenseDetailsSection(
                    selectedService = selectedService,
                    onServiceSelect = { selectedService = it },
                    purposeText = purposeText,
                    onPurposeChange = { purposeText = it },
                    costCenter = costCenter,
                    onCostCenterChange = { costCenter = it },
                )
            }

            // ── Additional Details tab ────────────────────────────────────────────
            if (selectedStep2Tab == "Additional Details") {
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
            if (selectedStep2Tab == "Attachments") {
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
            title = "Invoice Date",
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
                onAcknowledge = {
                    viewModel.onAction(LogMilesAction.DismissViolationDialog)
                    onSubmitted()
                },
            )
        }
    }
}

private val SERVICE_OPTIONS =
    listOf(
        "Client Visit",
        "Office Travel",
        "Training",
        "Medical",
        "Other",
    )

@Composable
private fun ExpenseDetailsSection(
    selectedService: String,
    onServiceSelect: (String) -> Unit,
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
                "Expense Details",
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
                            "Service Type",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            selectedService,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
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
                        SERVICE_OPTIONS.forEach { svc ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color =
                                    if (svc == selectedService) {
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
                                    svc,
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
                label = { Text("Purpose of travel") },
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedMd,
                singleLine = true,
            )

            // Cost center row.
            OutlinedTextField(
                value = costCenter,
                onValueChange = onCostCenterChange,
                label = { Text("Cost Center (optional)") },
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
                        shape = RoundedCornerShape(10.dp),
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
                            "Additional Details (2)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Required expense fields before submission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onToggle) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand")
                }
            }

            if (expanded) {
                Spacer(Modifier.size(DesignTokens.Spacing.l))

                Text(
                    "Invoice date *",
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
                                invoiceDateText ?: "Invoice date",
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
                        "Invoice date is required",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.size(DesignTokens.Spacing.l))

                Text(
                    "log miles",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("log miles") },
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
                    shape = RoundedCornerShape(10.dp),
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
                        "Tagged Employees",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Add teammates linked to this journey",
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
                        if (taggedCount == 0) "Tap to add employees" else "$taggedCount tagged · tap to edit",
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
                    shape = RoundedCornerShape(10.dp),
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
                    "Attachments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                "Add bills, invoices, docs…",
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
                        if (attachmentCount == 0) "Add Receipt" else "$attachmentCount added · Add more",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Use camera or gallery",
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
                shape = DesignTokens.Shape.roundedMd,
            ) { Text("Back") }

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
                        "Review the filled details before you submit this mileage expense.",
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
                shape = DesignTokens.Shape.roundedMd,
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
                    Text("Submit")
                }
            }
        }
    }
}
