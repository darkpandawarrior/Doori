package com.mileway.core.ui.components.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocField
import com.mileway.core.ai.model.DocType
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.ai.model.DuplicateVerdict
import com.mileway.core.ai.model.ExtractedValue
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_action_done
import com.mileway.core.ui.resources.media_action_continue_anyway
import com.mileway.core.ui.resources.media_action_ignore
import com.mileway.core.ui.resources.media_action_use_data
import com.mileway.core.ui.resources.media_ocr_badge_ai_detected
import com.mileway.core.ui.resources.media_ocr_badge_detected
import com.mileway.core.ui.resources.media_ocr_batch_results_title
import com.mileway.core.ui.resources.media_ocr_batch_status_duplicate
import com.mileway.core.ui.resources.media_ocr_batch_status_failed
import com.mileway.core.ui.resources.media_ocr_batch_status_success
import com.mileway.core.ui.resources.media_ocr_confirmation_duplicate_body
import com.mileway.core.ui.resources.media_ocr_confirmation_duplicate_confirmed_body
import com.mileway.core.ui.resources.media_ocr_confirmation_duplicate_confirmed_title
import com.mileway.core.ui.resources.media_ocr_confirmation_duplicate_title
import com.mileway.core.ui.resources.media_ocr_confirmation_not_receipt_body
import com.mileway.core.ui.resources.media_ocr_confirmation_not_receipt_title
import com.mileway.core.ui.resources.media_ocr_field_category
import com.mileway.core.ui.resources.media_ocr_field_currency
import com.mileway.core.ui.resources.media_ocr_field_date
import com.mileway.core.ui.resources.media_ocr_field_invoice_no
import com.mileway.core.ui.resources.media_ocr_field_merchant
import com.mileway.core.ui.resources.media_ocr_field_odometer
import com.mileway.core.ui.resources.media_ocr_field_tax
import com.mileway.core.ui.resources.media_ocr_field_total
import com.mileway.core.ui.resources.media_ocr_review_title
import com.mileway.core.ui.theme.DesignTokens
import org.jetbrains.compose.resources.stringResource

/*
 * Generalizes `feature:media`'s odometer-only `OcrResultBottomSheet` into three sheets driven by
 * `core:ai`'s [DocumentAnalysis] — the one shape every OCR caller (media capture, receipt logging,
 * the V27 expense scanner) produces. Lives in `core:ui` (not `feature:media`) so no feature module
 * has to depend on another feature to show these; `core:media`'s capture flow composes them
 * directly (see `OcrResultHost` below and `rememberMediaCaptureLauncher`'s Android actual).
 */

/** Status of one file in a multi-capture OCR batch, shown by [OcrBatchResultsSheet]. */
enum class BatchOcrStatus { Success, Duplicate, Failed }

/** One row of [OcrBatchResultsSheet] — a file label plus its OCR/dedup outcome. */
data class BatchOcrItem(val label: String, val status: BatchOcrStatus)

/**
 * Duplicate / not-a-receipt confirmation. Shown instead of [OcrReviewSheet] when
 * [analysis]'s [DocumentAnalysis.docType] doesn't match what the caller expected, or
 * [DocumentAnalysis.duplicate] is [DuplicateVerdict.Possible]/[DuplicateVerdict.Confirmed].
 */
@Composable
fun OcrConfirmationSheet(
    analysis: DocumentAnalysis,
    expectedDocType: DocType,
    onContinueAnyway: () -> Unit,
    onRetake: () -> Unit,
) {
    // Cross-module properties (DocumentAnalysis/DuplicateVerdict live in core:ai) can't be smart-cast
    // from a chained `analysis.duplicate is X` check — bind to a local val first.
    val duplicate = analysis.duplicate
    val (title, body, tone) =
        when {
            analysis.docType != expectedDocType ->
                Triple(
                    stringResource(Res.string.media_ocr_confirmation_not_receipt_title),
                    stringResource(Res.string.media_ocr_confirmation_not_receipt_body),
                    ActionConfirmationToneType.Warning,
                )
            duplicate is DuplicateVerdict.Confirmed ->
                Triple(
                    stringResource(Res.string.media_ocr_confirmation_duplicate_confirmed_title),
                    stringResource(Res.string.media_ocr_confirmation_duplicate_confirmed_body, duplicate.ref),
                    ActionConfirmationToneType.Danger,
                )
            duplicate is DuplicateVerdict.Possible ->
                Triple(
                    stringResource(Res.string.media_ocr_confirmation_duplicate_title),
                    stringResource(
                        Res.string.media_ocr_confirmation_duplicate_body,
                        duplicate.ref,
                        duplicate.reason,
                    ),
                    ActionConfirmationToneType.Warning,
                )
            // Unique + matching docType: nothing to confirm — OcrResultHost never calls this branch.
            else -> return
        }

    ActionConfirmationBottomSheet(
        title = title,
        description = body,
        confirmLabel = stringResource(Res.string.media_action_continue_anyway),
        dismissLabel = stringResource(Res.string.media_action_ignore),
        tone = tone,
        onConfirm = { onContinueAnyway() },
        onDismiss = onRetake,
    )
}

/**
 * Editable receipt-field review, built from [DocumentAnalysis.fields]. Each row shows an
 * AI-detected/detected badge sourced from that field's own [ExtractedValue.source] (part of
 * [DocumentAnalysis.contributingSources]) — [AnalyzerSource.ON_DEVICE_AI] reads "AI-detected",
 * [AnalyzerSource.TEXT_RECOGNITION]/[AnalyzerSource.HEURISTIC_CLASSIFIER] read "detected".
 *
 * @param onUseData receives the (possibly user-edited) field values, keyed by [DocField].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewSheet(
    fields: Map<DocField, ExtractedValue>,
    onUseData: (Map<DocField, String>) -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val edited = remember(fields) { mutableStateMapOf<DocField, String>().apply { fields.forEach { (k, v) -> put(k, v.value) } } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = DesignTokens.Shape.sheet,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl, vertical = DesignTokens.Spacing.l),
        ) {
            Text(
                text = stringResource(Res.string.media_ocr_review_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            fields.keys.sortedBy { it.ordinal }.forEach { field ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = edited[field].orEmpty(),
                        onValueChange = { edited[field] = it },
                        label = { Text(field.displayLabel()) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(DesignTokens.Spacing.s))
                    SourceBadge(fields.getValue(field).source)
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m),
            ) {
                OutlinedButton(onClick = onIgnore, shape = DesignTokens.Shape.button, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.media_action_ignore))
                }
                Button(
                    onClick = { onUseData(edited.toMap()) },
                    shape = DesignTokens.Shape.button,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.media_action_use_data))
                }
            }
        }
    }
}

/** Multi-file capture: one status row per file (success / duplicate / failed), then a Done button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrBatchResultsSheet(
    items: List<BatchOcrItem>,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDone,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = DesignTokens.Shape.sheet,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = DesignTokens.Spacing.xl, vertical = DesignTokens.Spacing.l),
        ) {
            Text(
                text = stringResource(Res.string.media_ocr_batch_results_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(DesignTokens.Spacing.l))

            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEach { item -> BatchResultRow(item) }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.l))
            Button(onClick = onDone, shape = DesignTokens.Shape.button, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.core_action_done))
            }
        }
    }
}

@Composable
private fun BatchResultRow(item: BatchOcrItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = DesignTokens.Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        BatchStatusBadge(item.status)
    }
}

@Composable
private fun BatchStatusBadge(status: BatchOcrStatus) {
    val (label, color, icon) =
        when (status) {
            BatchOcrStatus.Success ->
                Triple(stringResource(Res.string.media_ocr_batch_status_success), DesignTokens.StatusColors.success, Icons.Default.CheckCircle)
            BatchOcrStatus.Duplicate ->
                Triple(stringResource(Res.string.media_ocr_batch_status_duplicate), DesignTokens.StatusColors.warning, Icons.Default.FileCopy)
            BatchOcrStatus.Failed ->
                Triple(stringResource(Res.string.media_ocr_batch_status_failed), DesignTokens.StatusColors.error, Icons.Default.Error)
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = DesignTokens.Spacing.m, vertical = DesignTokens.Spacing.xs),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(DesignTokens.IconSize.inline))
        Spacer(Modifier.width(DesignTokens.Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

/** AI-detected vs detected badge — driven by one field's own [ExtractedValue.source]. */
@Composable
private fun SourceBadge(source: AnalyzerSource) {
    val isAi = source == AnalyzerSource.ON_DEVICE_AI
    val label = stringResource(if (isAi) Res.string.media_ocr_badge_ai_detected else Res.string.media_ocr_badge_detected)
    val color = if (isAi) DesignTokens.StatusColors.info else DesignTokens.StatusColors.neutral
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier =
            Modifier
                .clip(DesignTokens.Shape.chip)
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = DesignTokens.Spacing.s, vertical = DesignTokens.Spacing.xs),
    )
}

@Composable
private fun DocField.displayLabel(): String =
    stringResource(
        when (this) {
            DocField.MERCHANT -> Res.string.media_ocr_field_merchant
            DocField.TOTAL -> Res.string.media_ocr_field_total
            DocField.TAX -> Res.string.media_ocr_field_tax
            DocField.DATE -> Res.string.media_ocr_field_date
            DocField.INVOICE_NO -> Res.string.media_ocr_field_invoice_no
            DocField.ODOMETER -> Res.string.media_ocr_field_odometer
            DocField.CATEGORY -> Res.string.media_ocr_field_category
            DocField.CURRENCY -> Res.string.media_ocr_field_currency
        },
    )

/**
 * Single-item OCR result state machine + host: the one entry point `core:media`'s capture flow
 * (and any future caller) drives after a [DocumentAnalysis] comes back. Picks
 * [OcrConfirmationSheet] for a duplicate/wrong-doc-type verdict, [OcrReviewSheet] otherwise.
 * Multi-file batches use [OcrBatchResultsSheet] directly instead (its own aggregated status list
 * has no single [DocumentAnalysis] to key off).
 */
@Composable
fun OcrResultHost(
    analysis: DocumentAnalysis?,
    expectedDocType: DocType,
    onUseData: (Map<DocField, String>) -> Unit,
    onIgnore: () -> Unit,
    onContinueAnyway: () -> Unit,
    onRetake: () -> Unit,
) {
    if (analysis == null) return
    val needsConfirmation = analysis.docType != expectedDocType || analysis.duplicate != DuplicateVerdict.Unique
    if (needsConfirmation) {
        OcrConfirmationSheet(
            analysis = analysis,
            expectedDocType = expectedDocType,
            onContinueAnyway = onContinueAnyway,
            onRetake = onRetake,
        )
    } else {
        OcrReviewSheet(
            fields = analysis.fields,
            onUseData = onUseData,
            onIgnore = onIgnore,
            onDismiss = onRetake,
        )
    }
}
