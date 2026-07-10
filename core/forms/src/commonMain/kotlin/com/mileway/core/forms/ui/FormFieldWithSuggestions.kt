package com.mileway.core.forms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mileway.core.ai.model.AnalyzerSource
import com.mileway.core.ai.model.DocumentAnalysis
import com.mileway.core.forms.FieldId
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormSchema
import com.mileway.core.forms.isFieldValueBlank
import com.mileway.core.forms.suggestions.FieldSuggestion
import com.mileway.core.forms.suggestions.SuggestionConfidence
import com.mileway.core.forms.suggestions.fieldSuggestions
import com.mileway.core.forms.suggestions.highConfidenceSuggestions
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_forms_suggestion_accept
import com.mileway.core.ui.resources.core_forms_suggestion_accept_all
import com.mileway.core.ui.resources.core_forms_suggestion_dismiss_cd
import com.mileway.core.ui.resources.core_forms_suggestion_source_ai
import com.mileway.core.ui.resources.core_forms_suggestion_source_detected
import com.mileway.core.ui.resources.core_forms_suggestion_undo
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val SUGGESTION_DEBOUNCE_MILLIS = 400L

/**
 * ponytail: EXPERIMENTAL — on-device-AI form field suggestions (V27 P27.F.5, consumes core:ai's
 * DocumentIntelligence output). Compile/logic-verified only; on-device inference quality is
 * whatever `MlKitGenAiAnalyzer`/`FoundationModelsAnalyzer` already ship as EXPERIMENTAL. Revisit
 * chip copy/thresholds once real scanned-document suggestions get device QA.
 *
 * Opt-in wrapper around [FormRenderer]: pass a Scanner-originated [DocumentAnalysis] and this
 * renders an "accept all high-confidence" bar plus per-field accept/dismiss chips above the form
 * for every currently-empty field [analysis] has a plausible value for (see
 * `com.mileway.core.forms.suggestions.fieldSuggestions`). Existing call sites that only need
 * [FormRenderer] are unaffected — this wrapper never changes [FormRenderer]'s own behavior, so
 * adopting it is purely opt-in per screen.
 *
 * Degrades to zero chips, no crash, when [analysis] is null or [DocumentAnalysis.fields] is empty
 * (AI unavailable / nothing extracted) — the only contract this wrapper promises.
 */
@Composable
fun FormFieldWithSuggestions(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
    onValueChange: (FieldId, FormFieldValue) -> Unit,
    analysis: DocumentAnalysis?,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
) {
    // Debounced snapshot of `values` the fieldSuggestions() scan runs against, so it doesn't
    // re-run on every keystroke while the user is mid-typing some other field.
    var debouncedValues by remember { mutableStateOf(values) }
    LaunchedEffect(values) {
        delay(SUGGESTION_DEBOUNCE_MILLIS)
        debouncedValues = values
    }

    var dismissedKeys by remember(analysis) { mutableStateOf(emptySet<FieldId>()) }
    var lastDismissed by remember(analysis) { mutableStateOf<FieldSuggestion?>(null) }

    val suggestions =
        remember(schema, debouncedValues, analysis, dismissedKeys, values) {
            analysis
                ?.takeIf { it.fields.isNotEmpty() }
                ?.let { fieldSuggestions(schema, debouncedValues, it) }
                .orEmpty()
                // Re-check against the live (non-debounced) values too, so accepting/editing a
                // field hides its chip immediately instead of waiting out the debounce window.
                .filter { it.fieldKey !in dismissedKeys && isFieldValueBlank(values[it.fieldKey]) }
        }
    val highConfidence = remember(suggestions) { highConfidenceSuggestions(suggestions) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.m)) {
        if (suggestions.isNotEmpty()) {
            SuggestionBar(
                suggestions = suggestions,
                highConfidenceCount = highConfidence.size,
                onAccept = { onValueChange(it.fieldKey, it.value) },
                onDismiss = {
                    dismissedKeys = dismissedKeys + it.fieldKey
                    lastDismissed = it
                },
                onAcceptAllHighConfidence = { highConfidence.forEach { onValueChange(it.fieldKey, it.value) } },
            )
        }
        lastDismissed?.let { dismissed ->
            if (dismissed.fieldKey in dismissedKeys) {
                UndoRow(
                    fieldLabel = dismissed.label,
                    onUndo = {
                        dismissedKeys = dismissedKeys - dismissed.fieldKey
                        lastDismissed = null
                    },
                )
            }
        }
        FormRenderer(schema = schema, values = values, onValueChange = onValueChange, onReset = onReset)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionBar(
    suggestions: List<FieldSuggestion>,
    highConfidenceCount: Int,
    onAccept: (FieldSuggestion) -> Unit,
    onDismiss: (FieldSuggestion) -> Unit,
    onAcceptAllHighConfidence: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.xs)) {
        if (highConfidenceCount > 1) {
            TextButton(onClick = onAcceptAllHighConfidence) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
                Spacer(Modifier.size(DesignTokens.Spacing.xs))
                Text(stringResource(Res.string.core_forms_suggestion_accept_all))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(suggestion = suggestion, onAccept = { onAccept(suggestion) }, onDismiss = { onDismiss(suggestion) })
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: FieldSuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sourceLabel =
        stringResource(
            if (suggestion.source == AnalyzerSource.ON_DEVICE_AI) {
                Res.string.core_forms_suggestion_source_ai
            } else {
                Res.string.core_forms_suggestion_source_detected
            },
        )
    Surface(shape = DesignTokens.Shape.chip, color = suggestionChipColor(suggestion.tier)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.padding(
                    start = DesignTokens.Spacing.m,
                    end = DesignTokens.Spacing.xs,
                    top = DesignTokens.Spacing.xs,
                    bottom = DesignTokens.Spacing.xs,
                ),
        ) {
            Column {
                Text(
                    "${suggestion.label}: ${suggestion.displayValue}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAccept, modifier = Modifier.size(DesignTokens.IconSize.navigation)) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(Res.string.core_forms_suggestion_accept), tint = DesignTokens.StatusColors.success)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(DesignTokens.IconSize.navigation)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.core_forms_suggestion_dismiss_cd, suggestion.label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun UndoRow(
    fieldLabel: String,
    onUndo: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
        Text(fieldLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onUndo) { Text(stringResource(Res.string.core_forms_suggestion_undo)) }
    }
}

@Composable
private fun suggestionChipColor(tier: SuggestionConfidence) =
    when (tier) {
        SuggestionConfidence.HIGH -> DesignTokens.StatusColors.success.copy(alpha = 0.15f)
        SuggestionConfidence.MEDIUM -> MaterialTheme.colorScheme.surfaceVariant
        SuggestionConfidence.LOW -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
