@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming")

package com.mileway.feature.tracking.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mileway.core.ui.components.CollapsibleSectionCard
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_cd_open_in_maps
import com.mileway.core.ui.resources.tracking_discard_journey
import com.mileway.core.ui.resources.tracking_stat_avg_speed
import com.mileway.core.ui.resources.tracking_stat_distance
import com.mileway.core.ui.resources.tracking_stat_duration
import com.mileway.core.ui.resources.tracking_submit_add_attachments
import com.mileway.core.ui.resources.tracking_submit_additional_details
import com.mileway.core.ui.resources.tracking_submit_additional_details_subtitle
import com.mileway.core.ui.resources.tracking_submit_all_set
import com.mileway.core.ui.resources.tracking_submit_attachments
import com.mileway.core.ui.resources.tracking_submit_attachments_subtitle
import com.mileway.core.ui.resources.tracking_submit_cd_capture
import com.mileway.core.ui.resources.tracking_submit_cd_edit
import com.mileway.core.ui.resources.tracking_submit_cd_remove
import com.mileway.core.ui.resources.tracking_submit_continue_ready
import com.mileway.core.ui.resources.tracking_submit_field_select
import com.mileway.core.ui.resources.tracking_submit_journey_summary
import com.mileway.core.ui.resources.tracking_submit_location_details
import com.mileway.core.ui.resources.tracking_submit_manual
import com.mileway.core.ui.resources.tracking_submit_max_speed
import com.mileway.core.ui.resources.tracking_submit_odo_calculated
import com.mileway.core.ui.resources.tracking_submit_odo_end
import com.mileway.core.ui.resources.tracking_submit_odo_end_label
import com.mileway.core.ui.resources.tracking_submit_odo_not_captured
import com.mileway.core.ui.resources.tracking_submit_odo_readings
import com.mileway.core.ui.resources.tracking_submit_odo_start
import com.mileway.core.ui.resources.tracking_submit_odo_start_label
import com.mileway.core.ui.resources.tracking_submit_own_vehicle
import com.mileway.core.ui.resources.tracking_submit_pending_sync_desc
import com.mileway.core.ui.resources.tracking_submit_pending_sync_title
import com.mileway.core.ui.resources.tracking_submit_photo
import com.mileway.core.ui.resources.tracking_submit_ready
import com.mileway.core.ui.resources.tracking_submit_remaining
import com.mileway.core.ui.resources.tracking_submit_required_field
import com.mileway.core.ui.resources.tracking_submit_required_for_submission
import com.mileway.core.ui.resources.tracking_submit_review_title
import com.mileway.core.ui.resources.tracking_submit_save_draft_btn
import com.mileway.core.ui.resources.tracking_submit_save_draft_desc
import com.mileway.core.ui.resources.tracking_submit_save_draft_toggle
import com.mileway.core.ui.resources.tracking_submit_submit_miles
import com.mileway.core.ui.resources.tracking_submit_use_camera_gallery
import com.mileway.core.ui.resources.tracking_submit_vehicle
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

// Stateless building blocks for the mileage submission ("Review and submit journey") screen.
// UDF: data arrives through parameters, interactions surface through callbacks.
// None own a ViewModel; each piece is independently previewable and testable.
// Custom-form field model is shared by AdditionalDetailsForm.

/** Input control rendered for a [FormField]. */
enum class FormFieldType {
    /** Free-text single-line input rendered as an [OutlinedTextField]. */
    TEXT,

    /** A fixed set of [FormField.options] rendered as an [ExposedDropdownMenuBox]. */
    DROPDOWN,
}

/**
 * One dynamic custom-form field on the submission screen.
 *
 * @param id Stable identifier used to route value changes back to the caller.
 * @param label Human-readable label shown above the control.
 * @param type Which input control to render, see [FormFieldType].
 * @param value Current value (the source of truth lives in the caller).
 * @param required When true, a red asterisk is appended to the label.
 * @param options Choices for [FormFieldType.DROPDOWN]; ignored for text fields.
 * @param errorText Validation message shown in error color beneath the control, or null when valid.
 */
data class FormField(
    val id: String,
    val label: String,
    val type: FormFieldType,
    val value: String,
    val required: Boolean,
    val options: List<String> = emptyList(),
    val errorText: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// 1. Checklist header, "Review and submit journey"
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Tinted header card summarising what still needs attention before the journey can be submitted.
 *
 * Shows the title, an "N remaining" pill, and a wrap-row of requirement chips (e.g. "Office
 * selection", "Entity selection", "Complete required fields"). When [remaining] is 0 the card
 * switches to an "All set" confirmation with a check icon.
 *
 * @param remaining Number of outstanding requirements; drives the pill and the all-set state.
 * @param requirements Labels for the outstanding requirements, rendered as chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubmissionChecklistHeader(
    remaining: Int,
    requirements: List<String>,
    modifier: Modifier = Modifier,
) {
    val allSet = remaining <= 0
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (allSet) stringResource(Res.string.tracking_submit_all_set) else stringResource(Res.string.tracking_submit_review_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                RemainingPill(remaining = remaining)
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            if (allSet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = DesignTokens.StatusColors.success,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    Text(
                        text = stringResource(Res.string.tracking_submit_continue_ready),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    requirements.forEach { req -> RequirementChip(label = req) }
                }
            }
        }
    }
}

/** The "N remaining" pill in the checklist header. Turns green when nothing is outstanding. */
@Composable
private fun RemainingPill(remaining: Int) {
    val allSet = remaining <= 0
    val container =
        if (allSet) {
            DesignTokens.StatusColors.success.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        }
    val content =
        if (allSet) DesignTokens.StatusColors.success else MaterialTheme.colorScheme.primary
    Surface(shape = DesignTokens.Shape.button,
 color = container) {
        Text(
            text = if (allSet) stringResource(Res.string.tracking_submit_ready) else stringResource(Res.string.tracking_submit_remaining, remaining),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.xs),
        )
    }
}

/** A single rounded requirement chip shown in the checklist header wrap-row. */
@Composable
private fun RequirementChip(label: String) {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.s),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Pending data-sync warning
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pink/tertiary-tinted warning card shown when some location points have not been synced yet.
 *
 * @param pendingPoints Number of location points still pending sync.
 * @param totalPoints Total number of recorded location points.
 */
@Composable
fun PendingDataSyncCard(
    pendingPoints: Int,
    totalPoints: Int,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.tracking_submit_pending_sync_title),
        modifier = modifier,
        leadingIcon = Icons.Filled.Sync,
        titleColor = MaterialTheme.colorScheme.onTertiaryContainer,
        leadingIconTint = MaterialTheme.colorScheme.onTertiaryContainer,
        leadingIconContainerColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.10f),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            ) {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
                Text(
                    text = "$pendingPoints of $totalPoints location points pending sync",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Text(
                text = stringResource(Res.string.tracking_submit_pending_sync_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Journey summary (2x2 metric grid)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Journey Summary" card with a 2×2 grid of the trip's key metrics, each with a leading icon.
 *
 * Values are passed pre-formatted (e.g. "2.38 km", "00:10:28") so this component owns no
 * formatting logic.
 */
@Composable
fun JourneySummaryCard(
    distanceText: String,
    durationText: String,
    maxSpeedText: String,
    avgSpeedText: String,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.tracking_submit_journey_summary),
        modifier = modifier,
        leadingIcon = Icons.Outlined.Route,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
                MetricCell(
                    icon = Icons.Outlined.Place,
                    label = stringResource(Res.string.tracking_stat_distance),
                    value = distanceText,
                    modifier = Modifier.weight(1f),
                )
                MetricCell(
                    icon = Icons.Outlined.Timer,
                    label = stringResource(Res.string.tracking_stat_duration),
                    value = durationText,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
                MetricCell(
                    icon = Icons.Outlined.Speed,
                    label = stringResource(Res.string.tracking_submit_max_speed),
                    value = maxSpeedText,
                    modifier = Modifier.weight(1f),
                )
                MetricCell(
                    icon = Icons.Outlined.Speed,
                    label = stringResource(Res.string.tracking_stat_avg_speed),
                    value = avgSpeedText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A single icon + label + bold-value cell used in the journey-summary grid. */
@Composable
private fun MetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.inline),
            )
            Spacer(Modifier.width(DesignTokens.Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Location details
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Location Details" card listing the start/end addresses and times, with a trailing open-in-maps
 * action.
 *
 * @param onOpenInMaps Invoked when the open-in-maps icon is tapped (e.g. launch a map intent).
 */
@Composable
fun LocationDetailsCard(
    startAddress: String,
    endAddress: String,
    startTime: String,
    endTime: String,
    modifier: Modifier = Modifier,
    onOpenInMaps: () -> Unit = {},
) {
    SectionCard(
        title = stringResource(Res.string.tracking_submit_location_details),
        modifier = modifier,
        leadingIcon = Icons.Outlined.Place,
        trailingAction = {
            IconButton(onClick = onOpenInMaps) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = stringResource(Res.string.tracking_cd_open_in_maps),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            LocationLine(label = stringResource(Res.string.tracking_submit_odo_start), address = startAddress, time = startTime)
            LocationLine(label = stringResource(Res.string.tracking_submit_odo_end), address = endAddress, time = endTime)
        }
    }
}

/** One labelled address + time entry inside [LocationDetailsCard]. */
@Composable
private fun LocationLine(
    label: String,
    address: String,
    time: String,
) {
    Column {
        Text(
            text = "$label: $address",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.inline),
            )
            Spacer(Modifier.width(DesignTokens.Spacing.xs))
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Odometer readings (collapsible)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Collapsible "Odometer Readings" card with a start row and an end row. Each row shows a thumbnail
 * placeholder, the captured reading, a "Manual" chip when the reading was entered by hand, and
 * camera/edit actions. The calculated odometer distance is shown below both rows when available.
 *
 * @param startReading Odometer value at the start of the trip, or null if not captured.
 * @param endReading Odometer value at the end of the trip, or null if not captured.
 * @param isManualStart Whether the start reading was entered manually (shows a "Manual" chip).
 * @param isManualEnd Whether the end reading was entered manually (shows a "Manual" chip).
 * @param odometerDistanceKm Distance derived from the two readings (km), or null if not computable.
 * @param onCaptureStart Invoked to capture/edit the start reading.
 * @param onCaptureEnd Invoked to capture/edit the end reading.
 */
@Composable
fun OdometerReadingsCard(
    startReading: Int?,
    endReading: Int?,
    isManualStart: Boolean,
    isManualEnd: Boolean,
    odometerDistanceKm: Double?,
    onCaptureStart: () -> Unit,
    onCaptureEnd: () -> Unit,
    startImageUri: String? = null,
    endImageUri: String? = null,
    modifier: Modifier = Modifier,
) {
    CollapsibleSectionCard(
        title = stringResource(Res.string.tracking_submit_odo_readings),
        modifier = modifier,
        initiallyExpanded = true,
        leadingIcon = Icons.Outlined.Speed,
    ) {
        OdometerRow(
            label = stringResource(Res.string.tracking_submit_odo_start_label),
            reading = startReading,
            isManual = isManualStart,
            imageUri = startImageUri,
            onCapture = onCaptureStart,
        )
        OdometerRow(
            label = stringResource(Res.string.tracking_submit_odo_end_label),
            reading = endReading,
            isManual = isManualEnd,
            imageUri = endImageUri,
            onCapture = onCaptureEnd,
        )
        if (odometerDistanceKm != null) {
            Text(
                text = stringResource(Res.string.tracking_submit_odo_calculated, formatKm(odometerDistanceKm)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
            )
        }
    }
}

/** A single start/end odometer row: thumbnail, reading, manual chip, capture action. */
@Composable
private fun OdometerRow(
    label: String,
    reading: Int?,
    isManual: Boolean,
    imageUri: String?,
    onCapture: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Thumbnail: actual photo if captured, camera icon placeholder otherwise.
        Box(
            modifier =
                Modifier
                    .size(width = 80.dp, height = 60.dp)
                    .clip(DesignTokens.Shape.roundedSm)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = stringResource(Res.string.tracking_submit_photo, label),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
        }

        Spacer(Modifier.width(DesignTokens.Spacing.m))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (isManual) {
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    ManualChip()
                }
            }
            Text(
                text = reading?.toString() ?: stringResource(Res.string.tracking_submit_odo_not_captured),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color =
                    if (reading != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }

        IconButton(onClick = onCapture) {
            Icon(
                imageVector = if (reading == null) Icons.Filled.CameraAlt else Icons.Filled.Edit,
                contentDescription =
                    if (reading == null) {
                        stringResource(
                            Res.string.tracking_submit_cd_capture,
                            label,
                        )
                    } else {
                        stringResource(Res.string.tracking_submit_cd_edit, label)
                    },
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Small neutral "Manual" chip indicating a hand-entered reading. */
@Composable
private fun ManualChip() {
    Surface(
        shape = DesignTokens.Shape.chip,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(Res.string.tracking_submit_manual),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.s, vertical = 2.dp),
        )
    }
}

/** Trim a kilometre value to at most two decimals without trailing zeros. */
private fun formatKm(value: Double): String {
    val rounded = (value * 100).toLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Additional details (dynamic custom form, collapsible)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Collapsible "Additional Details (N)" section that renders the policy's dynamic custom-form
 * fields. [FormFieldType.TEXT] fields render as [OutlinedTextField]s and [FormFieldType.DROPDOWN]
 * fields render as an [ExposedDropdownMenuBox]. Required fields show a red asterisk and any
 * [FormField.errorText] is shown beneath the control in the error color.
 *
 * @param fields The custom-form fields to render. The header count reflects [fields].size.
 * @param onValueChange Invoked with (fieldId, newValue) whenever a control's value changes.
 */
@Composable
fun AdditionalDetailsForm(
    fields: List<FormField>,
    onValueChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    CollapsibleSectionCard(
        title = stringResource(Res.string.tracking_submit_additional_details, fields.size),
        modifier = modifier,
        initiallyExpanded = true,
        subtitle = stringResource(Res.string.tracking_submit_additional_details_subtitle),
    ) {
        fields.forEach { field ->
            when (field.type) {
                FormFieldType.TEXT -> FormTextField(field = field, onValueChange = onValueChange)
                FormFieldType.DROPDOWN -> FormDropdownField(field = field, onValueChange = onValueChange)
            }
        }
    }
}

/** Composes a label with a red asterisk appended when [required]. */
@Composable
private fun FieldLabel(
    label: String,
    required: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (required) {
            Text(
                text = "*",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Error helper text rendered in the error color, or nothing when [errorText] is null. */
@Composable
private fun FieldError(errorText: String?) {
    if (errorText != null) {
        Text(
            text = errorText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
        )
    }
}

/** A free-text custom-form field. */
@Composable
private fun FormTextField(
    field: FormField,
    onValueChange: (String, String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(label = field.label, required = field.required)
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        OutlinedTextField(
            value = field.value,
            onValueChange = { onValueChange(field.id, it) },
            placeholder = { Text(field.label) },
            isError = field.errorText != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(field.errorText)
    }
}

/** A single-select dropdown custom-form field backed by [FormField.options]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdownField(
    field: FormField,
    onValueChange: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        FieldLabel(label = field.label, required = field.required)
        Spacer(Modifier.height(DesignTokens.Spacing.xs))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = field.value,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(Res.string.tracking_submit_field_select, field.label)) },
                isError = field.errorText != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                field.options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(field.id, option)
                            expanded = false
                        },
                    )
                }
            }
        }
        FieldError(field.errorText)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Office / Entity select row
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A selectable "Office / Entity" row with a leading building icon, the current [value] (or the
 * [requiredHint] placeholder when null) and a trailing chevron. When [value] is null a red
 * "Required field" line is shown beneath.
 *
 * @param label Section label, e.g. "Office" or "Entity".
 * @param value The chosen value, or null when nothing is selected yet.
 * @param requiredHint Placeholder shown when [value] is null, e.g. "Select Office (Required)".
 * @param onClick Invoked when the row is tapped (e.g. to open the picker sheet).
 */
@Composable
fun OfficeEntitySelectRow(
    label: String,
    value: String?,
    requiredHint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMissing = value == null
    SectionCard(
        title = label,
        modifier = modifier,
        subtitle = stringResource(Res.string.tracking_submit_required_for_submission),
        leadingIcon = Icons.Outlined.Apartment,
    ) {
        Column {
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.surface,
                border =
                    BorderStroke(
                        1.dp,
                        if (isMissing) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        },
                    ),
                modifier = Modifier.fillMaxWidth().clip(DesignTokens.Shape.roundedSm).clickable(onClick = onClick),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = value ?: requiredHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            if (isMissing) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isMissing) {
                Text(
                    text = stringResource(Res.string.tracking_submit_required_field),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. Attachments
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Attachments" section with a dashed "Add Attachments" tile and a wrap-row of thumbnails. Each
 * thumbnail carries a delete overlay.
 *
 * @param attachments Labels/identifiers for the currently attached items (rendered as thumbnails).
 * @param onAdd Invoked when the add tile is tapped (e.g. open camera/gallery picker).
 * @param onRemove Invoked with the attachment identifier to remove.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentsSection(
    attachments: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.tracking_submit_attachments),
        modifier = modifier,
        subtitle = stringResource(Res.string.tracking_submit_attachments_subtitle),
        leadingIcon = Icons.Filled.Add,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
            // Dashed add tile.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .clip(DesignTokens.Shape.roundedSm)
                        .dashedBorder(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = DesignTokens.Shape.roundedSm,
                        )
                        .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.header),
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.xs))
                    Text(
                        text = stringResource(Res.string.tracking_submit_add_attachments),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.tracking_submit_use_camera_gallery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (attachments.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    attachments.forEach { attachment ->
                        AttachmentThumbnail(
                            label = attachment,
                            onRemove = { onRemove(attachment) },
                        )
                    }
                }
            }
        }
    }
}

/** A square attachment thumbnail placeholder with a delete overlay in the top-right corner. */
@Composable
private fun AttachmentThumbnail(
    label: String,
    onRemove: () -> Unit,
) {
    Box(modifier = Modifier.size(72.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(DesignTokens.Shape.roundedSm)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.take(2).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Delete overlay.
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(DesignTokens.Shape.button)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(Res.string.tracking_submit_cd_remove, label),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. Sticky bottom bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Sticky bottom action bar for the submission screen: a "Save as Draft" toggle with an
 * informational row, plus Discard (outlined) and "Submit Miles" (filled) actions. The submit
 * button is disabled until [submitEnabled] is true.
 *
 * @param saveAsDraft Whether the draft toggle is on.
 * @param onToggleDraft Invoked with the new toggle value.
 * @param submitEnabled Whether the submit button is enabled.
 * @param onDiscard Invoked when Discard is tapped.
 * @param onSubmit Invoked when Submit Miles is tapped.
 * @param infoText Guidance line shown above the toggle, e.g. "Review journey details…".
 */
@Composable
fun MileageDraftBottomBar(
    saveAsDraft: Boolean,
    onToggleDraft: (Boolean) -> Unit,
    submitEnabled: Boolean,
    onDiscard: () -> Unit,
    onSubmit: () -> Unit,
    infoText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.raised,
        shadowElevation = DesignTokens.Elevation.raised,
    ) {
        // Inner content clears the gesture-nav area (the bar itself spans full-bleed).
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            // Info + draft toggle, on a subtle tinted surface.
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.m)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = DesignTokens.Shape.button,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(6.dp).size(DesignTokens.IconSize.inline),
                            )
                        }
                        Spacer(Modifier.width(DesignTokens.Spacing.s))
                        Text(
                            text = infoText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.s))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = saveAsDraft,
                            onCheckedChange = onToggleDraft,
                        )
                        Spacer(Modifier.width(DesignTokens.Spacing.m))
                        Column {
                            Text(
                                text = stringResource(Res.string.tracking_submit_save_draft_toggle),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.tracking_submit_save_draft_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Action buttons.
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.tracking_discard_journey))
                }
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = onSubmit,
                    enabled = submitEnabled,
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (saveAsDraft) stringResource(Res.string.tracking_submit_save_draft_btn) else stringResource(Res.string.tracking_submit_submit_miles),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draws a dashed rounded border using the platform stroke API. Kept local so the attachments tile
 * can render the dashed outline seen on the submission screen without pulling in a dependency.
 */
private fun Modifier.dashedBorder(
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp,
    dashLength: Float = 12f,
    gapLength: Float = 8f,
): Modifier =
    this.then(
        Modifier.androidx_drawBehindDashed(color, shape, strokeWidth, dashLength, gapLength),
    )

// ─────────────────────────────────────────────────────────────────────────────
// 11. Vehicle summary card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Non-interactive summary card for the selected vehicle: icon, name, and rate per km.
 * Shown in the submission screen's "Vehicle" section.
 */
@Composable
fun VehicleSummaryCard(
    vehicleName: String,
    ratePerKm: Double,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.tracking_submit_vehicle),
        modifier = modifier,
        leadingIcon = Icons.Filled.DirectionsCar,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(DesignTokens.Shape.roundedSm)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.header),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicleName.ifBlank { stringResource(Res.string.tracking_submit_own_vehicle) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (ratePerKm > 0.0) {
                    Text(
                        text = "₹${(ratePerKm * 100).toLong() / 100.0} / km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Backing draw modifier for [dashedBorder]. */
private fun Modifier.androidx_drawBehindDashed(
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
    strokeWidth: androidx.compose.ui.unit.Dp,
    dashLength: Float,
    gapLength: Float,
): Modifier =
    this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path =
            when (outline) {
                is androidx.compose.ui.graphics.Outline.Generic -> outline.path
                is androidx.compose.ui.graphics.Outline.Rounded ->
                    androidx.compose.ui.graphics.Path().apply { addRoundRect(outline.roundRect) }
                is androidx.compose.ui.graphics.Outline.Rectangle ->
                    androidx.compose.ui.graphics.Path().apply { addRect(outline.rect) }
            }
        drawPath(
            path = path,
            color = color,
            style =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth.toPx(),
                    pathEffect =
                        androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(dashLength, gapLength),
                            0f,
                        ),
                ),
        )
    }
