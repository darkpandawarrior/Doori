package com.miletracker.feature.tracking.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miletracker.core.ui.components.SectionCard

/**
 * The three high-level steps of the START flow surfaced by [JourneyGuideSheet].
 *
 * The flow is intentionally condensed to three checkpoints so the stepper stays
 * legible: grant permissions, prepare the vehicle (and optional start odometer),
 * then begin tracking. The richer internal sub-steps of the source flow collapse
 * onto these three for the visual indicator.
 */
enum class JourneyGuideStep {
    /** Step 1, runtime location/notification permissions are granted. */
    PERMISSIONS,

    /** Step 2, a vehicle is picked and (if required) the start odometer captured. */
    VEHICLE,

    /** Step 3, tracking is live. */
    TRACKING,
}

/**
 * Immutable, hoisted state for [JourneyGuideSheet] (unidirectional data flow).
 *
 * The sheet renders purely from this object; every mutation is requested via a
 * callback and applied by the caller, who owns the source of truth. This keeps
 * the sheet free of ViewModels, navigation and side effects so it previews and
 * tests trivially.
 *
 * @param step Which checkpoint the stepper highlights. The stepper marks earlier
 *   steps complete and the matching step active.
 * @param vehicleName Display name of the selected vehicle, or `null` if none is
 *   chosen yet (drives the "Select Vehicle" checklist row).
 * @param vehicleRatePerKm Optional per-km rate shown under the vehicle name
 *   (e.g. `10.0` renders as "₹10.0/km"). `null` hides the rate line.
 * @param startOdometer Captured start odometer reading, or `null` if not yet
 *   captured. When present it is shown in the odometer row in place of the hint.
 * @param draftEnabled Current state of the "save as draft" toggle.
 * @param requiresOdometer Whether this configuration mandates a start-odometer
 *   capture before tracking can begin. When `false` the odometer row still shows
 *   but never blocks the CTA.
 */
data class JourneyGuideState(
    val step: JourneyGuideStep = JourneyGuideStep.VEHICLE,
    val vehicleName: String? = null,
    val vehicleRatePerKm: Double? = null,
    val startOdometer: Int? = null,
    val draftEnabled: Boolean = false,
    val requiresOdometer: Boolean = true,
) {
    /** A vehicle has been chosen. */
    val vehicleSelected: Boolean get() = vehicleName != null

    /** The start odometer has been captured. */
    val odometerCaptured: Boolean get() = startOdometer != null

    /**
     * Whether the "Start Tracking" CTA is enabled: a vehicle is selected and,
     * when the configuration requires it, the start odometer has been captured.
     */
    val canStart: Boolean
        get() = vehicleSelected && (!requiresOdometer || odometerCaptured)
}

// ── Tunable visual constants (ported 1:1 from the source guide sheet) ────────────
private val SheetCorner = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
private val DragHandleWidth = 44.dp
private val DragHandleHeight = 5.dp
private val StepCircleSize = 28.dp
private val StepCheckSize = 18.dp
private val StepConnectorHeight = 2.dp
private val CtaHeight = 54.dp

/**
 * The 3-step START flow as a stateless [ModalBottomSheet].
 *
 * Layout mirrors the production journey guide:
 * - A centered "Journey Guide" header with a step subtitle.
 * - A Quick Start Checklist summarising the outstanding prerequisites.
 * - A ✓ → 2 → 3 stepper.
 * - A Vehicle Selection card (tap to pick a vehicle) and a Start Odometer row
 *   (tap the camera to capture).
 * - A Draft mode toggle.
 * - A pinned bottom bar with a step chip, the "Start Tracking" CTA and "Close".
 *
 * The composable holds no state of its own. Data flows in via [state]; all intent
 * leaves via the callbacks. Wire it from a host that owns the real state.
 *
 * @param state Hoisted UI state; see [JourneyGuideState].
 * @param onPickVehicle Invoked when the vehicle card is tapped.
 * @param onCaptureOdometer Invoked when the odometer camera affordance is tapped.
 * @param onToggleDraft Invoked with the requested new draft-toggle value.
 * @param onStartTracking Invoked when an enabled "Start Tracking" CTA is tapped.
 * @param onDismiss Invoked on swipe-down, scrim tap, or the "Close" button.
 * @param modifier Modifier applied to the sheet content column.
 * @param sheetState The [ModalBottomSheet] state; defaults to fully-expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyGuideSheet(
    state: JourneyGuideState,
    onPickVehicle: () -> Unit,
    onCaptureOdometer: () -> Unit,
    onToggleDraft: (Boolean) -> Unit,
    onStartTracking: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SheetCorner,
        dragHandle = { JourneyDragHandle() },
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Header + checklist + stepper live above the scroll region so they
            // stay anchored, matching the source sheet's structure.
            JourneyGuideHeader(state = state)

            JourneyStepper(step = state.step)

            // Scrollable content region.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VehicleSelectionCard(
                    vehicleName = state.vehicleName,
                    ratePerKm = state.vehicleRatePerKm,
                    onClick = onPickVehicle,
                )

                if (state.requiresOdometer || state.odometerCaptured) {
                    StartOdometerRow(
                        reading = state.startOdometer,
                        onCapture = onCaptureOdometer,
                    )
                }

                DraftModeCard(
                    enabled = state.draftEnabled,
                    onToggle = onToggleDraft,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            JourneyGuideBottomBar(
                step = state.step,
                canStart = state.canStart,
                onStartTracking = onStartTracking,
                onClose = onDismiss,
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyGuideHeader(state: JourneyGuideState) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Journey Guide",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stepSubtitle(state.step),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // The Quick Start Checklist is only meaningful before tracking starts.
            if (state.step != JourneyGuideStep.TRACKING) {
                val items = checklistItems(state)
                if (items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Quick Start Checklist",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        items.forEach { item ->
                            ChecklistRow(title = item.title, isComplete = item.complete)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** A single Quick Start Checklist line: a circle (empty) or a filled check + label. */
@Composable
private fun ChecklistRow(
    title: String,
    isComplete: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color =
            if (isComplete) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isComplete) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(2.dp).size(16.dp),
                    )
                }
            } else {
                // Empty (incomplete) state: an outlined circle.
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(20.dp),
                ) {}
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ── Stepper (geometry ported 1:1 from the source JourneyStepper) ────────────────

@Composable
private fun JourneyStepper(step: JourneyGuideStep) {
    val steps =
        listOf(
            JourneyGuideStep.PERMISSIONS,
            JourneyGuideStep.VEHICLE,
            JourneyGuideStep.TRACKING,
        )
    val currentIndex = steps.indexOf(step)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, _ ->
            val isCurrent = index == currentIndex
            val isCompleted = index < currentIndex

            Surface(
                shape = CircleShape,
                color =
                    when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                modifier = Modifier.size(StepCircleSize),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(StepCheckSize),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = (index + 1).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color =
                                if (isCurrent) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            if (index < steps.size - 1) {
                val connectorComplete = index < currentIndex
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(StepConnectorHeight)
                            .background(
                                if (connectorComplete) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                )
            }
        }
    }
}

// ── Vehicle selection card ──────────────────────────────────────────────────────

@Composable
private fun VehicleSelectionCard(
    vehicleName: String?,
    ratePerKm: Double?,
    onClick: () -> Unit,
) {
    SectionCard(title = "Vehicle Selection") {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vehicle Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = vehicleName ?: "Select a vehicle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            if (vehicleName != null) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    if (vehicleName != null && ratePerKm != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "₹$ratePerKm/km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Change vehicle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Start odometer row ──────────────────────────────────────────────────────────

@Composable
private fun StartOdometerRow(
    reading: Int?,
    onCapture: () -> Unit,
) {
    SectionCard(title = "Start Odometer") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Thumbnail placeholder for the (future) captured photo.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp),
            ) {}

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Start Odometer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = reading?.toString() ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (reading != null) {
                            "Captured · tap camera to retake"
                        } else {
                            "Tap camera icon to capture"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier =
                    Modifier
                        .size(44.dp)
                        .clickable(onClick = onCapture)
                        .semantics { contentDescription = "Capture start odometer" },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

// ── Draft mode card ─────────────────────────────────────────────────────────────

@Composable
private fun DraftModeCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    SectionCard(
        title = "Draft mode",
        subtitle = "Enable this to save your journey when you stop.",
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable draft mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "You can save this journey as a draft when you stop.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

// ── Bottom action bar ───────────────────────────────────────────────────────────

@Composable
private fun JourneyGuideBottomBar(
    step: JourneyGuideStep,
    canStart: Boolean,
    onStartTracking: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Step label chip.
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Current step: ${stepChipLabel(step)}"
                        },
                ) {
                    Text(
                        text = stepChipLabel(step),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                // Start Tracking CTA.
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color =
                        if (canStart) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(CtaHeight)
                            .clickable(enabled = canStart, onClick = onStartTracking),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint =
                                if (canStart) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Start Tracking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                if (canStart) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Close",
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ── Consent / declaration sheet ─────────────────────────────────────────────────

/**
 * The consent step shown right before a journey begins ("I Accept & Start
 * Journey"). Structure ported from the production declaration sheet: a rounded
 * icon tile, a bold title, a "Journey Guide" persona chip, the [disclaimer] body
 * and a primary confirm button.
 *
 * Stateless: [onAccept] fires when the user accepts; [onDismiss] on swipe-down or
 * scrim tap.
 *
 * @param disclaimer The consent/declaration body text to display.
 * @param onAccept Invoked when the user taps "I Accept & Start Journey".
 * @param onDismiss Invoked when the sheet is dismissed without accepting.
 * @param title Heading shown beside the icon; defaults to "Journey consent".
 * @param confirmLabel Primary button label; defaults to "I Accept & Start Journey".
 * @param sheetState The [ModalBottomSheet] state; defaults to fully-expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyConsentSheet(
    disclaimer: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Journey consent",
    confirmLabel: String = "I Accept & Start Journey",
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SheetCorner,
        dragHandle = { JourneyDragHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(14.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConsentPersonaChip(label = "Journey Guide")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = disclaimer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 21.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(CtaHeight)
                        .clickable(onClick = onAccept),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ConsentPersonaChip(label: String) {
    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = CircleShape,
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Shared bits ─────────────────────────────────────────────────────────────────

@Composable
private fun JourneyDragHandle() {
    Box(
        modifier =
            Modifier
                .padding(vertical = 12.dp)
                .width(DragHandleWidth)
                .height(DragHandleHeight)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(100),
                ),
    )
}

private data class ChecklistItem(val title: String, val complete: Boolean)

/** Builds the Quick Start Checklist rows from the current state. */
private fun checklistItems(state: JourneyGuideState): List<ChecklistItem> =
    buildList {
        add(ChecklistItem("Select Vehicle", state.vehicleSelected))
        if (state.requiresOdometer) {
            add(ChecklistItem("Capture Start Odometer", state.odometerCaptured))
        }
    }

/** Subtitle under the header, per step. */
private fun stepSubtitle(step: JourneyGuideStep): String =
    when (step) {
        JourneyGuideStep.PERMISSIONS -> "Grant the permissions needed to track"
        JourneyGuideStep.VEHICLE -> "Choose your vehicle and (if required) capture start odometer"
        JourneyGuideStep.TRACKING -> "Tracking in progress"
    }

/** Short chip label shown in the bottom bar, per step. */
private fun stepChipLabel(step: JourneyGuideStep): String =
    when (step) {
        JourneyGuideStep.PERMISSIONS -> "Step 1"
        JourneyGuideStep.VEHICLE -> "Step 2"
        JourneyGuideStep.TRACKING -> "Step 3"
    }
