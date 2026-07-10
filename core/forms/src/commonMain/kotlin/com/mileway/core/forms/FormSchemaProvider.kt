package com.mileway.core.forms

/**
 * Local generic-schema-engine lookup — DiCE's `FormEndpoint` (GENERIC/PROCESSOR/EXPENSE/...)
 * becomes a plain local string key here since there is no backend yet (see project CLAUDE.md,
 * "The backend").
 */
fun interface FormSchemaProvider {
    fun schemaFor(formContext: String): List<MockFormSchema>
}

/** Canned schemas for the mock provider, keyed by [FormSchemaProvider.schemaFor]'s formContext. */
object MockFormCatalog {
    const val CONTEXT_GENERIC = "generic"
    const val CONTEXT_EXPENSE = "expense"

    val schemas: Map<String, List<MockFormSchema>> =
        mapOf(
            CONTEXT_GENERIC to
                listOf(
                    MockFormSchema(
                        id = "generic.name",
                        fieldKey = "name",
                        label = "Name",
                        type = FormFieldType.TEXT,
                        required = true,
                        rank = 0,
                        maxLength = 80,
                    ),
                    MockFormSchema(
                        id = "generic.notes",
                        fieldKey = "notes",
                        label = "Notes",
                        type = FormFieldType.TEXTAREA,
                        rank = 1,
                        maxLength = 500,
                    ),
                ),
            CONTEXT_EXPENSE to
                listOf(
                    MockFormSchema(
                        id = "expense.baseAmount",
                        fieldKey = "baseAmount",
                        label = "Base amount",
                        type = FormFieldType.NUMBER,
                        required = true,
                        rank = 0,
                        min = 0.0,
                    ),
                    MockFormSchema(
                        id = "expense.gstAmount",
                        fieldKey = "gstAmount",
                        label = "GST (18%)",
                        type = FormFieldType.NUMBER,
                        rank = 1,
                        editable = false,
                        autoFill = true,
                        relatedFieldKey = "baseAmount",
                        relationType = RelationType.GST_RATE,
                        defaultValue = "18",
                    ),
                    MockFormSchema(
                        id = "expense.totalAmount",
                        fieldKey = "totalAmount",
                        label = "Total (incl. GST)",
                        type = FormFieldType.CURRENCY,
                        rank = 2,
                        editable = false,
                        autoFill = true,
                        relatedFieldKey = "baseAmount",
                        relationType = RelationType.GST_TOTAL,
                    ),
                    MockFormSchema(
                        id = "expense.declaration",
                        fieldKey = "declaration",
                        label = "I declare this expense is genuine",
                        type = FormFieldType.DECLARATION,
                        required = true,
                        rank = 3,
                        declarationId = "expense-genuine-v1",
                    ),
                ),
        )

    /**
     * Canned master data for the enterprise-only field types ([FormFieldType.CITY_AIRPORT],
     * [FormFieldType.IRN], [FormFieldType.MASTER], [FormFieldType.EMPLOYEE_DEPARTMENT]), keyed by
     * [MockFormSchema.masterType]. The renderer looks this up instead of calling a backend — see
     * project CLAUDE.md, "The backend".
     */
    val masterData: Map<String, List<String>> =
        mapOf(
            "city_airport" to listOf("Mumbai (BOM)", "Delhi (DEL)", "Bengaluru (BLR)", "Chennai (MAA)", "Pune (PNQ)"),
            "irn" to listOf("IRN-000123", "IRN-000456", "IRN-000789"),
            "employee_department" to listOf("Engineering", "Sales", "Finance", "Operations", "HR"),
            "cost_center" to listOf("CC-100", "CC-200", "CC-300"),
            "vendor" to listOf("Acme Vendors", "Globex Supplies", "Initech Logistics"),
        )
}

class MockFormSchemaProvider : FormSchemaProvider {
    override fun schemaFor(formContext: String): List<MockFormSchema> = MockFormCatalog.schemas[formContext].orEmpty()
}
