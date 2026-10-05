package com.mileway.core.forms

import com.mileway.core.data.domain.claim.SplitTarget
import com.mileway.core.forms.field.PercentageSplitInput
import com.mileway.core.forms.field.percentageAllocations
import com.mileway.core.forms.field.splitError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PercentageSplitFieldTest {
    private val entries =
        listOf(
            PercentageSplitInput(SplitTarget.COST_CENTER, "Sales", "33.33"),
            PercentageSplitInput(SplitTarget.PROJECT, "Project A", "33.33"),
            PercentageSplitInput(SplitTarget.PERSON, "Guest", "33.34"),
        )

    @Test
    fun allocationsReconcileEveryCentIncludingLargeAnchors() {
        listOf(1L, 101L, 12345L, Long.MAX_VALUE).forEach { anchor ->
            val allocations = assertNotNull(percentageAllocations(anchor, entries))
            assertEquals(anchor, checkedMinorTotal(allocations.map { it.amountMinor }))
            assertTrue(allocations.all { it.amountMinor >= 0 })
        }
    }

    @Test
    fun rejectsMismatchInvalidPercentagesAndRepeatedTargets() {
        assertNotNull(splitError(100, entries.dropLast(1)))
        assertNull(percentageAllocations(100, listOf(entries.first().copy(percentageText = "NaN"))))
        assertNull(percentageAllocations(100, listOf(entries.first().copy(percentageText = "100.001"))))
        assertNull(percentageAllocations(100, listOf(entries.first().copy(percentageText = "-1"))))
        assertNull(percentageAllocations(100, listOf(entries.first().copy(percentageText = "101"))))
        assertNull(percentageAllocations(0, entries))
        val repeated = listOf(entries.first().copy(percentageText = "50"), entries.first().copy(percentageText = "50"))
        assertNull(percentageAllocations(100, repeated))
    }

    @Test
    fun dynamicFormUsesCardAnchorAndRejectsIncompleteSplits() {
        val schema = listOf(MockFormSchema("split", "split", "Split", FormFieldType.PERCENTAGE_SPLIT))
        val context = ExpenseFieldContext(receiptAmountMinor = 100, cardMatchedAmountMinor = 200)
        assertEquals(200L, percentageAllocations(context.anchorAmountMinor, entries)!!.sumOf { it.amountMinor })
        assertTrue(validationErrors(schema, mapOf("split" to FormFieldValue.PercentageSplit(entries)), context).isEmpty())
        assertTrue(validationErrors(schema, mapOf("split" to FormFieldValue.PercentageSplit(entries.dropLast(1))), context).isNotEmpty())
        assertTrue(validationErrors(schema, mapOf("split" to FormFieldValue.PercentageSplit(entries))).isNotEmpty())
    }

    @Test
    fun decimalInputRejectsOverflowAndPreservesExactMinorUnits() {
        assertEquals(1001L, parseMinorAmount("10.01"))
        assertEquals(Long.MAX_VALUE, parseMinorAmount("92233720368547758.07"))
        assertNull(parseMinorAmount("92233720368547758.08"))
        assertNull(parseMinorAmount("1e3"))
        assertNull(parseMinorAmount("Infinity"))
    }
}
