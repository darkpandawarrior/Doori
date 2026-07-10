package com.mileway.feature.logging.catalog

import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.MockFormSchema
import com.mileway.feature.logging.model.ExpenseCategoryDef

/**
 * V27 P27.E.1: step-2's "custom forms" section — a per-category [MockFormSchema] list rendered
 * through `core:forms`' shared `FormRenderer`, mirroring how [ExpenseCategoryCatalog] already
 * gates the office/cost-center field. Today only [ExpenseCategoryDef.requiresGst] categories carry
 * a schema (GST invoice number + accuracy declaration) — a real, previously-unused catalog flag
 * that had no field wired to it anywhere. Categories with no custom schema render nothing extra.
 */
object ExpenseCustomFormCatalog {
    private val gstInvoiceNumberField =
        MockFormSchema(
            id = "gstInvoiceNumber",
            fieldKey = "gstInvoiceNumber",
            label = "GST Invoice Number",
            type = FormFieldType.TEXT,
            required = true,
            rank = 0,
        )
    private val gstDeclarationField =
        MockFormSchema(
            id = "gstDeclaration",
            fieldKey = "gstDeclaration",
            label = "I confirm the GST details above are accurate",
            type = FormFieldType.DECLARATION,
            required = true,
            rank = 1,
        )

    fun schemaFor(catalogDef: ExpenseCategoryDef?): List<MockFormSchema> =
        if (catalogDef?.requiresGst == true) listOf(gstInvoiceNumberField, gstDeclarationField) else emptyList()
}
