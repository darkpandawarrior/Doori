package com.mileway.core.forms

import com.mileway.core.common.UiText
import kotlin.math.round

private val COMPARISON_RELATIONS =
    setOf(
        RelationType.EQUALS,
        RelationType.NOT_EQUALS,
        RelationType.GREATER_THAN,
        RelationType.LESS_THAN,
        RelationType.GREATER_OR_EQUAL,
        RelationType.LESS_OR_EQUAL,
    )

/**
 * Dependent-field filter — see [MockFormSchema]'s KDoc for the AND (across comma-separated
 * `dependentFieldKey` segments) / OR (within `|`-separated `dependentExpectedValue` alternatives)
 * composition rule. A field with no [MockFormSchema.dependentFieldKey] is always visible.
 */
fun visibleFields(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
): List<MockFormSchema> = schema.filter { isVisible(it, values) }

private fun isVisible(
    field: MockFormSchema,
    values: Map<FieldId, FormFieldValue>,
): Boolean {
    val keys = field.dependentFieldKey?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return true
    if (keys.isEmpty()) return true
    val expectedGroups = field.dependentExpectedValue?.split(",")?.map { it.trim() } ?: return false
    return keys.indices.all { i ->
        val expected = expectedGroups.getOrNull(i) ?: return@all false
        val alternatives = expected.split("|").map { it.trim() }
        stringValueOf(values[keys[i]]) in alternatives
    }
}

/**
 * Full local `validateForm()` rule set: required, maxLength, numeric min/max, declaration
 * acceptance, cross-field numeric relations and GST-consistency. Only fields currently visible
 * (per [visibleFields]) are validated — a hidden field can't block submission.
 */
fun validationErrors(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
): Map<FieldId, UiText> {
    val errors = linkedMapOf<FieldId, UiText>()
    for (field in visibleFields(schema, values)) {
        val error = fieldError(field, values[field.fieldKey], values)
        if (error != null) errors[field.fieldKey] = error
    }
    errors.putAll(gstConsistencyErrors(schema, values))
    return errors
}

private fun fieldError(
    field: MockFormSchema,
    value: FormFieldValue?,
    allValues: Map<FieldId, FormFieldValue>,
): UiText? {
    if (field.required && isFieldValueBlank(value)) {
        return UiText.of("${field.label} is required")
    }
    if (value == null) return null

    when (value) {
        is FormFieldValue.Text ->
            field.maxLength?.let { limit ->
                if (value.value.length > limit) {
                    return UiText.of("${field.label} must be at most $limit characters")
                }
            }
        is FormFieldValue.Number -> value.value?.let { boundsError(field, it)?.let { e -> return e } }
        is FormFieldValue.Currency -> value.amount?.let { boundsError(field, it)?.let { e -> return e } }
        is FormFieldValue.Declaration ->
            if (field.required && !value.accepted) {
                return UiText.of("${field.label} must be accepted")
            }
        else -> Unit
    }

    return relationError(field, value, allValues)
}

private fun boundsError(
    field: MockFormSchema,
    number: Double,
): UiText? {
    field.min?.let { if (number < it) return UiText.of("${field.label} must be at least $it") }
    field.max?.let { if (number > it) return UiText.of("${field.label} must be at most $it") }
    return null
}

private fun relationError(
    field: MockFormSchema,
    value: FormFieldValue,
    allValues: Map<FieldId, FormFieldValue>,
): UiText? {
    val relation = field.relationType ?: return null
    if (relation !in COMPARISON_RELATIONS) return null // GST_RATE/GST_TOTAL are computed, not validated here.
    val relatedKey = field.relatedFieldKey ?: return null
    val current = numericValueOf(value) ?: return null
    val related = numericValueOf(allValues[relatedKey]) ?: return null
    val holds =
        when (relation) {
            RelationType.EQUALS -> current == related
            RelationType.NOT_EQUALS -> current != related
            RelationType.GREATER_THAN -> current > related
            RelationType.LESS_THAN -> current < related
            RelationType.GREATER_OR_EQUAL -> current >= related
            RelationType.LESS_OR_EQUAL -> current <= related
            else -> true
        }
    return if (holds) null else UiText.of("${field.label} must be $relation ${field.relatedFieldKey}")
}

private fun gstConsistencyErrors(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
): Map<FieldId, UiText> {
    val computed = computedFields(schema, values)
    val errors = linkedMapOf<FieldId, UiText>()
    for (field in schema.filter { it.relationType == RelationType.GST_TOTAL }) {
        val expected = numericValueOf(computed[field.fieldKey]) ?: continue
        val actual = numericValueOf(values[field.fieldKey]) ?: continue
        if (!approximatelyEquals(actual, expected)) {
            errors[field.fieldKey] = UiText.of("${field.label} does not match computed GST total")
        }
    }
    return errors
}

/**
 * autoFill propagation (copies a [MockFormSchema.relatedFieldKey]'s current value verbatim into
 * an `autoFill` field) plus GST rate/round-off auto-calc: a [RelationType.GST_RATE] field derives
 * `base * rate / 100` from its related base-amount field (rate taken from
 * [MockFormSchema.defaultValue], see [MockFormSchema]'s KDoc); a [RelationType.GST_TOTAL] field
 * derives `base + tax`, both rounded to 2dp.
 */
fun computedFields(
    schema: List<MockFormSchema>,
    values: Map<FieldId, FormFieldValue>,
): Map<FieldId, FormFieldValue> {
    val computed = linkedMapOf<FieldId, FormFieldValue>()

    for (field in schema) {
        when (field.relationType) {
            RelationType.GST_RATE -> {
                val baseKey = field.relatedFieldKey ?: continue
                val base = numericValueOf(values[baseKey]) ?: continue
                val rate = field.defaultValue?.toDoubleOrNull() ?: continue
                computed[field.fieldKey] = FormFieldValue.Number(round2(base * rate / 100.0))
            }
            RelationType.GST_TOTAL -> {
                val baseKey = field.relatedFieldKey ?: continue
                val base = numericValueOf(values[baseKey]) ?: continue
                val taxField = schema.firstOrNull { it.relationType == RelationType.GST_RATE && it.relatedFieldKey == baseKey }
                val tax = taxField?.let { tf -> numericValueOf(computed[tf.fieldKey]) ?: numericValueOf(values[tf.fieldKey]) } ?: 0.0
                val currencyCode = (values[field.fieldKey] as? FormFieldValue.Currency)?.currencyCode ?: "INR"
                computed[field.fieldKey] = FormFieldValue.Currency(round2(base + tax), currencyCode)
            }
            else -> Unit
        }
    }

    for (field in schema) {
        if (field.autoFill && field.relationType != RelationType.GST_RATE && field.relationType != RelationType.GST_TOTAL) {
            val relatedKey = field.relatedFieldKey ?: continue
            values[relatedKey]?.let { computed[field.fieldKey] = it }
        }
    }

    return computed
}

/**
 * Prefilled starting values for a schema — [MockFormSchema.defaultValue] parsed into the field's
 * [FormFieldValue] subtype (blank/zero/false when absent). The `resetFormValues` path: a ViewModel
 * calls this to build the values map it hands back to the renderer on "Reset".
 */
fun defaultFormValues(schema: List<MockFormSchema>): Map<FieldId, FormFieldValue> = schema.associate { it.fieldKey to defaultValueFor(it) }

private fun defaultValueFor(field: MockFormSchema): FormFieldValue {
    val raw = field.defaultValue
    return when (field.type) {
        FormFieldType.TEXT, FormFieldType.TEXTAREA, FormFieldType.EMAIL -> FormFieldValue.Text(raw.orEmpty())
        FormFieldType.NUMBER -> FormFieldValue.Number(raw?.toDoubleOrNull())
        FormFieldType.CURRENCY -> FormFieldValue.Currency(raw?.toDoubleOrNull(), "INR")
        FormFieldType.SELECT, FormFieldType.CITY_AIRPORT, FormFieldType.IRN, FormFieldType.MASTER, FormFieldType.EMPLOYEE_DEPARTMENT ->
            FormFieldValue.Select(raw)
        FormFieldType.RATING -> FormFieldValue.Rating(raw?.toIntOrNull() ?: 0)
        FormFieldType.DATE -> FormFieldValue.Date(raw)
        FormFieldType.TIME -> FormFieldValue.Time(raw)
        FormFieldType.LOCATION -> FormFieldValue.Location(lat = null, lng = null, label = raw)
        FormFieldType.DECLARATION -> FormFieldValue.Declaration(accepted = false)
        FormFieldType.FILE_PDF -> FormFieldValue.FileRef(emptyList())
    }
}

/** Whether [value] counts as "empty" for a field of its type — reused by [validationErrors]'s
 * required check and by `core.forms.suggestions.fieldSuggestions` (V27 P27.F.5) to only surface an
 * AI suggestion chip on a field the user hasn't already filled in. */
fun isFieldValueBlank(value: FormFieldValue?): Boolean =
    when (value) {
        null -> true
        is FormFieldValue.Text -> value.value.isBlank()
        is FormFieldValue.Number -> value.value == null
        is FormFieldValue.Select -> value.value.isNullOrBlank()
        is FormFieldValue.MultiSelect -> value.values.isEmpty()
        is FormFieldValue.Date -> value.isoValue.isNullOrBlank()
        is FormFieldValue.Time -> value.value.isNullOrBlank()
        is FormFieldValue.FileRef -> value.paths.isEmpty()
        is FormFieldValue.Currency -> value.amount == null
        is FormFieldValue.Rating -> value.value <= 0
        is FormFieldValue.Location -> value.lat == null || value.lng == null
        is FormFieldValue.Declaration -> false // declaration acceptance has its own required rule
    }

private fun numericValueOf(value: FormFieldValue?): Double? =
    when (value) {
        is FormFieldValue.Number -> value.value
        is FormFieldValue.Currency -> value.amount
        else -> null
    }

private fun stringValueOf(value: FormFieldValue?): String? =
    when (value) {
        null -> null
        is FormFieldValue.Text -> value.value
        is FormFieldValue.Select -> value.value
        is FormFieldValue.Number -> value.value?.toString()
        is FormFieldValue.Declaration -> value.accepted.toString()
        is FormFieldValue.Time -> value.value
        is FormFieldValue.Date -> value.isoValue
        else -> null
    }

private fun round2(value: Double): Double = round(value * 100.0) / 100.0

private fun approximatelyEquals(
    a: Double,
    b: Double,
    epsilon: Double = 0.01,
): Boolean = kotlin.math.abs(a - b) <= epsilon
