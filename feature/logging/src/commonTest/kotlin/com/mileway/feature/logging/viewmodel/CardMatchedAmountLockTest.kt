package com.mileway.feature.logging.viewmodel

import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CardMatchedAmountLockTest {
    @Test
    fun matchedForeignExpenseRetainsAmountCurrencyPinAndL8Anchor() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val records = ExpenseRepository()
                val pin = FxRate(85.0, "USD", sourceDate = "2026-09-25", source = FxRateSource.CARD_MATCHED)
                records.insert(
                    ExpenseRecord(
                        id = "matched",
                        category = ExpenseCategory.FOOD,
                        merchantName = "Cafe",
                        amountRupees = 10.0,
                        amountMinor = 1000,
                        currencyCode = "USD",
                        status = ExpenseStatus.DRAFT,
                        dateMs = 42,
                        cardMatchId = "tx",
                        fxRate = pin,
                        fxRatePinnedAt = 42,
                    ),
                )
                val vm = ExpenseViewModel(records)
                vm.onAction(ExpenseAction.OpenEdit("matched"))
                vm.onAction(ExpenseAction.SetAmount("20"))
                vm.onAction(ExpenseAction.SetCurrency("INR"))
                vm.onAction(ExpenseAction.SetManualFxRate("100"))
                assertEquals("10.00", vm.state.value.form.amountText)
                assertEquals("USD", vm.state.value.form.currencyCode)
                assertEquals(pin, vm.state.value.form.fxRate)
                assertEquals(
                    1000,
                    vm.state.value.form
                        .expenseFieldContext()
                        ?.anchorAmountMinor,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }
}
