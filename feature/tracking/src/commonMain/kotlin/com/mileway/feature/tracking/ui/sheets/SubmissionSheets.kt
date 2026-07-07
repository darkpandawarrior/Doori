@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.core.data.model.network.ViolationSeverity
import com.mileway.core.network.model.BusinessEntity
import com.mileway.core.network.model.Office
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_cancel
import com.mileway.core.ui.resources.tracking_entity_picker_title
import com.mileway.core.ui.resources.tracking_entity_search_placeholder
import com.mileway.core.ui.resources.tracking_office_gstin
import com.mileway.core.ui.resources.tracking_office_picker_title
import com.mileway.core.ui.resources.tracking_office_search_placeholder
import com.mileway.core.ui.resources.tracking_office_suggested
import com.mileway.core.ui.resources.tracking_policy_ask_authorities
import com.mileway.core.ui.resources.tracking_policy_fix_resubmit
import com.mileway.core.ui.resources.tracking_policy_fix_resubmit_desc
import com.mileway.core.ui.resources.tracking_policy_issue_found
import com.mileway.core.ui.resources.tracking_policy_note_placeholder
import com.mileway.core.ui.resources.tracking_policy_submit
import com.mileway.core.ui.resources.tracking_policy_submit_review
import com.mileway.core.ui.resources.tracking_policy_submit_review_desc
import com.mileway.core.ui.resources.tracking_policy_violations
import com.mileway.core.ui.resources.tracking_smart_confirm_stop
import com.mileway.core.ui.resources.tracking_smart_difference
import com.mileway.core.ui.resources.tracking_smart_explain_placeholder
import com.mileway.core.ui.resources.tracking_smart_gps_tracked
import com.mileway.core.ui.resources.tracking_smart_large_discrepancy
import com.mileway.core.ui.resources.tracking_smart_odometer
import com.mileway.core.ui.resources.tracking_smart_subtitle
import com.mileway.core.ui.resources.tracking_smart_verified
import com.mileway.core.ui.resources.tracking_stop_tracking_title
import com.mileway.core.ui.resources.tracking_submit_confirm_note
import com.mileway.core.ui.resources.tracking_submit_confirm_prompt
import com.mileway.core.ui.resources.tracking_submit_confirm_title
import com.mileway.core.ui.resources.tracking_submit_submit_miles
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Submission → success modal bottom sheets.
//
// Every sheet here is fully stateless and previewable: data flows in via params,
// events flow out via callbacks (UDF). The integrator owns the ViewModel, sheet
// visibility and navigation; these composables only render and emit intent.
//
// Colours derive from MaterialTheme.colorScheme; spacing/shape/status colours come
// from DesignTokens so the whole submission flow shares one visual language.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Threshold above which an odometer-vs-tracked discrepancy is flagged as a "Very
 * Large Discrepancy" (red pill) in [SmartDistanceSheet].
 */
private const val VERY_LARGE_DISCREPANCY_PERCENT = 50.0

/**
 * "Stop Tracking?" review sheet shown when the captured end-odometer reading is
 * larger than the GPS-tracked distance. It compares the two distances, explains the
 * gap, lets the user attest the readings are correct and optionally add a note, then
 * offers to stop or keep tracking.
 *
 * The discrepancy percentage is derived internally from [trackedKm]/[odometerKm];
 * callers do not pre-compute it. When the odometer exceeds tracked distance by more
 * than [VERY_LARGE_DISCREPANCY_PERCENT]% the banner and a "Very Large Discrepancy"
 * pill switch to the error palette.
 *
 * @param trackedKm GPS-tracked distance for the journey, in kilometres.
 * @param odometerKm Odometer-derived distance for the journey, in kilometres.
 * @param verified Whether the user has ticked "I have verified my readings are correct".
 * @param explanation Optional free-text explanation for the discrepancy.
 * @param onVerifiedChange Emitted when the verification checkbox toggles.
 * @param onExplanationChange Emitted as the explanation text changes.
 * @param onStop Emitted when the user confirms stopping tracking.
 * @param onContinue Emitted when the user chooses to keep tracking.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartDistanceSheet(
    trackedKm: Double,
    odometerKm: Double,
    verified: Boolean,
    explanation: String,
    onVerifiedChange: (Boolean) -> Unit,
    onExplanationChange: (String) -> Unit,
    onStop: () -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    // Percentage by which the odometer exceeds the tracked distance. Guard against a
    // zero/negative tracked distance so we never divide by zero.
    val discrepancyPercent: Double =
        if (trackedKm > 0.0) {
            ((odometerKm - trackedKm) / trackedKm) * 100.0
        } else {
            0.0
        }
    val isVeryLarge = discrepancyPercent > VERY_LARGE_DISCREPANCY_PERCENT
    val percentLabel = "${discrepancyPercent.roundToInt()}%"
    val errorColor = MaterialTheme.colorScheme.error

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            // Title row.
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.tracking_stop_tracking_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.tracking_smart_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = errorColor,
                    modifier = Modifier.size(DesignTokens.IconSize.header),
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Three-column distance comparison (ref 50 layout).
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    DistanceColumn(
                        label = stringResource(Res.string.tracking_smart_gps_tracked),
                        value = "${(trackedKm * 100).toLong() / 100.0} km",
                        valueColor = MaterialTheme.colorScheme.onSurface,
                    )
                    DistanceColumn(
                        label = stringResource(Res.string.tracking_smart_difference),
                        value = "$percentLabel",
                        valueColor = if (discrepancyPercent > 15) errorColor else MaterialTheme.colorScheme.onSurface,
                    )
                    DistanceColumn(
                        label = stringResource(Res.string.tracking_smart_odometer),
                        value = "${(odometerKm * 100).toLong() / 100.0} km",
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Warning banner shown only when discrepancy > 20%.
            if (discrepancyPercent > 20.0) {
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                Surface(
                    shape = DesignTokens.Shape.roundedSm,
                    color = errorColor.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.Spacing.m),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = errorColor, modifier = Modifier.size(DesignTokens.IconSize.badge))
                        Text(
                            text = stringResource(Res.string.tracking_smart_large_discrepancy, percentLabel),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = errorColor,
                        )
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Verification checkbox.
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onVerifiedChange(!verified) }
                            .padding(end = DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = verified, onCheckedChange = onVerifiedChange)
                    Text(
                        text = stringResource(Res.string.tracking_smart_verified),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Explanation field visible only when checkbox checked.
            if (verified) {
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                OutlinedTextField(
                    value = explanation,
                    onValueChange = onExplanationChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.tracking_smart_explain_placeholder)) },
                    shape = DesignTokens.Shape.roundedSm,
                    minLines = 2,
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Two-button row: Cancel (outlined) + Confirm Stop (filled, disabled until verified).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    shape = DesignTokens.Shape.roundedSm,
                ) {
                    Text(stringResource(Res.string.tracking_action_cancel))
                }
                Button(
                    onClick = onStop,
                    enabled = verified,
                    modifier = Modifier.weight(1f),
                    shape = DesignTokens.Shape.roundedSm,
                ) {
                    Text(stringResource(Res.string.tracking_smart_confirm_stop), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Single column in the three-column distance comparison row (ref 50). */
@Composable
private fun DistanceColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A label/value comparison row used by [SmartDistanceSheet]'s compare card. */
@Composable
private fun CompareRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize.badge),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

/**
 * "Submit Track Miles" confirmation sheet: a centred info icon, heading, prompt, an
 * info note and Submit/Cancel actions. Stateless, all three outcomes are callbacks.
 *
 * @param onConfirm Emitted when the user confirms submission.
 * @param onCancel Emitted when the user cancels.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitConfirmSheet(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Circular info badge.
            Box(
                modifier =
                    Modifier
                        .padding(top = DesignTokens.Spacing.s)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.header),
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            Text(
                text = stringResource(Res.string.tracking_submit_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))
            Text(
                text = stringResource(Res.string.tracking_submit_confirm_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Info note.
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(DesignTokens.Spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                    Text(
                        text = stringResource(Res.string.tracking_submit_confirm_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Primary action: submit.
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedSm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(stringResource(Res.string.tracking_submit_submit_miles), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.size(DesignTokens.Spacing.s))

            // Secondary action: cancel.
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize.inline),
                )
                Spacer(Modifier.size(DesignTokens.Spacing.s))
                Text(stringResource(Res.string.tracking_action_cancel))
            }
        }
    }
}

/**
 * "Policy violation" sheet shown when the policy engine reports one or more
 * violations the user must resolve before the submission can proceed. Lists each
 * [PolicyViolation] in a red card, then offers an "Ask Authorities" resolution that
 * reveals a note field. The Submit button is enabled only once the user has chosen a
 * resolution and entered a note.
 *
 * @param violations The policy violations returned for this submission.
 * @param askAuthoritiesSelected Whether the "Ask Authorities" resolution is chosen.
 * @param note Free-text note accompanying the resolution.
 * @param onToggleAskAuthorities Emitted when the "Ask Authorities" card is selected.
 * @param onNoteChange Emitted as the note text changes.
 * @param onSubmit Emitted when the user submits with a chosen resolution + note.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyViolationSheet(
    violations: List<PolicyViolation>,
    askAuthoritiesSelected: Boolean,
    note: String,
    onToggleAskAuthorities: () -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    // A resolution is chosen and a note has been entered → submission is allowed.
    val canSubmit = askAuthoritiesSelected && note.isNotBlank()
    val errorColor = MaterialTheme.colorScheme.error

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            // Header: amber warning icon + title.
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(DesignTokens.StatusColors.warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = DesignTokens.StatusColors.warning,
                        modifier = Modifier.size(DesignTokens.IconSize.badge),
                    )
                }
                Column {
                    Text(
                        text = stringResource(Res.string.tracking_policy_issue_found),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (violations.isNotEmpty()) {
                        Text(
                            text = "${violations.size} ${if (violations.size == 1) "violation" else "violations"} detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Red violations card listing each violation message.
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color = errorColor.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(DesignTokens.Spacing.l),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                ) {
                    Text(
                        text = stringResource(Res.string.tracking_policy_violations),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = errorColor,
                    )
                    violations.forEach { violation ->
                        ViolationRow(violation = violation, errorColor = errorColor)
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Resolution options header.
            Text(
                text = stringResource(Res.string.tracking_policy_ask_authorities),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(DesignTokens.Spacing.s))

            // Option 1: Submit for Review.
            val submitForReviewBorder =
                if (askAuthoritiesSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color =
                    if (askAuthoritiesSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                modifier = Modifier.fillMaxWidth().border(1.dp, submitForReviewBorder, DesignTokens.Shape.roundedSm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { if (!askAuthoritiesSelected) onToggleAskAuthorities() }.padding(DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    RadioButton(selected = askAuthoritiesSelected, onClick = { if (!askAuthoritiesSelected) onToggleAskAuthorities() })
                    Column {
                        Text(
                            stringResource(Res.string.tracking_policy_submit_review),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(Res.string.tracking_policy_submit_review_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.size(DesignTokens.Spacing.s))

            // Option 2: Fix and Resubmit.
            val fixBorder =
                if (!askAuthoritiesSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            Surface(
                shape = DesignTokens.Shape.roundedSm,
                color =
                    if (!askAuthoritiesSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                modifier = Modifier.fillMaxWidth().border(1.dp, fixBorder, DesignTokens.Shape.roundedSm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { if (askAuthoritiesSelected) onToggleAskAuthorities() }.padding(DesignTokens.Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                ) {
                    RadioButton(selected = !askAuthoritiesSelected, onClick = { if (askAuthoritiesSelected) onToggleAskAuthorities() })
                    Column {
                        Text(
                            stringResource(Res.string.tracking_policy_fix_resubmit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(Res.string.tracking_policy_fix_resubmit_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Note field visible only when "Submit for Review" is selected.
            if (askAuthoritiesSelected) {
                Spacer(Modifier.size(DesignTokens.Spacing.m))
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(Res.string.tracking_policy_note_placeholder)) },
                    shape = DesignTokens.Shape.roundedSm,
                    singleLine = true,
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Submit, enabled only with a chosen resolution + note.
            Button(
                onClick = onSubmit,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = DesignTokens.Shape.roundedSm,
            ) {
                Text(stringResource(Res.string.tracking_policy_submit), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** A single violation line (warning icon + message) inside the red violations card. */
@Composable
private fun ViolationRow(
    violation: PolicyViolation,
    errorColor: Color,
) {
    // Hard stops use the strongest error tint; reimbursable/standard use a warning amber.
    val iconTint =
        when (violation.severity) {
            ViolationSeverity.HARDSTOP -> errorColor
            ViolationSeverity.VIOLATION -> errorColor
            ViolationSeverity.REIMBURSABLE -> DesignTokens.StatusColors.warning
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(DesignTokens.IconSize.badge),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (violation.title.isNotBlank()) {
                Text(
                    text = violation.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = violation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * "Select Office (Required)" picker: a search field over a list of office cards (map
 * icon, "code - name", address, "GSTIN: …", chevron). Filtering is applied internally
 * across code/name/address; selection emits the office [Office.code].
 *
 * @param offices The offices the user can pick from.
 * @param query Current search query.
 * @param onQueryChange Emitted as the query changes.
 * @param onSelect Emitted with the chosen office's code.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficePickerSheet(
    offices: List<Office>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val filtered =
        offices.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
        }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.tracking_office_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = DesignTokens.Spacing.m),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.tracking_office_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = DesignTokens.Shape.roundedSm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            // Section header + result count.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tracking_office_suggested),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${filtered.size} ${if (filtered.size == 1) "result" else "results"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.size(DesignTokens.Spacing.m))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                modifier = Modifier.heightIn(max = 460.dp),
            ) {
                items(filtered, key = { it.code }) { office ->
                    OfficeRow(office = office, onClick = { onSelect(office.code) })
                }
            }
        }
    }
}

@Composable
private fun OfficeRow(
    office: Office,
    onClick: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(DesignTokens.Shape.roundedSm)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${office.code} - ${office.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (office.address.isNotBlank()) {
                    Text(
                        text = office.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (office.gstin.isNotBlank()) {
                    Text(
                        text = stringResource(Res.string.tracking_office_gstin, office.gstin),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * "Select Entity" picker: a count line, a search field and a list of entity cards
 * (building icon, name, "country • Currency: X"). Filtering is applied internally on
 * the entity name; selection emits the entity [BusinessEntity.name].
 *
 * @param entities The business entities the user can pick from.
 * @param query Current search query.
 * @param onQueryChange Emitted as the query changes.
 * @param onSelect Emitted with the chosen entity's name.
 * @param onDismiss Emitted when the sheet is dismissed (scrim tap / drag down).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityPickerSheet(
    entities: List<BusinessEntity>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val filtered = entities.filter { it.name.contains(query, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.l)
                    .padding(bottom = DesignTokens.Spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.tracking_entity_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = DesignTokens.Spacing.m),
            )
            Text(
                text = "${entities.size} ${if (entities.size == 1) "entity" else "entities"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.m),
            )
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.tracking_entity_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = DesignTokens.Shape.roundedSm,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )

            Spacer(Modifier.size(DesignTokens.Spacing.l))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
                modifier = Modifier.heightIn(max = 480.dp),
            ) {
                items(filtered, key = { it.name }) { entity ->
                    EntityRow(entity = entity, onClick = { onSelect(entity.name) })
                }
            }
        }
    }
}

@Composable
private fun EntityRow(
    entity: BusinessEntity,
    onClick: () -> Unit,
) {
    Surface(
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(DesignTokens.Spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Apartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(DesignTokens.IconSize.navigation),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = entitySubtitle(entity),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/**
 * Builds the "country • Currency: X" subtitle for an entity card, gracefully omitting
 * the country segment when none is set (mirrors the reference, which shows just the
 * currency for country-less entities).
 */
private fun entitySubtitle(entity: BusinessEntity): String {
    val currencyPart = "Currency: ${entity.currencySymbol.ifBlank { "—" }}"
    return if (entity.country.isNotBlank()) {
        "${entity.country} • $currencyPart"
    } else {
        currencyPart
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Odometer reading confirm sheet (Phase X.2)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Bottom sheet shown after the user captures an odometer photo. Simulates a 1.2s
 * OCR processing delay, then presents the detected reading for confirmation.
 *
 * @param capturedUri   Local file URI of the captured odometer photo.
 * @param purpose       START or END, drives the sheet title.
 * @param baseReading   Start odometer reading (used to derive the END reading).
 * @param sessionDistanceKm Tracked GPS distance (used to estimate END reading delta).
 * @param onUseReading  Invoked with the confirmed reading when the user taps "Use This Reading".
 * @param onRetake      Invoked when the user taps "Retake", caller re-opens the camera.
 * @param onDismiss     Invoked when the sheet is dismissed without a result.
 */
