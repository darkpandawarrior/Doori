package com.mileway.core.forms

import com.mileway.core.common.asString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun textField(
    key: String = "name",
    required: Boolean = false,
    maxLength: Int? = null,
    dependentFieldKey: String? = null,
    dependentExpectedValue: String? = null,
) = MockFormSchema(
    id = key,
    fieldKey = key,
    label = key.replaceFirstChar { it.uppercase() },
    type = FormFieldType.TEXT,
    required = required,
    maxLength = maxLength,
    dependentFieldKey = dependentFieldKey,
    dependentExpectedValue = dependentExpectedValue,
)

private fun numberField(
    key: String,
    required: Boolean = false,
    min: Double? = null,
    max: Double? = null,
    relatedFieldKey: String? = null,
    relationType: String? = null,
    defaultValue: String? = null,
    autoFill: Boolean = false,
) = MockFormSchema(
    id = key,
    fieldKey = key,
    label = key.replaceFirstChar { it.uppercase() },
    type = FormFieldType.NUMBER,
    required = required,
    min = min,
    max = max,
    relatedFieldKey = relatedFieldKey,
    relationType = relationType,
    defaultValue = defaultValue,
    autoFill = autoFill,
)

class FormLogicTest {
    // ---- validationErrors: required ----

    @Test
    fun required_field_missing_produces_error() {
        val schema = listOf(textField("name", required = true))

        val errors = validationErrors(schema, emptyMap())

        assertTrue("name" in errors)
        assertEquals("Name is required", errors.getValue("name").asString())
    }

    @Test
    fun required_field_present_has_no_error() {
        val schema = listOf(textField("name", required = true))
        val values = mapOf("name" to FormFieldValue.Text("Sid"))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- validationErrors: maxLength ----

    @Test
    fun text_over_max_length_produces_error() {
        val schema = listOf(textField("notes", maxLength = 5))
        val values = mapOf("notes" to FormFieldValue.Text("way too long"))

        val errors = validationErrors(schema, values)

        assertTrue("notes" in errors)
    }

    @Test
    fun text_within_max_length_has_no_error() {
        val schema = listOf(textField("notes", maxLength = 5))
        val values = mapOf("notes" to FormFieldValue.Text("ok"))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- validationErrors: min/max bounds ----

    @Test
    fun number_below_min_produces_error() {
        val schema = listOf(numberField("amount", min = 0.0, max = 100.0))
        val values = mapOf("amount" to FormFieldValue.Number(-5.0))

        assertTrue("amount" in validationErrors(schema, values))
    }

    @Test
    fun number_above_max_produces_error() {
        val schema = listOf(numberField("amount", min = 0.0, max = 100.0))
        val values = mapOf("amount" to FormFieldValue.Number(150.0))

        assertTrue("amount" in validationErrors(schema, values))
    }

    @Test
    fun number_within_bounds_has_no_error() {
        val schema = listOf(numberField("amount", min = 0.0, max = 100.0))
        val values = mapOf("amount" to FormFieldValue.Number(50.0))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- validationErrors: declaration acceptance ----

    @Test
    fun declaration_not_accepted_produces_error() {
        val schema =
            listOf(
                MockFormSchema(
                    id = "d1",
                    fieldKey = "declaration",
                    label = "Declaration",
                    type = FormFieldType.DECLARATION,
                    required = true,
                ),
            )
        val values = mapOf("declaration" to FormFieldValue.Declaration(accepted = false))

        assertTrue("declaration" in validationErrors(schema, values))
    }

    @Test
    fun declaration_accepted_has_no_error() {
        val schema =
            listOf(
                MockFormSchema(
                    id = "d1",
                    fieldKey = "declaration",
                    label = "Declaration",
                    type = FormFieldType.DECLARATION,
                    required = true,
                ),
            )
        val values = mapOf("declaration" to FormFieldValue.Declaration(accepted = true))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- validationErrors: cross-field relation ----

    @Test
    fun cross_field_relation_failing_produces_error() {
        val schema =
            listOf(
                numberField("budget"),
                numberField("amount", relatedFieldKey = "budget", relationType = RelationType.LESS_OR_EQUAL),
            )
        val values = mapOf("budget" to FormFieldValue.Number(100.0), "amount" to FormFieldValue.Number(150.0))

        assertTrue("amount" in validationErrors(schema, values))
    }

    @Test
    fun cross_field_relation_passing_has_no_error() {
        val schema =
            listOf(
                numberField("budget"),
                numberField("amount", relatedFieldKey = "budget", relationType = RelationType.LESS_OR_EQUAL),
            )
        val values = mapOf("budget" to FormFieldValue.Number(100.0), "amount" to FormFieldValue.Number(50.0))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- validationErrors: GST-consistency ----

    private val gstSchema =
        listOf(
            numberField("baseAmount"),
            numberField(
                "gstAmount",
                relatedFieldKey = "baseAmount",
                relationType = RelationType.GST_RATE,
                defaultValue = "18",
            ),
            MockFormSchema(
                id = "totalAmount",
                fieldKey = "totalAmount",
                label = "Total",
                type = FormFieldType.CURRENCY,
                relatedFieldKey = "baseAmount",
                relationType = RelationType.GST_TOTAL,
            ),
        )

    @Test
    fun gst_consistent_total_has_no_error() {
        val values =
            mapOf(
                "baseAmount" to FormFieldValue.Number(100.0),
                "totalAmount" to FormFieldValue.Currency(118.0, "INR"),
            )

        assertTrue(validationErrors(gstSchema, values).isEmpty())
    }

    @Test
    fun gst_inconsistent_total_produces_error() {
        val values =
            mapOf(
                "baseAmount" to FormFieldValue.Number(100.0),
                "totalAmount" to FormFieldValue.Currency(200.0, "INR"),
            )

        assertTrue("totalAmount" in validationErrors(gstSchema, values))
    }

    // ---- visibleFields: AND/OR composition ----

    @Test
    fun field_with_no_dependency_is_always_visible() {
        val schema = listOf(textField("name"))

        assertEquals(1, visibleFields(schema, emptyMap()).size)
    }

    @Test
    fun single_dependency_match_is_visible() {
        val schema = listOf(textField("other", dependentFieldKey = "type", dependentExpectedValue = "A"))
        val values = mapOf("type" to FormFieldValue.Select("A"))

        assertEquals(1, visibleFields(schema, values).size)
    }

    @Test
    fun single_dependency_mismatch_is_hidden() {
        val schema = listOf(textField("other", dependentFieldKey = "type", dependentExpectedValue = "A"))
        val values = mapOf("type" to FormFieldValue.Select("B"))

        assertTrue(visibleFields(schema, values).isEmpty())
    }

    @Test
    fun or_composition_within_one_dependent_key_matches_any_alternative() {
        val schema = listOf(textField("other", dependentFieldKey = "city", dependentExpectedValue = "MUM|DEL"))

        assertEquals(1, visibleFields(schema, mapOf("city" to FormFieldValue.Select("DEL"))).size)
        assertTrue(visibleFields(schema, mapOf("city" to FormFieldValue.Select("BLR"))).isEmpty())
    }

    @Test
    fun and_composition_across_dependent_keys_requires_all_to_match() {
        val schema =
            listOf(
                textField(
                    "other",
                    dependentFieldKey = "country,city",
                    dependentExpectedValue = "IN,MUM|DEL",
                ),
            )
        val bothMatch = mapOf("country" to FormFieldValue.Select("IN"), "city" to FormFieldValue.Select("MUM"))
        val onlyOneMatches = mapOf("country" to FormFieldValue.Select("US"), "city" to FormFieldValue.Select("MUM"))

        assertEquals(1, visibleFields(schema, bothMatch).size)
        assertTrue(visibleFields(schema, onlyOneMatches).isEmpty())
    }

    @Test
    fun hidden_required_field_is_not_validated() {
        val schema = listOf(textField("other", required = true, dependentFieldKey = "type", dependentExpectedValue = "A"))
        val values = mapOf("type" to FormFieldValue.Select("B"))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    // ---- computedFields: GST math ----

    @Test
    fun computed_fields_derive_gst_rate_and_total() {
        val values = mapOf("baseAmount" to FormFieldValue.Number(200.0))

        val computed = computedFields(gstSchema, values)

        assertEquals(FormFieldValue.Number(36.0), computed["gstAmount"])
        assertEquals(FormFieldValue.Currency(236.0, "INR"), computed["totalAmount"])
    }

    @Test
    fun computed_fields_round_to_two_decimal_places() {
        val values = mapOf("baseAmount" to FormFieldValue.Number(33.333))

        val computed = computedFields(gstSchema, values)

        assertEquals(FormFieldValue.Number(6.0), computed["gstAmount"])
    }

    @Test
    fun computed_fields_return_empty_when_base_missing() {
        assertNull(computedFields(gstSchema, emptyMap())["gstAmount"])
        assertNull(computedFields(gstSchema, emptyMap())["totalAmount"])
    }

    // ---- computedFields: autoFill propagation ----

    @Test
    fun autofill_field_copies_related_field_value() {
        val schema =
            listOf(
                textField("source"),
                textField("target").copy(autoFill = true, relatedFieldKey = "source"),
            )
        val values = mapOf("source" to FormFieldValue.Text("hello"))

        val computed = computedFields(schema, values)

        assertEquals(FormFieldValue.Text("hello"), computed["target"])
    }

    @Test
    fun autofill_field_without_related_value_is_not_computed() {
        val schema = listOf(textField("target").copy(autoFill = true, relatedFieldKey = "source"))

        assertNull(computedFields(schema, emptyMap())["target"])
    }
}
