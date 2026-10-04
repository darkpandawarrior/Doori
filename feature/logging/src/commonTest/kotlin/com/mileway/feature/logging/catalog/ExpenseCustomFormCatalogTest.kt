package com.mileway.feature.logging.catalog

import com.mileway.core.forms.FormFieldType
import com.mileway.feature.logging.model.ExpenseCategory
import kotlin.test.Test
import kotlin.test.assertTrue

/** V27 P27.E.1: [ExpenseCustomFormCatalog.schemaFor] is gated purely on [ExpenseCategoryDef.requiresGst]. */
class ExpenseCustomFormCatalogTest {
    @Test
    fun `a requiresGst category gets a non-empty schema`() {
        val def = ExpenseCategoryCatalog.default().first { it.category == ExpenseCategory.ACCOMMODATION }
        assertTrue(def.requiresGst)
        assertTrue(ExpenseCustomFormCatalog.schemaFor(def).isNotEmpty())
    }

    @Test
    fun `a non-requiresGst category gets optional typed expense fields`() {
        val def = ExpenseCategoryCatalog.default().first { it.category == ExpenseCategory.FOOD }
        assertTrue(!def.requiresGst)
        val fields = ExpenseCustomFormCatalog.schemaFor(def)
        assertTrue(
            fields.map { it.type }.toSet() ==
                setOf(
                    FormFieldType.PERCENTAGE_SPLIT,
                    FormFieldType.ATTENDEE_LIST,
                    FormFieldType.ITEMIZED_LINES,
                ),
        )
        assertTrue(fields.none { it.required })
    }

    @Test
    fun `a null catalogDef gets an empty schema`() {
        assertTrue(ExpenseCustomFormCatalog.schemaFor(null).isEmpty())
    }
}
