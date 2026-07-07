package com.mileway.feature.tracking.ui.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_action_cancel
import com.mileway.core.ui.resources.tracking_action_close
import com.mileway.core.ui.resources.tracking_action_discard
import com.mileway.core.ui.resources.tracking_action_resume
import com.mileway.core.ui.resources.tracking_pause_add_custom
import com.mileway.core.ui.resources.tracking_pause_at
import com.mileway.core.ui.resources.tracking_pause_cd_selected
import com.mileway.core.ui.resources.tracking_pause_char_count
import com.mileway.core.ui.resources.tracking_pause_custom_placeholder
import com.mileway.core.ui.resources.tracking_pause_quick_reasons
import com.mileway.core.ui.resources.tracking_pause_reason_break
import com.mileway.core.ui.resources.tracking_pause_reason_fuel
import com.mileway.core.ui.resources.tracking_pause_reason_meeting
import com.mileway.core.ui.resources.tracking_pause_reason_personal
import com.mileway.core.ui.resources.tracking_pause_reason_traffic
import com.mileway.core.ui.resources.tracking_pause_reason_vehicle_issue
import com.mileway.core.ui.resources.tracking_pause_title
import com.mileway.core.ui.resources.tracking_pause_use_quick
import com.mileway.core.ui.resources.tracking_pause_why
import com.mileway.core.ui.resources.tracking_restore_action_ignore
import com.mileway.core.ui.resources.tracking_restore_action_restore
import com.mileway.core.ui.resources.tracking_restore_check_dismissed
import com.mileway.core.ui.resources.tracking_restore_empty
import com.mileway.core.ui.resources.tracking_restore_sheet_subtitle
import com.mileway.core.ui.resources.tracking_restore_sheet_title
import com.mileway.core.ui.resources.tracking_resume_char_count
import com.mileway.core.ui.resources.tracking_resume_notes_placeholder
import com.mileway.core.ui.resources.tracking_resume_optional_notes
import com.mileway.core.ui.resources.tracking_resume_ready_continue
import com.mileway.core.ui.resources.tracking_resume_title
import com.mileway.core.ui.resources.tracking_resume_you_paused_for
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

// ─────────────────────────────────────────────────────────────────────────────
// Pause / Resume / Restore sheet bodies (STATELESS)
//
// These are pure, hoisted bottom-sheet *bodies*: data comes in via params and
// every interaction is surfaced through a callback. They deliberately do NOT
// host a ModalBottomSheet, manage selection state, or talk to a ViewModel, the
// integrator wraps each in its own ModalBottomSheet and owns the state. That
// keeps them trivially previewable and reusable.
//
// Sizes, spacing, springs and chip styling mirror the production tracker so the
// migrated UI keeps visual parity.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The six default quick-pause reasons, in display order.
 *
 * These are canonical English keys, not display text: they double as the icon
 * lookup key ([pauseReasonIcon]), the selection-match value, and the stored
 * reason. The localized chip label is resolved separately via [pauseReasonLabel].
 */
val DefaultPauseReasons: List<String> =
    listOf(
        "Break",
        "Meeting",
        "Traffic",
        "Fuel",
        "Personal",
        "Vehicle Issue",
    )

/** Maps a known quick reason to its leading icon; falls back to a neutral glyph. */
private fun pauseReasonIcon(reason: String): ImageVector =
    when (reason.lowercase()) {
        "break" -> Icons.Default.Coffee
        "meeting" -> Icons.Default.Event
        "traffic" -> Icons.Default.Traffic
        "fuel" -> Icons.Default.LocalGasStation
        "personal" -> Icons.Default.Person
        "vehicle issue" -> Icons.Default.DirectionsCar
        else -> Icons.Default.MoreHoriz
    }

/**
 * Localized display label for a quick reason. Canonical keys map to a string
 * resource; any other value (e.g. a custom free-text reason) is returned as-is.
 */
@Composable
private fun pauseReasonLabel(reason: String): String =
    when (reason.lowercase()) {
        "break" -> stringResource(Res.string.tracking_pause_reason_break)
        "meeting" -> stringResource(Res.string.tracking_pause_reason_meeting)
        "traffic" -> stringResource(Res.string.tracking_pause_reason_traffic)
        "fuel" -> stringResource(Res.string.tracking_pause_reason_fuel)
        "personal" -> stringResource(Res.string.tracking_pause_reason_personal)
        "vehicle issue" -> stringResource(Res.string.tracking_pause_reason_vehicle_issue)
        else -> reason
    }

/**
 * Body of the "Pause Tracking" sheet.
 *
 * Shows a title with a "Pausing at …" timestamp, a short rationale line, a wrap
 * grid of quick-reason [FilterChip]s, an "Add custom reason" affordance that
 * reveals a free-text field, and a Cancel / Pause Tracking action row. The
 * confirm button stays disabled until a reason (quick or custom) is provided,
 * matching the greyed-out state in the reference.
 *
 * Selection state is fully hoisted: pass [selectedReason] and [customReason] in,
 * react to [onSelectReason] / [onCustomReason], and resolve the final reason in
 * [onConfirm].
 *
 * @param timestamp Pre-formatted clock time shown next to the title (e.g. "3:12 AM").
 * @param reasons Quick-reason labels to render as chips. Defaults to the six standard reasons.
 * @param selectedReason Currently selected quick reason, or null when none / custom.
 * @param customReason Current custom free-text reason (empty when not in use).
 * @param showCustomInput Whether the custom-reason text field is revealed.
 * @param onSelectReason Invoked with the tapped chip label (or null to clear selection).
 * @param onCustomReason Invoked as the custom text changes (capped at 200 chars by the caller).
 * @param onToggleCustom Invoked to reveal/hide the custom input ([show] = desired visibility).
 * @param onConfirm Invoked with the resolved reason when Pause Tracking is tapped.
 * @param onCancel Invoked when Cancel or the close icon is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PauseReasonSheet(
    timestamp: String,
    selectedReason: String?,
    customReason: String,
    onSelectReason: (String?) -> Unit,
    onCustomReason: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    reasons: List<String> = DefaultPauseReasons,
    showCustomInput: Boolean = customReason.isNotEmpty(),
    onToggleCustom: (show: Boolean) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
    ) {
        // Header: title + timestamp on the left, close icon on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.tracking_pause_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.tracking_pause_at, timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.tracking_action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.tracking_pause_why),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.tracking_pause_quick_reasons),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            reasons.forEach { reason ->
                val isSelected = selectedReason == reason
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Toggle selection; clearing custom input on a fresh pick.
                        onSelectReason(if (isSelected) null else reason)
                        if (!isSelected) {
                            onToggleCustom(false)
                            onCustomReason("")
                        }
                    },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = pauseReasonIcon(reason),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(pauseReasonLabel(reason))
                        }
                    },
                    leadingIcon =
                        if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.tracking_pause_cd_selected),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // "Add custom reason" CTA, hidden once the custom field is showing.
        if (!showCustomInput) {
            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleCustom(true)
                    onSelectReason(null)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tracking_pause_add_custom))
            }
        }

        // Custom free-text input, revealed with a medium-bouncy expand + fade.
        AnimatedVisibility(
            visible = showCustomInput,
            enter =
                expandVertically(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = customReason,
                    onValueChange = { text ->
                        if (text.length <= 200) {
                            onCustomReason(text)
                            onSelectReason(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(stringResource(Res.string.tracking_pause_custom_placeholder))
                    },
                    supportingText = {
                        Text(
                            text = stringResource(Res.string.tracking_pause_char_count, customReason.length),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (customReason.length > 180) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = DesignTokens.Shape.roundedSm,
                )

                TextButton(
                    shape = DesignTokens.Shape.button,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleCustom(false)
                        onCustomReason("")
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(Res.string.tracking_pause_use_quick))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Resolve the final reason once so both the enabled state and confirm
        // payload agree. Kept non-null so [onConfirm] receives a plain String.
        val finalReason: String =
            when {
                !selectedReason.isNullOrBlank() -> selectedReason
                customReason.isNotBlank() -> customReason.trim()
                else -> ""
            }
        val isValid = finalReason.isNotBlank()

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.tracking_action_cancel))
            }

            Button(
                shape = DesignTokens.Shape.button,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isValid) onConfirm(finalReason)
                },
                modifier = Modifier.weight(1f),
                enabled = isValid,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tracking_pause_title))
            }
        }
    }
}

/**
 * Body of the "Resume Tracking?" sheet.
 *
 * Centred play glyph, a bold title, the reason the trip was paused for (quoted),
 * a divider, an optional "resume notes" field with a live character counter, and
 * a Cancel / Resume action row.
 *
 * @param pauseReason The reason the trip was paused for; shown quoted when present.
 * @param resumeNotes Current resume-notes text (hoisted).
 * @param onNotesChange Invoked as the notes change (caller caps length; this body caps at 200).
 * @param onResume Invoked with the trimmed notes when Resume is tapped.
 * @param onCancel Invoked when Cancel is tapped.
 */
@Composable
fun ResumeTrackingSheet(
    pauseReason: String?,
    resumeNotes: String,
    onNotesChange: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Text(
            text = stringResource(Res.string.tracking_resume_title),
            style =
                MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!pauseReason.isNullOrBlank()) {
            Text(
                text = stringResource(Res.string.tracking_resume_you_paused_for),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "\"${pauseReasonLabel(pauseReason)}\"",
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text(
                text = stringResource(Res.string.tracking_resume_ready_continue),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = stringResource(Res.string.tracking_resume_optional_notes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = resumeNotes,
            onValueChange = { if (it.length <= 200) onNotesChange(it) },
            placeholder = {
                Text(stringResource(Res.string.tracking_resume_notes_placeholder))
            },
            supportingText = {
                Text(
                    text = stringResource(Res.string.tracking_resume_char_count, resumeNotes.length),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                shape = DesignTokens.Shape.button,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.tracking_action_cancel))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                shape = DesignTokens.Shape.button,
                onClick = { onResume(resumeNotes.trim()) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(Res.string.tracking_action_resume))
            }
        }
    }
}

/**
 * An unfinished tracking session that can be restored, discarded or ignored.
 *
 * @param token Opaque session identifier; only a short prefix is shown.
 * @param label Human-friendly session name (e.g. "Server Session", "Draft").
 * @param timestamp Pre-formatted "when" string for the session.
 */
data class RestorableSession(
    val token: String,
    val label: String,
    val timestamp: String,
)

/**
 * Body of the "Drafts & Server Sessions" restore sheet.
 *
 * A leading history badge + title/subtitle, an optional "Check dismissed restore
 * sessions" affordance, then one row per [RestorableSession] showing the label
 * and a short token, with Restore / Discard / Ignore actions. When the list is
 * empty an unobtrusive empty-state line is shown instead.
 *
 * @param sessions Restorable sessions to list.
 * @param onRestore Invoked to restore the given session.
 * @param onDiscard Invoked to discard (resubmit) the given session.
 * @param onIgnore Invoked to ignore/dismiss the given session for now.
 * @param onCheckDismissed Optional: invoked when the "Check dismissed…" row is tapped; row hidden when null.
 */
@Composable
fun SessionRestoreSheet(
    sessions: List<RestorableSession>,
    onRestore: (RestorableSession) -> Unit,
    onDiscard: (RestorableSession) -> Unit,
    onIgnore: (RestorableSession) -> Unit,
    modifier: Modifier = Modifier,
    onCheckDismissed: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header: history badge + title/subtitle.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = DesignTokens.Shape.roundedSm,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(Res.string.tracking_restore_sheet_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.tracking_restore_sheet_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // "Check dismissed restore sessions" affordance (optional).
        if (onCheckDismissed != null) {
            OutlinedButton(
                shape = DesignTokens.Shape.button,
                onClick = onCheckDismissed,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.tracking_restore_check_dismissed))
            }
        }

        if (sessions.isEmpty()) {
            Text(
                text = stringResource(Res.string.tracking_restore_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        } else {
            sessions.forEach { session ->
                RestorableSessionRow(
                    session = session,
                    onRestore = { onRestore(session) },
                    onDiscard = { onDiscard(session) },
                    onIgnore = { onIgnore(session) },
                )
            }
        }
    }
}

/** A single restorable-session card with label, short token and three actions. */
@Composable
private fun RestorableSessionRow(
    session: RestorableSession,
    onRestore: () -> Unit,
    onDiscard: () -> Unit,
    onIgnore: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedSm,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = DesignTokens.Shape.button,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = shortToken(session.token),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (session.timestamp.isNotBlank()) {
                        Text(
                            text = session.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    shape = DesignTokens.Shape.button,
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(stringResource(Res.string.tracking_restore_action_restore), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    shape = DesignTokens.Shape.button,
                    onClick = onDiscard,
                    modifier = Modifier.weight(1f),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(stringResource(Res.string.tracking_action_discard), style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    shape = DesignTokens.Shape.button,
                    onClick = onIgnore,
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(stringResource(Res.string.tracking_restore_action_ignore), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Returns the first 7 characters of a token (or the whole token if shorter). */
private fun shortToken(token: String): String = if (token.length <= 7) token else token.take(7)
