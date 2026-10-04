package com.mileway.core.forms

import com.mileway.core.forms.field.attendeeError
import com.mileway.core.forms.field.perHeadAmountMinor
import com.mileway.core.forms.itemization.ItemizedLineInput
import com.mileway.core.forms.itemization.hotelTemplate
import com.mileway.core.forms.itemization.itemizationError
import com.mileway.core.forms.itemization.reconciliationDelta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExpenseDetailFieldsTest {
    @Test
    fun attendeeCountRecomputesPerHeadAndValidatesNames() {
        assertNull(perHeadAmountMinor(1001, 0))
        assertEquals(1001L, perHeadAmountMinor(1001, 1))
        assertEquals(501L, perHeadAmountMinor(1001, 2))
        assertEquals(334L, perHeadAmountMinor(1001, 3))
        assertNotNull(attendeeError(listOf("Alex", " alex ")))
        assertNotNull(attendeeError(listOf("")))
        assertNull(attendeeError(listOf("Alex", "Jordan")))
    }

    @Test
    fun itemizationReconcilesAgainstReceiptAndNeverInventsHotelAmounts() {
        val hotel = hotelTemplate()
        assertEquals(listOf("Room", "Meals", "Tax"), hotel.map { it.description })
        assertTrue(hotel.all { it.amountText.isEmpty() })
        assertNull(reconciliationDelta(1000, hotel))
        val items = listOf(ItemizedLineInput("Room", "8.00"), ItemizedLineInput("Tax", "2.00"))
        assertEquals(0L, reconciliationDelta(1000, items))
        assertNull(itemizationError(1000, items))
        assertEquals(-1L, reconciliationDelta(1001, items))
        assertNotNull(itemizationError(1001, items))
        assertNull(reconciliationDelta(0, listOf(ItemizedLineInput("a", "92233720368547758.07"), ItemizedLineInput("b", "0.01"))))
    }

    @Test
    fun dynamicFormRejectsBadAttendeesAndItemizationButAllowsEmptyOptionalFields() {
        val schema =
            listOf(
                MockFormSchema("a", "a", "Attendees", FormFieldType.ATTENDEE_LIST),
                MockFormSchema("i", "i", "Itemization", FormFieldType.ITEMIZED_LINES),
            )
        val values =
            mapOf(
                "a" to FormFieldValue.AttendeeList(listOf("")),
                "i" to FormFieldValue.ItemizedLines(listOf(ItemizedLineInput("Room", "1.00"))),
            )
        assertEquals(setOf("a", "i"), validationErrors(schema, values, ExpenseFieldContext(200)).keys)
        assertTrue(validationErrors(schema, defaultFormValues(schema)).isEmpty())
    }
}
