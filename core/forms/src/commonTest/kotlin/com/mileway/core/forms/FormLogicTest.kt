package com.mileway.core.forms

import com.siddharth.kmp.common.asString
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

    // ---- defaultFormValues: FormFieldType -> FormFieldValue prefill mapping ----

    @Test
    fun default_form_values_parses_declared_defaults_per_type() {
        val schema =
            listOf(
                textField("name").copy(defaultValue = "Sid"),
                numberField("amount").copy(defaultValue = "42.5"),
                MockFormSchema(id = "r", fieldKey = "rating", label = "Rating", type = FormFieldType.RATING, defaultValue = "3"),
            )

        val defaults = defaultFormValues(schema)

        assertEquals(FormFieldValue.Text("Sid"), defaults["name"])
        assertEquals(FormFieldValue.Number(42.5), defaults["amount"])
        assertEquals(FormFieldValue.Rating(3), defaults["rating"])
    }

    @Test
    fun default_form_values_falls_back_to_blank_per_type_when_undeclared() {
        val schema =
            listOf(
                textField("name"),
                numberField("amount"),
                MockFormSchema(id = "d", fieldKey = "decl", label = "Declaration", type = FormFieldType.DECLARATION),
                MockFormSchema(id = "f", fieldKey = "file", label = "File", type = FormFieldType.FILE_PDF),
            )

        val defaults = defaultFormValues(schema)

        assertEquals(FormFieldValue.Text(""), defaults["name"])
        assertEquals(FormFieldValue.Number(null), defaults["amount"])
        assertEquals(FormFieldValue.Declaration(false), defaults["decl"])
        assertEquals(FormFieldValue.FileRef(emptyList()), defaults["file"])
    }

    @Test
    fun default_form_values_covers_every_field_type_without_crashing() {
        val schema = FormFieldType.entries.mapIndexed { i, type -> MockFormSchema(id = "$i", fieldKey = "f$i", label = "F$i", type = type) }

        assertEquals(FormFieldType.entries.size, defaultFormValues(schema).size)
    }

    // ---- validationErrors: FILE_PDF attachment field (P27.F.2) ----

    private fun attachmentField(
        key: String = "receipt",
        required: Boolean = false,
    ) = MockFormSchema(id = key, fieldKey = key, label = key.replaceFirstChar { it.uppercase() }, type = FormFieldType.FILE_PDF, required = required)

    @Test
    fun required_attachment_field_with_no_files_produces_error() {
        val schema = listOf(attachmentField(required = true))

        assertTrue("receipt" in validationErrors(schema, mapOf("receipt" to FormFieldValue.FileRef(emptyList()))))
        assertTrue("receipt" in validationErrors(schema, emptyMap()))
    }

    @Test
    fun required_attachment_field_with_a_file_has_no_error() {
        val schema = listOf(attachmentField(required = true))
        val values = mapOf("receipt" to FormFieldValue.FileRef(listOf("file:///receipt.jpg")))

        assertTrue(validationErrors(schema, values).isEmpty())
    }

    @Test
    fun single_purpose_attachment_field_is_independent_of_a_general_receipts_bucket() {
        // Two distinct FILE_PDF fields (a schema-defined single-purpose "toll receipt" plus a
        // general receipts bucket) validate independently by fieldKey — filling one never
        // satisfies the other's required-check.
        val schema = listOf(attachmentField("tollReceipt", required = true), attachmentField("receipts", required = false))
        val values = mapOf("receipts" to FormFieldValue.FileRef(listOf("file:///a.jpg", "file:///b.jpg")))

        val errors = validationErrors(schema, values)

        assertTrue("tollReceipt" in errors)
        assertTrue("receipts" !in errors)
    }

    // ---- validationErrors: the real field sets consolidated forms render (P27.F.6) ----
    // TrackSubmissionScreen's "Additional Details" fields (MileageSubmissionViewModel.SubmissionField)
    // and LogMilesStep2Screen's "Additional Details" card fields, mapped 1:1 into MockFormSchema —
    // locks in the single validationErrors() path both consolidated screens now render through.

    private val trackingAdditionalDetailsSchema =
        listOf(
            MockFormSchema(id = "purpose", fieldKey = "purpose", label = "Purpose of travel", type = FormFieldType.TEXT, required = true),
            MockFormSchema(
                id = "gender",
                fieldKey = "gender",
                label = "Gender",
                type = FormFieldType.SELECT,
                required = true,
                options = listOf("Male", "Female", "Others"),
            ),
        )

    @Test
    fun tracking_additional_details_blank_required_fields_produce_errors() {
        val errors = validationErrors(trackingAdditionalDetailsSchema, emptyMap())

        assertTrue("purpose" in errors)
        assertTrue("gender" in errors)
    }

    @Test
    fun tracking_additional_details_filled_required_fields_have_no_errors() {
        val values = mapOf("purpose" to FormFieldValue.Text("Client visit"), "gender" to FormFieldValue.Select("Male"))

        assertTrue(validationErrors(trackingAdditionalDetailsSchema, values).isEmpty())
    }

    private val logMilesStep2Schema =
        listOf(
            MockFormSchema(id = "invoiceDate", fieldKey = "invoiceDate", label = "Invoice date", type = FormFieldType.DATE, required = true),
            MockFormSchema(id = "note", fieldKey = "note", label = "Note", type = FormFieldType.TEXTAREA, required = false),
        )

    @Test
    fun log_miles_step2_missing_invoice_date_produces_error_but_optional_note_does_not() {
        val errors = validationErrors(logMilesStep2Schema, mapOf("note" to FormFieldValue.Text("")))

        assertTrue("invoiceDate" in errors)
        assertTrue("note" !in errors)
    }

    @Test
    fun log_miles_step2_with_invoice_date_has_no_error() {
        val values = mapOf("invoiceDate" to FormFieldValue.Date("2026-07-10"), "note" to FormFieldValue.Text(""))

        assertTrue(validationErrors(logMilesStep2Schema, values).isEmpty())
    }
}
