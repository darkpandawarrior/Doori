package com.mileway.feature.logging.catalog

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
    fun `a non-requiresGst category gets an empty schema`() {
        val def = ExpenseCategoryCatalog.default().first { it.category == ExpenseCategory.FOOD }
        assertTrue(!def.requiresGst)
        assertTrue(ExpenseCustomFormCatalog.schemaFor(def).isEmpty())
    }

    @Test
    fun `a null catalogDef gets an empty schema`() {
        assertTrue(ExpenseCustomFormCatalog.schemaFor(null).isEmpty())
    }
}
