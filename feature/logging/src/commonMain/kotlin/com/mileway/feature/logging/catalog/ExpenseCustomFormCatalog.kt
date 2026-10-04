package com.mileway.feature.logging.catalog

import com.mileway.core.forms.FormFieldType
import com.mileway.core.forms.MockFormSchema
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseCategoryDef

/** Optional typed expense details plus the existing category-specific GST fields. */
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

    const val SPLITS = "expenseSplits"
    const val ATTENDEES = "expenseAttendees"
    const val ITEMIZED = "expenseItemized"

    fun schemaFor(catalogDef: ExpenseCategoryDef?): List<MockFormSchema> {
        if (catalogDef == null) return emptyList()
        val gst = if (catalogDef.requiresGst) listOf(gstInvoiceNumberField, gstDeclarationField) else emptyList()
        return gst +
            listOf(
                MockFormSchema(SPLITS, SPLITS, "Split expense", FormFieldType.PERCENTAGE_SPLIT, rank = 2),
                MockFormSchema(ATTENDEES, ATTENDEES, "Attendees", FormFieldType.ATTENDEE_LIST, rank = 3),
                MockFormSchema(
                    ITEMIZED,
                    ITEMIZED,
                    "Receipt itemization",
                    FormFieldType.ITEMIZED_LINES,
                    rank = 4,
                    defaultValue = if (catalogDef.category == ExpenseCategory.ACCOMMODATION) "hotel" else null,
                ),
            )
    }
}
