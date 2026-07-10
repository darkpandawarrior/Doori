package com.mileway.core.forms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MockFormSchemaTest {
    @Test
    fun schema_construction_applies_defaults() {
        val field =
            MockFormSchema(
                id = "f1",
                fieldKey = "amount",
                label = "Amount",
                type = FormFieldType.NUMBER,
            )

        assertEquals(false, field.required)
        assertEquals(emptyList(), field.options)
        assertEquals(0, field.rank)
        assertTrue(field.editable)
        assertEquals(false, field.autoFill)
        assertEquals(null, field.dependentFieldKey)
    }

    @Test
    fun provider_returns_canned_schema_for_known_context() {
        val provider: FormSchemaProvider = MockFormSchemaProvider()

        val generic = provider.schemaFor(MockFormCatalog.CONTEXT_GENERIC)
        val expense = provider.schemaFor(MockFormCatalog.CONTEXT_EXPENSE)

        assertEquals(2, generic.size)
        assertEquals(4, expense.size)
        assertTrue(expense.any { it.fieldKey == "baseAmount" })
    }

    @Test
    fun provider_returns_empty_for_unknown_context() {
        val provider: FormSchemaProvider = MockFormSchemaProvider()

        assertEquals(emptyList(), provider.schemaFor("does-not-exist"))
    }

    @Test
    fun all_16_field_types_are_distinct() {
        assertEquals(16, FormFieldType.entries.size)
        assertEquals(FormFieldType.entries.toSet().size, FormFieldType.entries.size)
    }
}
