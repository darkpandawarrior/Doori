@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:property-naming", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mileway.core.data.util.DateUtils
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.tracking_saved_cd_filters
import com.mileway.core.ui.resources.tracking_saved_journeys_count
import com.mileway.core.ui.resources.tracking_saved_no_journeys_week
import com.mileway.core.ui.resources.tracking_saved_no_journeys_week_hint
import com.mileway.core.ui.resources.tracking_saved_submissions_count
import com.mileway.core.ui.resources.tracking_saved_view_all
import com.mileway.core.ui.resources.tracking_status_approval_approved
import com.mileway.core.ui.resources.tracking_status_approval_pending
import com.mileway.core.ui.resources.tracking_status_approval_reimbursed
import com.mileway.core.ui.resources.tracking_status_approval_rejected
import com.mileway.core.ui.resources.tracking_submission_acknowledged
import com.mileway.core.ui.resources.tracking_submission_attachments
import com.mileway.core.ui.resources.tracking_submission_clear_selection
import com.mileway.core.ui.resources.tracking_submission_create_voucher_count
import com.mileway.core.ui.resources.tracking_submission_expense_date
import com.mileway.core.ui.resources.tracking_submission_expense_id
import com.mileway.core.ui.resources.tracking_submission_new_tracker
import com.mileway.core.ui.resources.tracking_submission_other
import com.mileway.core.ui.resources.tracking_submission_selected_count
import com.mileway.core.ui.resources.tracking_submission_self_paid
import com.mileway.core.ui.resources.tracking_submission_violations_count
import com.mileway.core.ui.resources.tracking_submission_voucher_filed
import com.mileway.core.ui.resources.tracking_submission_voucher_not_created
import com.mileway.core.ui.resources.tracking_track_miles_label
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.core.ui.theme.dataStyle
import com.siddharth.kmp.common.formatDecimal
import org.jetbrains.compose.resources.stringResource

// Stateless building blocks for the revamped Saved Tracks screen.
// UDF: data arrives through parameters, interactions surface through callbacks.
// None own a ViewModel; each piece is independently testable.

/** One pill of the [SavedTracksSegmentedToggle]. */
enum class SavedTracksSegment { JOURNEYS, SUBMISSIONS }

/**
 * Two outlined/filled pills letting the user flip between the Journeys and Submissions tabs.
 * The selected pill fills with the primary container; the other stays outlined.
 *
 * @param selected Currently active segment.
 * @param journeyCount Count rendered in the "Journeys (N)" label.
 * @param submissionCount Count rendered in the "Submissions (N)" label.
 * @param onSelect Invoked with the tapped segment.
 */
@Composable
fun SavedTracksSegmentedToggle(
    selected: SavedTracksSegment,
    journeyCount: Int,
    submissionCount: Int,
    onSelect: (SavedTracksSegment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
    ) {
        SegmentPill(
            label = stringResource(Res.string.tracking_saved_journeys_count, journeyCount),
            icon = Icons.Default.Map,
            selected = selected == SavedTracksSegment.JOURNEYS,
            onClick = { onSelect(SavedTracksSegment.JOURNEYS) },
            modifier = Modifier.weight(1f),
        )
        SegmentPill(
            label = stringResource(Res.string.tracking_saved_submissions_count, submissionCount),
            icon = Icons.Default.Receipt,
            selected = selected == SavedTracksSegment.SUBMISSIONS,
            onClick = { onSelect(SavedTracksSegment.SUBMISSIONS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = 300f),
        label = "segmentBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = 300f),
        label = "segmentContent",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = 300f),
        label = "segmentBorder",
    )

    Surface(
        modifier = modifier.height(44.dp),
        shape = DesignTokens.Shape.roundedSm,
        color = background,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (selected) DesignTokens.Elevation.raised else 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.badge))
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Rounded search field
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Pill-shaped, always-visible search field with a leading magnifier and a trailing filter glyph.
 *
 * @param query Current text (source of truth lives in the caller / ViewModel).
 * @param placeholder Hint text, e.g. "Search journeys…" or "Search submissions…".
 * @param onQueryChange Reports every keystroke.
 * @param onFilterClick Optional tap target on the trailing filter glyph; hidden when null.
 */
@Composable
fun SavedTracksSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFilterClick: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon =
            onFilterClick?.let { onClick ->
                {
                    IconButton(onClick = onClick) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(Res.string.tracking_saved_cd_filters),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        singleLine = true,
        shape = DesignTokens.Shape.button,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Filter chip rows
// ─────────────────────────────────────────────────────────────────────────────

/** A labelled, optionally-counted filter chip, the unit of every chip row below. */
@Composable
fun SavedTracksFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon =
            leadingIcon?.let {
                { Icon(it, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline)) }
            },
        shape = DesignTokens.Shape.chip,
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}

/**
 * Horizontal flow of filter chips. Wraps onto a second line on narrow screens instead of
 * clipping, which keeps the secondary Submissions chips fully reachable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedTracksChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Date group header
// ─────────────────────────────────────────────────────────────────────────────

/** Sticky-styled date label that separates submission groups (e.g. "Sun 31 May 2026"). */
@Composable
fun SubmissionDateHeader(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = DesignTokens.Spacing.s),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Submission card
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Stateless render data for a single [SubmissionCard]. The integrating screen maps its
 * ViewModel model onto this so the card never depends on feature-specific types.
 */
data class SubmissionCardData(
    val id: String,
    val transId: String,
    val amount: Double,
    val expenseDateMillis: Long,
    val attachmentCount: Int,
    val violationCount: Int,
    val acknowledged: Boolean,
    val isNewTracker: Boolean,
    val voucherCreated: Boolean,
    val approvalStatus: String = "Pending Approval",
    val voucherNumber: String? = null,
)

/**
 * One submitted-mileage expense, matching the production submission card:
 * a leading car/receipt icon, "#<transId>", a red "Voucher Not Created" status, "Self Paid",
 * a chip row (source / attachments / violations / acknowledged), the "₹<amount>" with a
 * "Track Miles" tag, and the expense date / id footer lines.
 *
 * Unclaimed cards show a checkbox and support long-press to enter selection mode.
 *
 * @param isSelected Whether this card is currently selected.
 * @param selectionMode When true the whole list is in selection mode (drives checkbox visibility).
 * @param onClick Tap, toggles selection in selection mode, otherwise opens (caller decides).
 * @param onLongClick Long-press, used to enter selection mode for unclaimed cards.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubmissionCard(
    data: SubmissionCardData,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val showCheckbox = data.voucherCreated.not() && (selectionMode || isSelected)

    // VI.3: card springs to 0.97 scale when selection mode is active (ready-to-select feel)
    val scale by animateFloatAsState(
        targetValue = if (selectionMode && !isSelected) 0.97f else 1.0f,
        animationSpec = spring(stiffness = 400f),
        label = "cardScale",
    )

    val approvalColor =
        when (data.approvalStatus) {
            "Approved" -> DesignTokens.StatusColors.success
            "Rejected" -> DesignTokens.StatusColors.error
            "Reimbursed" -> DesignTokens.StatusColors.success
            else -> DesignTokens.StatusColors.warning
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = DesignTokens.Shape.roundedMd,
        color =
            if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        tonalElevation = DesignTokens.Elevation.card,
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.l)) {
            // Header: icon + id/status block + (optional) selection checkbox.
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(DesignTokens.Shape.roundedSm)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(DesignTokens.IconSize.actionTile),
                    )
                }
                Spacer(Modifier.width(DesignTokens.Spacing.m))
                Column(modifier = Modifier.weight(1f)) {
                    // Monospace transaction ID (10sp per VI.2)
                    Text(
                        text = "#${data.transId}",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text =
                            if (data.voucherCreated) {
                                stringResource(
                                    Res.string.tracking_submission_voucher_filed,
                                )
                            } else {
                                stringResource(Res.string.tracking_submission_voucher_not_created)
                            },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (data.voucherCreated) DesignTokens.StatusColors.success else DesignTokens.StatusColors.error,
                    )
                    Text(
                        text = stringResource(Res.string.tracking_submission_self_paid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showCheckbox) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            // Chip row: source / attachments / violations / acknowledged.
            SubmissionChipRow(data = data)

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            // Amount (18sp SemiBold primary per VI.2) + approval status chip + Track Miles tag.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${data.amount.formatDecimal(2)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp).dataStyle(),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(DesignTokens.Spacing.m))
                ApprovalStatusChip(status = data.approvalStatus.localizedApprovalStatus(), color = approvalColor)
                Spacer(Modifier.width(DesignTokens.Spacing.s))
                TrackMilesTag()
            }

            // Voucher chip (if issued per VI.2)
            if (data.voucherNumber != null) {
                Spacer(Modifier.height(DesignTokens.Spacing.s))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
                    modifier =
                        Modifier
                            .clip(DesignTokens.Shape.chip)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(DesignTokens.IconSize.inline),
                    )
                    Text(
                        data.voucherNumber,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.m))

            // Footer: expense date + id lines.
            Row(modifier = Modifier.fillMaxWidth()) {
                FooterField(
                    label = stringResource(Res.string.tracking_submission_expense_date),
                    value = DateUtils.epochToDisplayDate(data.expenseDateMillis),
                    modifier = Modifier.weight(1f),
                )
                FooterField(label = stringResource(Res.string.tracking_submission_expense_id), value = data.transId, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Localized display text for the canonical (English) approval-status keys stored in
 * [SubmissionCardData.approvalStatus]. The raw value stays canonical since it also drives
 * [approvalColor]'s comparison; only the rendered chip text is localized.
 */
@Composable
private fun String.localizedApprovalStatus(): String =
    when (this) {
        "Approved" -> stringResource(Res.string.tracking_status_approval_approved)
        "Rejected" -> stringResource(Res.string.tracking_status_approval_rejected)
        "Reimbursed" -> stringResource(Res.string.tracking_status_approval_reimbursed)
        "Pending Approval" -> stringResource(Res.string.tracking_status_approval_pending)
        else -> this
    }

@Composable
private fun ApprovalStatusChip(
    status: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
    ) {
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmissionChipRow(data: SubmissionCardData) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
    ) {
        StatusPill(
            text = if (data.isNewTracker) stringResource(Res.string.tracking_submission_new_tracker) else stringResource(Res.string.tracking_submission_other),
            icon = Icons.Default.Map,
            color = MaterialTheme.colorScheme.primary,
        )
        if (data.attachmentCount > 0) {
            StatusPill(
                text = stringResource(Res.string.tracking_submission_attachments),
                icon = Icons.Default.AttachFile,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (data.violationCount > 0) {
            StatusPill(
                text = stringResource(Res.string.tracking_submission_violations_count, data.violationCount),
                icon = Icons.Default.Warning,
                color = DesignTokens.StatusColors.error,
            )
        }
        if (data.acknowledged) {
            StatusPill(
                text = stringResource(Res.string.tracking_submission_acknowledged),
                icon = Icons.Default.CheckCircle,
                color = DesignTokens.StatusColors.success,
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: ImageVector,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(DesignTokens.IconSize.inline))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun TrackMilesTag() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs),
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(DesignTokens.IconSize.inline),
        )
        Text(
            text = stringResource(Res.string.tracking_track_miles_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun FooterField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Selection mode row + Create Voucher button
// ─────────────────────────────────────────────────────────────────────────────

/** "Selected N" with a "Clear selection" action, shown above the submission list in selection mode. */
@Composable
fun SubmissionSelectionRow(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.tracking_submission_selected_count, selectedCount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(
            shape = DesignTokens.Shape.button,
            onClick = onClearSelection,
        ) {
            Text(stringResource(Res.string.tracking_submission_clear_selection))
        }
    }
}

/** Full-width "+ Create Voucher (N)" call to action for the selected unclaimed submissions. */
@Composable
fun CreateVoucherButton(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = DesignTokens.Shape.roundedSm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.badge))
        Spacer(Modifier.width(DesignTokens.Spacing.s))
        Text(
            stringResource(Res.string.tracking_submission_create_voucher_count, count),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. Empty states
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "No journeys this week" empty state for the Journeys tab when the This-Week filter is empty.
 * Offers a "View All" button so the user can jump to their full history.
 */
@Composable
fun NoJourneysThisWeekState(
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyStateBlock(
        icon = Icons.Default.CalendarToday,
        title = stringResource(Res.string.tracking_saved_no_journeys_week),
        subtitle = stringResource(Res.string.tracking_saved_no_journeys_week_hint),
        modifier = modifier,
    ) {
        OutlinedButton(
            shape = DesignTokens.Shape.button,
            onClick = onViewAll,
        ) {
            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.badge))
            Spacer(Modifier.width(DesignTokens.Spacing.s))
            Text(stringResource(Res.string.tracking_saved_view_all))
        }
    }
}

/** Generic empty state for the Submissions tab (no submissions / no search match). */
@Composable
fun NoSubmissionsState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    EmptyStateBlock(
        icon = Icons.Default.Receipt,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
    )
}

@Composable
private fun EmptyStateBlock(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.xxl, vertical = DesignTokens.Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = DesignTokens.Shape.roundedLg,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ) {
            Box(modifier = Modifier.padding(DesignTokens.Spacing.xl), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Spacer(Modifier.height(DesignTokens.Spacing.l))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(DesignTokens.Spacing.s))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl))
            action()
        }
    }
}
