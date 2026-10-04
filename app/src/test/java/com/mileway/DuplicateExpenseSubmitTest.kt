package com.mileway

import app.cash.turbine.test
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.core.data.model.ScannerPrefill
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DuplicateExpenseSubmitTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun warningPrecedesInsertAndDismissDoesNotSave() = runTest {
        val repository = ExpenseRepository()
        val existing = repository.getById("EXP-001")!!
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Scanner(ScannerPrefill(
            merchant = existing.merchantName, amountText = existing.amountRupees.toString(),
            category = ExpenseCategory.FOOD.name, dateEpochMs = existing.dateMs,
            currency = "INR", overallConfidence = 1f, duplicateWarning = null,
        ))))
        advanceUntilIdle()
        val count = repository.getAll().size
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowDuplicateWarning)
            assertEquals(count, repository.getAll().size)
            vm.onAction(ExpenseAction.DismissDuplicateWarning)
            assertEquals(count, repository.getAll().size)
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowDuplicateWarning)
            vm.onAction(ExpenseAction.ConfirmDuplicateExpense)
            advanceUntilIdle()
            assertTrue(awaitItem() is ExpenseEffect.NavigateToSuccess)
            assertEquals(count + 1, repository.getAll().size)
        }
    }

    @Test
    fun staleConfirmationCannotSaveAnInvalidEditedForm() = runTest {
        val repository = ExpenseRepository()
        val existing = repository.getById("EXP-001")!!
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Scanner(ScannerPrefill(
            merchant = existing.merchantName, amountText = existing.amountRupees.toString(),
            category = ExpenseCategory.FOOD.name, dateEpochMs = existing.dateMs,
            currency = "INR", overallConfidence = 1f, duplicateWarning = null,
        ))))
        advanceUntilIdle()
        val count = repository.getAll().size
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowDuplicateWarning)
            vm.onAction(ExpenseAction.SetAmount(""))
            vm.onAction(ExpenseAction.ConfirmDuplicateExpense)
            advanceUntilIdle()
            assertTrue(vm.state.value.form.errors.isNotEmpty())
            assertEquals(count, repository.getAll().size)
            expectNoEvents()
        }
    }
    @Test
    fun policyConfirmationStillChecksDuplicatesBeforeSaving() = runTest {
        val repository = ExpenseRepository()
        val existing = repository.getById("EXP-002")!!
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.OpenWithContext(ExpenseSourceContext.Scanner(ScannerPrefill(
            merchant = existing.merchantName, amountText = existing.amountRupees.toString(),
            category = ExpenseCategory.TRAVEL.name, dateEpochMs = existing.dateMs,
            currency = "INR", overallConfidence = 1f, duplicateWarning = null,
        ))))
        vm.onAction(ExpenseAction.SetReceiptImage(existing.receiptImagePath))
        vm.onAction(ExpenseAction.SetOfficeCode(existing.officeCode))
        advanceUntilIdle()
        val count = repository.getAll().size
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
            vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            assertTrue(awaitItem() is ExpenseEffect.ShowDuplicateWarning)
            assertEquals(count, repository.getAll().size)
        }
    }

}
