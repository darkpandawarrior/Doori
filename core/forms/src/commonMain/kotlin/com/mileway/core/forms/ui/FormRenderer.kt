package com.mileway.core.forms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.common.asString
import com.mileway.core.forms.FieldId
import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.MockFormCatalog
import com.mileway.core.forms.MockFormSchema
import com.mileway.core.forms.RelationType
import com.mileway.core.forms.computedFields
import com.mileway.core.forms.validationErrors
import com.mileway.core.forms.visibleFields
import com.mileway.core.media.model.CaptureMode
import com.mileway.core.media.model.MediaCaptureConfig
import com.mileway.core.media.model.MediaCaptureResult
import com.mileway.core.media.rememberMediaCaptureLauncher
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.components.pickers.WheelTimePickerDialog
import com.mileway.core.ui.components.sheet.SearchablePickerSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.core_forms_amount_details
import com.mileway.core.ui.resources.core_forms_attach_file
import com.mileway.core.ui.resources.core_forms_attach_file_hint
import com.mileway.core.ui.resources.core_forms_remove_attachment_cd
import com.mileway.core.ui.resources.core_forms_reset
import com.mileway.core.ui.resources.core_forms_select_field
import com.mileway.core.ui.resources.core_forms_star_cd
import com.mileway.core.ui.resources.core_select_date
import com.mileway.core.ui.resources.core_select_time
import com.mileway.core.ui.theme.DesignTokens
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/** GST fields fold into [AmountDetailsCard] instead of rendering inline — see [FormRenderer]'s KDoc. */
private val AMOUNT_ROW_RELATIONS = setOf(RelationType.GST_RATE, RelationType.GST_TOTAL)

private val CURRENCY_CODES = listOf("INR", "USD", "EUR", "GBP")

/**
 * The shared Compose renderer for a dynamic [MockFormSchema] list (V27 P27.F.1/.3/.4) — branches
 * on all 16 [FormFieldType] cases, each against its matching [FormFieldValue] subtype:
 * - TEXT/TEXTAREA/EMAIL -> a text field (multi-line for TEXTAREA, email keyboard for EMAIL).
 * - NUMBER -> a decimal-keyboard text field; CURRENCY -> the same plus a currency-code dropdown.
 * - SELECT -> a small fixed-option dropdown ([MockFormSchema.options]).
 * - RATING -> a 5-star row. DATE/TIME -> a tap row opening core:ui's wheel picker dialogs.
 * - LOCATION -> a label text field (lat/lng capture is a separate, already-built flow).
 * - DECLARATION -> a checkbox with the field's label as its statement text.
 * - CITY_AIRPORT/IRN/MASTER/EMPLOYEE_DEPARTMENT (enterprise, catalog-backed) -> a tap row opening
 *   [SearchablePickerSheet] over [MockFormCatalog.masterData], single- or multi-select depending on
 *   whether the current value is a [FormFieldValue.Select] or [FormFieldValue.MultiSelect].
 * - FILE_PDF -> an "Attach file" button wired through `core:media`'s `rememberMediaCaptureLauncher`
 *   (P27.F.2): [CaptureMode.Gallery] only, the one mode with a real actual on both Android and iOS
 *   (see [AttachmentControl]'s KDoc). Attached items append into the field's [FormFieldValue.FileRef]
 *   paths and each carries its own remove action; the same required-check every other field type
 *   gets applies here too (an empty [FormFieldValue.FileRef] is "blank", see `FormLogic.isBlank`).
 *   A schema-defined single-purpose attachment (e.g. "Toll receipt") is just its own FILE_PDF
 *   field/fieldKey, independent of any general receipts-bucket field elsewhere in the schema.
 *
 * Only [visibleFields] render (reactive: recomposes as [values] changes), and
 * [MockFormSchema.editable] == false disables the control. GST/autoFill fields ([computedFields])
 * override the field's displayed value; GST_RATE/GST_TOTAL fields specifically are pulled out of
 * the main list into the read-only "Amount Details" row. [onReset], when non-null, renders a
 * "Reset" action — wire it to `com.mileway.core.forms.defaultFormValues(schema)`.
 */
@Composable
fun FormRenderer(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
    onValueChange: (FieldId, FormFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    onReset: (() -> Unit)? = null,
) {
    val computed = computedFields(schema, values)
    val errors = validationErrors(schema, values)
    val amountRows = schema.filter { it.relationType in AMOUNT_ROW_RELATIONS }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l)) {
        for (field in visibleFields(schema, values)) {
            if (field.relationType in AMOUNT_ROW_RELATIONS) continue
            FormFieldRow(
                field = field,
                value = computed[field.fieldKey] ?: values[field.fieldKey],
                error = errors[field.fieldKey]?.asString(),
                onValueChange = { onValueChange(field.fieldKey, it) },
            )
        }
        if (amountRows.isNotEmpty()) {
            AmountDetailsCard(amountFields = amountRows, computed = computed, values = values)
        }
        if (onReset != null) {
            TextButton(onClick = onReset) { Text(stringResource(Res.string.core_forms_reset)) }
        }
    }
}

@Composable
private fun FormFieldRow(
    field: MockFormSchema,
    value: FormFieldValue?,
    error: String?,
    onValueChange: (FormFieldValue) -> Unit,
) {
    val enabled = field.editable
    Column(modifier = Modifier.fillMaxWidth()) {
        // The declaration checkbox already carries the label as its statement text.
        if (field.type != FormFieldType.DECLARATION) {
            FieldLabel(field.label, field.required)
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
        }
        when (field.type) {
            FormFieldType.TEXT -> TextControl(value as? FormFieldValue.Text, enabled, KeyboardType.Text, singleLine = true, onValueChange)
            FormFieldType.TEXTAREA -> TextControl(value as? FormFieldValue.Text, enabled, KeyboardType.Text, singleLine = false, onValueChange)
            FormFieldType.EMAIL -> TextControl(value as? FormFieldValue.Text, enabled, KeyboardType.Email, singleLine = true, onValueChange)
            FormFieldType.NUMBER -> NumberControl(value as? FormFieldValue.Number, enabled, onValueChange)
            FormFieldType.CURRENCY -> CurrencyControl(value as? FormFieldValue.Currency, enabled, onValueChange)
            FormFieldType.SELECT -> SelectControl(field, value as? FormFieldValue.Select, enabled, onValueChange)
            FormFieldType.RATING -> RatingControl(value as? FormFieldValue.Rating, enabled, onValueChange)
            FormFieldType.DATE -> DateControl(value as? FormFieldValue.Date, enabled, onValueChange)
            FormFieldType.TIME -> TimeControl(value as? FormFieldValue.Time, enabled, onValueChange)
            FormFieldType.LOCATION -> LocationControl(value as? FormFieldValue.Location, enabled, onValueChange)
            FormFieldType.DECLARATION -> DeclarationControl(field, value as? FormFieldValue.Declaration, enabled, onValueChange)
            FormFieldType.CITY_AIRPORT, FormFieldType.IRN, FormFieldType.EMPLOYEE_DEPARTMENT, FormFieldType.MASTER ->
                EnterpriseControl(field, value, enabled, onValueChange)
            FormFieldType.FILE_PDF -> AttachmentControl(value as? FormFieldValue.FileRef, enabled, onValueChange)
        }
        if (error != null) FieldError(error)
    }
}

@Composable
private fun FieldLabel(
    label: String,
    required: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        if (required) {
            Text("*", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
    )
}

@Composable
private fun TextControl(
    value: FormFieldValue.Text?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    singleLine: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    OutlinedTextField(
        value = value?.value.orEmpty(),
        onValueChange = { onValueChange(FormFieldValue.Text(it)) },
        enabled = enabled,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberControl(
    value: FormFieldValue.Number?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    OutlinedTextField(
        // ponytail: round-tripping through toDoubleOrNull loses an in-progress trailing "." on
        // every keystroke; acceptable for a first-pass renderer, revisit with a text-buffer state
        // if that UX rough edge shows up in Wave 2 QA.
        value = value?.value?.toDisplayString().orEmpty(),
        onValueChange = { onValueChange(FormFieldValue.Number(it.toDoubleOrNull())) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyControl(
    value: FormFieldValue.Currency?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    var codeExpanded by remember { mutableStateOf(false) }
    val code = value?.currencyCode ?: "INR"
    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value?.amount?.toDisplayString().orEmpty(),
            onValueChange = { onValueChange(FormFieldValue.Currency(it.toDoubleOrNull(), code)) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        ExposedDropdownMenuBox(
            expanded = codeExpanded,
            onExpandedChange = { if (enabled) codeExpanded = it },
            modifier = Modifier.weight(0.6f),
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(codeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = codeExpanded, onDismissRequest = { codeExpanded = false }) {
                CURRENCY_CODES.forEach { currencyCode ->
                    DropdownMenuItem(
                        text = { Text(currencyCode) },
                        onClick = {
                            onValueChange(FormFieldValue.Currency(value?.amount, currencyCode))
                            codeExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectControl(
    field: MockFormSchema,
    value: FormFieldValue.Select?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = value?.value.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            placeholder = { Text(stringResource(Res.string.core_forms_select_field, field.label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(FormFieldValue.Select(option))
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun RatingControl(
    value: FormFieldValue.Rating?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    val rating = value?.value ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            IconButton(onClick = { onValueChange(FormFieldValue.Rating(star)) }, enabled = enabled) {
                Icon(
                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(Res.string.core_forms_star_cd, star),
                    tint = if (star <= rating) DesignTokens.StatusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DateControl(
    value: FormFieldValue.Date?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    TapRow(
        text = value?.isoValue.takeUnless { it.isNullOrBlank() } ?: stringResource(Res.string.core_select_date),
        isPlaceholder = value?.isoValue.isNullOrBlank(),
        icon = Icons.Filled.CalendarMonth,
        enabled = enabled,
        onClick = { showPicker = true },
    )
    if (showPicker) {
        WheelDatePickerDialog(
            initialDateMillis = isoDateToMillis(value?.isoValue),
            onConfirm = { millis ->
                onValueChange(FormFieldValue.Date(millisToIsoDate(millis)))
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun TimeControl(
    value: FormFieldValue.Time?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    TapRow(
        text = value?.value.takeUnless { it.isNullOrBlank() } ?: stringResource(Res.string.core_select_time),
        isPlaceholder = value?.value.isNullOrBlank(),
        icon = Icons.Filled.Schedule,
        enabled = enabled,
        onClick = { showPicker = true },
    )
    if (showPicker) {
        WheelTimePickerDialog(
            initialMinutes = timeStringToMinutes(value?.value),
            onConfirm = { hour, minute ->
                onValueChange(FormFieldValue.Time(minutesToTimeString(hour, minute)))
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun LocationControl(
    value: FormFieldValue.Location?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    // A label-only field per V27 P27.F.1 scope; lat/lng capture is the existing location-search flow.
    OutlinedTextField(
        value = value?.label.orEmpty(),
        onValueChange = { onValueChange(FormFieldValue.Location(value?.lat, value?.lng, it)) },
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DeclarationControl(
    field: MockFormSchema,
    value: FormFieldValue.Declaration?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = value?.accepted ?: false, onCheckedChange = { onValueChange(FormFieldValue.Declaration(it)) }, enabled = enabled)
        Text(field.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * CITY_AIRPORT/IRN/MASTER/EMPLOYEE_DEPARTMENT — catalog-backed lookups via
 * [MockFormCatalog.masterData], keyed on [MockFormSchema.masterType] (falls back to
 * [MockFormSchema.options] when set). Single- vs multi-select is read off the current value's
 * runtime type, since [FormFieldType.MASTER] collapses both shapes (see [FormFieldType]'s KDoc).
 */
@Composable
private fun EnterpriseControl(
    field: MockFormSchema,
    value: FormFieldValue?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val options = field.options.ifEmpty { MockFormCatalog.masterData[field.masterType].orEmpty() }
    val isMulti = value is FormFieldValue.MultiSelect
    val display =
        when (value) {
            is FormFieldValue.MultiSelect -> value.values.joinToString(", ")
            is FormFieldValue.Select -> value.value.orEmpty()
            else -> ""
        }

    TapRow(
        text = display.ifBlank { stringResource(Res.string.core_forms_select_field, field.label) },
        isPlaceholder = display.isBlank(),
        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        enabled = enabled,
        onClick = { showSheet = true },
    )

    if (showSheet) {
        SearchablePickerSheet(
            title = field.label,
            items = options,
            filter = { item, query -> item.contains(query, ignoreCase = true) },
            onDismiss = { showSheet = false },
        ) { filtered, _ ->
            filtered.forEach { option ->
                val selected =
                    when (value) {
                        is FormFieldValue.MultiSelect -> option in value.values
                        is FormFieldValue.Select -> option == value.value
                        else -> false
                    }
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable {
                                if (isMulti) {
                                    val current = value.values
                                    onValueChange(FormFieldValue.MultiSelect(if (selected) current - option else current + option))
                                } else {
                                    onValueChange(FormFieldValue.Select(option))
                                    showSheet = false
                                }
                            }
                            .padding(vertical = DesignTokens.Spacing.s),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option, color = MaterialTheme.colorScheme.onSurface)
                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * "Attach file" button + a wrap-row of currently attached items, each removable (P27.F.2).
 *
 * ponytail: [CaptureMode.Gallery] only — the one `core:media` capture mode with a real actual on
 * both Android and iOS (`ReceiptAttachmentLauncher` in feature:logging uses the same mode for the
 * same reason). Camera needs `feature:media`'s full-screen `CameraCaptureScreen`, which core:forms
 * can't reach without a feature dependency; Files/Document both throw "not wired" on iOS today
 * (see `MediaCaptureLauncher.ios.kt`). Add a per-field capture-mode config to [MockFormSchema] if a
 * schema ever needs Camera/Files/Document specifically.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttachmentControl(
    value: FormFieldValue.FileRef?,
    enabled: Boolean,
    onValueChange: (FormFieldValue) -> Unit,
) {
    val paths = value?.paths.orEmpty()
    val launchPicker =
        rememberMediaCaptureLauncher(
            config = MediaCaptureConfig(allowedModes = setOf(CaptureMode.Gallery), multiple = true, maxCount = 5),
            onResult = { result ->
                if (result is MediaCaptureResult.Attachments) {
                    onValueChange(FormFieldValue.FileRef(paths + result.items.map { it.uri }))
                }
            },
        )
    Column {
        OutlinedButton(onClick = launchPicker, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(DesignTokens.IconSize.inline))
            Spacer(Modifier.size(DesignTokens.Spacing.xs))
            Text(stringResource(Res.string.core_forms_attach_file))
        }
        if (paths.isEmpty()) {
            Text(
                stringResource(Res.string.core_forms_attach_file_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xs),
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
                modifier = Modifier.padding(top = DesignTokens.Spacing.s),
            ) {
                paths.forEach { path ->
                    AttachmentChip(
                        path = path,
                        enabled = enabled,
                        onRemove = { onValueChange(FormFieldValue.FileRef(paths - path)) },
                    )
                }
            }
        }
    }
}

/** One removable chip for [AttachmentControl] naming the attachment by its last path segment. */
@Composable
private fun AttachmentChip(
    path: String,
    enabled: Boolean,
    onRemove: () -> Unit,
) {
    val name = path.substringAfterLast('/')
    Surface(shape = DesignTokens.Shape.chip, color = MaterialTheme.colorScheme.surfaceVariant) {
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
            Text(name.take(24), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (enabled) {
                IconButton(onClick = onRemove, modifier = Modifier.size(DesignTokens.IconSize.navigation)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.core_forms_remove_attachment_cd, name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The read-only "Amount Details" row P27.F.4 asks for: live GST rate/total, sorted by rank. */
@Composable
private fun AmountDetailsCard(
    amountFields: List<MockFormSchema>,
    computed: Map<FieldId, FormFieldValue>,
    values: Map<FieldId, FormFieldValue>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s),
        ) {
            Text(stringResource(Res.string.core_forms_amount_details), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            for (field in amountFields.sortedBy { it.rank }) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(field.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatAmountValue(computed[field.fieldKey] ?: values[field.fieldKey]), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TapRow(
    text: String,
    isPlaceholder: Boolean,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.Shape.roundedMd,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.l, vertical = DesignTokens.Spacing.m),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, color = if (isPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(DesignTokens.IconSize.navigation))
        }
    }
}

private fun formatAmountValue(value: FormFieldValue?): String =
    when (value) {
        is FormFieldValue.Number -> value.value?.toDisplayString() ?: "—"
        is FormFieldValue.Currency -> "${value.currencyCode} ${value.amount?.toDisplayString() ?: "—"}"
        else -> "—"
    }

private fun Double.toDisplayString(): String = if (this == kotlin.math.floor(this) && !this.isInfinite()) this.toLong().toString() else this.toString()

private fun isoDateToMillis(iso: String?): Long? =
    iso?.let { runCatching { LocalDate.parse(it).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds() }.getOrNull() }

private fun millisToIsoDate(millis: Long): String = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private fun timeStringToMinutes(value: String?): Int {
    val parts = value?.split(":") ?: return 9 * 60
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour * 60 + minute
}

private fun minutesToTimeString(
    hour: Int,
    minute: Int,
): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
