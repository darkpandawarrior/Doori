package com.mileway.core.forms

/** A form field's key, used to address it in a values/errors/computed map. */
typealias FieldId = String

/**
 * One field definition in a dynamic form schema — the full `Form.java`-shaped field set (V27
 * needs every one of these to reach 16-type parity), kept as pure data with no rendering.
 *
 * Visibility, validation and auto-calc conventions built on top of these fields (see
 * [visibleFields], [validationErrors], [computedFields]) are documented next to each field they
 * drive:
 * - [dependentFieldKey] / [dependentExpectedValue]: comma-separated segments AND together across
 *   fields; within one segment, `|`-separated alternatives OR together. E.g.
 *   `dependentFieldKey = "country,city"`, `dependentExpectedValue = "IN,MUM|DEL"` means: visible
 *   when `country == "IN"` AND `city` is `MUM` or `DEL`.
 * - [relatedFieldKey] / [relationType]: either a numeric comparison
 *   ([RelationType.EQUALS]/[RelationType.NOT_EQUALS]/[RelationType.GREATER_THAN]/
 *   [RelationType.LESS_THAN]/[RelationType.GREATER_OR_EQUAL]/[RelationType.LESS_OR_EQUAL] against
 *   [relatedFieldKey]'s numeric value), or a GST auto-calc role
 *   ([RelationType.GST_RATE]/[RelationType.GST_TOTAL], see [computedFields]).
 * - GST_RATE fields store their percentage rate in [defaultValue] (e.g. `"18"` for 18%) since
 *   [min]/[max] stay reserved for plain numeric-bounds validation.
 */
data class MockFormSchema(
    val id: String,
    val fieldKey: FieldId,
    val label: String,
    val type: FormFieldType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
    val dependentFieldKey: String? = null,
    val dependentExpectedValue: String? = null,
    val rank: Int = 0,
    val editable: Boolean = true,
    val defaultValue: String? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val relatedFieldKey: FieldId? = null,
    val relationType: String? = null,
    val autoFill: Boolean = false,
    val masterType: String? = null,
    val declarationId: String? = null,
)

/** String constants for [MockFormSchema.relationType] — kept as strings to match the schema shape. */
object RelationType {
    const val EQUALS = "EQUALS"
    const val NOT_EQUALS = "NOT_EQUALS"
    const val GREATER_THAN = "GREATER_THAN"
    const val LESS_THAN = "LESS_THAN"
    const val GREATER_OR_EQUAL = "GREATER_OR_EQUAL"
    const val LESS_OR_EQUAL = "LESS_OR_EQUAL"

    /** [MockFormSchema.relatedFieldKey] points at the GST base amount field. */
    const val GST_RATE = "GST_RATE"

    /** [MockFormSchema.relatedFieldKey] points at the GST base amount field. */
    const val GST_TOTAL = "GST_TOTAL"
}
