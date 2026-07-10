package com.mileway.feature.logging.validation

import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.viewmodel.ExpenseFormState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpenseFormValidatorTest {
    private val travelDef = ExpenseCategoryCatalog.default().first { it.category == ExpenseCategory.TRAVEL }

    @Test
    fun `valid form has no errors`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "Ola Cabs",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `blank merchant name produces merchant error only`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "   ",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_MERCHANT_NAME), errors.keys)
    }

    @Test
    fun `zero amount produces amount error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "0",
                merchantName = "Ola Cabs",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_AMOUNT), errors.keys)
    }

    @Test
    fun `negative amount produces amount error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "-50",
                merchantName = "Ola Cabs",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_AMOUNT), errors.keys)
    }

    @Test
    fun `non-numeric amount text produces amount error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "abc",
                merchantName = "Ola Cabs",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_AMOUNT), errors.keys)
    }

    @Test
    fun `missing category produces category error`() {
        val form =
            ExpenseFormState(
                category = null,
                amountText = "500",
                merchantName = "Ola Cabs",
            )
        val errors = ExpenseFormValidator.validate(form, null)
        assertEquals(setOf(ExpenseFormValidator.FIELD_CATEGORY), errors.keys)
    }

    @Test
    fun `all fields blank produces all three errors`() {
        val errors = ExpenseFormValidator.validate(ExpenseFormState(), null)
        assertEquals(
            setOf(
                ExpenseFormValidator.FIELD_CATEGORY,
                ExpenseFormValidator.FIELD_MERCHANT_NAME,
                ExpenseFormValidator.FIELD_AMOUNT,
            ),
            errors.keys,
        )
    }

    // ── P1.7: cost-center-gated categories require an officeCode ──────────────

    @Test
    fun `missing officeCode on a cost-center-gated category produces an officeCode error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "Ola Cabs",
                officeCode = null,
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_OFFICE_CODE), errors.keys)
    }

    @Test
    fun `blank officeCode on a cost-center-gated category produces an officeCode error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "Ola Cabs",
                officeCode = "   ",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_OFFICE_CODE), errors.keys)
    }

    @Test
    fun `officeCode present on a cost-center-gated category has no errors`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "Ola Cabs",
                officeCode = "1345",
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `missing officeCode on a non-cost-center category is not required`() {
        val foodDef = ExpenseCategoryCatalog.default().first { it.category == ExpenseCategory.FOOD }
        val form =
            ExpenseFormState(
                category = ExpenseCategory.FOOD,
                amountText = "500",
                merchantName = "Cafe Coffee Day",
                officeCode = null,
            )
        val errors = ExpenseFormValidator.validate(form, foodDef)
        assertTrue(errors.isEmpty())
    }

    // ── P27.E.4: Card-context amount ceiling ────────────────────────────────

    @Test
    fun `amount above the card transaction ceiling produces an amount error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "600",
                merchantName = "Ola Cabs",
                officeCode = "1345",
                sourceContext = ExpenseSourceContext.Card("card-1", "txn-1", transactionAmountRupees = 500.0),
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertEquals(setOf(ExpenseFormValidator.FIELD_AMOUNT), errors.keys)
    }

    @Test
    fun `amount at or below the card transaction ceiling has no amount error`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "500",
                merchantName = "Ola Cabs",
                officeCode = "1345",
                sourceContext = ExpenseSourceContext.Card("card-1", "txn-1", transactionAmountRupees = 500.0),
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `card context without a known transaction amount does not cap the entered amount`() {
        val form =
            ExpenseFormState(
                category = ExpenseCategory.TRAVEL,
                amountText = "99999",
                merchantName = "Ola Cabs",
                officeCode = "1345",
                sourceContext = ExpenseSourceContext.Card("card-1", "txn-1"),
            )
        val errors = ExpenseFormValidator.validate(form, travelDef)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `lockedFieldKeys locks merchant and category only for the Card context`() {
        assertEquals(
            setOf(ExpenseFormValidator.FIELD_MERCHANT_NAME, ExpenseFormValidator.FIELD_CATEGORY),
            ExpenseFormValidator.lockedFieldKeys(ExpenseSourceContext.Card("card-1", "txn-1")),
        )
        assertTrue(ExpenseFormValidator.lockedFieldKeys(ExpenseSourceContext.Trip("trip-1")).isEmpty())
        assertTrue(ExpenseFormValidator.lockedFieldKeys(ExpenseSourceContext.Regular).isEmpty())
    }
}
