package com.mileway

import app.cash.turbine.test
import com.mileway.core.data.model.db.DraftExpenseEntity
import com.mileway.core.network.model.SubmissionStatus
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.validation.ExpenseFormValidator
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseFilter
import com.mileway.feature.logging.viewmodel.ExpenseListData
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * H: behavioural coverage for [ExpenseViewModel], the expense list + multi-step submission reducer.
 * The repository is a concrete in-memory mock (no deps).
 */
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = ExpenseViewModel(ExpenseRepository())

    private fun listData(vm: ExpenseViewModel): ExpenseListData {
        val s = vm.state.value.listState
        assertTrue(s is ScreenState.Content<ExpenseListData>)
        return (s as ScreenState.Content<ExpenseListData>).data
    }

    @Test
    fun `init loads all expenses under the ALL filter`() {
        val data = listData(viewModel())
        assertEquals(ExpenseFilter.ALL, data.activeFilter)
        assertEquals(ExpenseRepository().getAll().size, data.records.size)
    }

    @Test
    fun `SetFilter narrows the list to the chosen status`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetFilter(ExpenseFilter.DRAFTS))
        val data = listData(vm)
        assertEquals(ExpenseFilter.DRAFTS, data.activeFilter)
        assertTrue(data.records.isNotEmpty())
        assertTrue(data.records.all { it.status == ExpenseStatus.DRAFT })
    }

    @Test
    fun `SetSort by amount orders records high to low and keeps the active sort`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetSort(com.mileway.feature.logging.viewmodel.ExpenseSort.AMOUNT))
        val data = listData(vm)
        assertEquals(com.mileway.feature.logging.viewmodel.ExpenseSort.AMOUNT, data.activeSort)
        val amounts = data.records.map { it.amountRupees }
        assertEquals(amounts.sortedDescending(), amounts)
    }

    // ── V27 P27.E.1: the 2-step wizard's step state + AdvanceStep/RetreatStep gate ─────

    @Test
    fun `SelectCategory sets the category but stays on step one`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.entries.first()))
        assertEquals(1, vm.state.value.form.step)
        assertEquals(ExpenseCategory.entries.first(), vm.state.value.form.category)
    }

    @Test
    fun `AdvanceStep without a category sets a category error and stays on step one`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.AdvanceStep)
        assertEquals(1, vm.state.value.form.step)
        assertTrue(vm.state.value.form.errors.containsKey(ExpenseFormValidator.FIELD_CATEGORY))
    }

    @Test
    fun `AdvanceStep after SelectCategory moves to step two`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.AdvanceStep)
        assertEquals(2, vm.state.value.form.step)
        assertTrue(vm.state.value.form.errors.isEmpty())
    }

    @Test
    fun `RetreatStep moves step two back to step one without clearing fields`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.AdvanceStep)
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.RetreatStep)
        assertEquals(1, vm.state.value.form.step)
        assertEquals("Cafe Coffee Day", vm.state.value.form.merchantName)
    }

    @Test
    fun `SetFormValue records a custom-form field value`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetFormValue("gstInvoiceNumber", com.mileway.core.forms.FormFieldValue.Text("GST-123")))
        assertEquals(
            com.mileway.core.forms.FormFieldValue.Text("GST-123"),
            vm.state.value.form.formValues["gstInvoiceNumber"],
        )
    }

    @Test
    fun `SubmitExpense records the amount and navigates to success`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            val effect = awaitItem()
            assertTrue(effect is ExpenseEffect.NavigateToSuccess)
            assertEquals((effect as ExpenseEffect.NavigateToSuccess).id, vm.state.value.lastSubmittedId)
        }
        assertEquals(249.50, vm.state.value.lastSubmittedAmount)
    }

    @Test
    fun `SubmitExpense appends the new record to the repository`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val before = repository.getAll().size
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        assertEquals(before + 1, repository.getAll().size)
        val inserted = repository.getById(vm.state.value.lastSubmittedId)
        assertNotNull(inserted)
        assertEquals("Cafe Coffee Day", inserted.merchantName)
        assertEquals(ExpenseCategory.FOOD, inserted.category)
    }

    @Test
    fun `SubmitExpense with a blank merchant name sets a field error instead of submitting`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.onAction(ExpenseAction.SubmitExpense)
        assertEquals("", vm.state.value.lastSubmittedId)
        assertTrue(vm.state.value.form.errors.containsKey(ExpenseFormValidator.FIELD_MERCHANT_NAME))
    }

    @Test
    fun `SetReceiptImage attaches then SetReceiptImage null clears it`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetReceiptImage("content://media/picked/1"))
        assertEquals("content://media/picked/1", vm.state.value.form.receiptImagePath)
        vm.onAction(ExpenseAction.SetReceiptImage(null))
        assertEquals(null, vm.state.value.form.receiptImagePath)
    }

    @Test
    fun `SubmitExpense persists the attached receipt image path to the repository`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.onAction(ExpenseAction.SetReceiptImage("content://media/picked/2"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        val inserted = repository.getById(vm.state.value.lastSubmittedId)
        assertNotNull(inserted)
        assertEquals("content://media/picked/2", inserted.receiptImagePath)
    }

    @Test
    fun `SubmitExpense with no receipt attached persists a null receiptImagePath`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        val inserted = repository.getById(vm.state.value.lastSubmittedId)
        assertNotNull(inserted)
        assertEquals(null, inserted.receiptImagePath)
    }

    // ── P1.7: project/cost-center tagging via stub Office picker ───────────────

    @Test
    fun `SetOfficeCode sets then clears the form's officeCode`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        assertEquals("1345", vm.state.value.form.officeCode)
        vm.onAction(ExpenseAction.SetOfficeCode(null))
        assertEquals(null, vm.state.value.form.officeCode)
    }

    @Test
    fun `SubmitExpense on a cost-center-gated category without an officeCode sets a field error`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Ola Cabs"))
        vm.onAction(ExpenseAction.SetAmount("500"))
        vm.onAction(ExpenseAction.SubmitExpense)
        assertEquals("", vm.state.value.lastSubmittedId)
        assertTrue(vm.state.value.form.errors.containsKey(ExpenseFormValidator.FIELD_OFFICE_CODE))
    }

    @Test
    fun `SubmitExpense on a cost-center-gated category persists the selected officeCode`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Ola Cabs"))
        vm.onAction(ExpenseAction.SetAmount("500"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        val inserted = repository.getById(vm.state.value.lastSubmittedId)
        assertNotNull(inserted)
        assertEquals("1345", inserted.officeCode)
    }

    @Test
    fun `SubmitExpense on a category that does not require a cost center persists a null officeCode`() = runTest {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        val inserted = repository.getById(vm.state.value.lastSubmittedId)
        assertNotNull(inserted)
        assertEquals(null, inserted.officeCode)
    }

    // ── V27 P27.E.1: step-2 custom form (ExpenseCustomFormCatalog, requiresGst categories) ──

    @Test
    fun `SubmitExpense on a GST-required category without the custom form fields sets a custom-form error`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.ACCOMMODATION)) // requiresGst = true
        vm.onAction(ExpenseAction.SetMerchant("Taj Hotel"))
        vm.onAction(ExpenseAction.SetAmount("2000.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.onAction(ExpenseAction.SubmitExpense)
        assertEquals("", vm.state.value.lastSubmittedId)
        assertTrue(vm.state.value.form.errors.containsKey("gstInvoiceNumber"))
    }

    @Test
    fun `SubmitExpense on a GST-required category with the custom form filled in submits`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.ACCOMMODATION))
        vm.onAction(ExpenseAction.SetMerchant("Taj Hotel"))
        vm.onAction(ExpenseAction.SetAmount("2000.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.onAction(ExpenseAction.SetFormValue("gstInvoiceNumber", com.mileway.core.forms.FormFieldValue.Text("GST-INV-1")))
        vm.onAction(ExpenseAction.SetFormValue("gstDeclaration", com.mileway.core.forms.FormFieldValue.Declaration(true)))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.NavigateToSuccess)
        }
        assertTrue(vm.state.value.lastSubmittedId.isNotEmpty())
    }

    @Test
    fun `SubmitExpense on a category with no requiresGst flag never demands the custom form`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.NavigateToSuccess)
        }
        assertTrue(vm.state.value.form.errors.isEmpty())
    }

    @Test
    fun `OpenDetail resolves a known id and falls back to Empty for an unknown one`() {
        val vm = viewModel()
        val known: ExpenseRecord = ExpenseRepository().getAll().first()
        vm.onAction(ExpenseAction.OpenDetail(known.id))
        assertTrue(vm.state.value.detailState is ScreenState.Content<ExpenseRecord>)

        vm.onAction(ExpenseAction.OpenDetail("does-not-exist"))
        assertEquals(ScreenState.Empty, vm.state.value.detailState)
    }

    @Test
    fun `ResetForm clears the form and last submission`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SetMerchant("X"))
        vm.onAction(ExpenseAction.ResetForm)
        assertEquals("", vm.state.value.form.merchantName)
        assertEquals("", vm.state.value.lastSubmittedId)
    }

    @Test
    fun `SaveDraft persists the current form to the repository`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            val repository = ExpenseRepository(dao)
            val vm = ExpenseViewModel(repository)
            vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
            vm.onAction(ExpenseAction.SetMerchant("Uber: Airport"))
            vm.onAction(ExpenseAction.SetAmount("450.0"))
            vm.effect.test {
                vm.onAction(ExpenseAction.SaveDraft)
                val effect = awaitItem()
                assertTrue(effect is ExpenseEffect.ShowToast)
            }
            val persisted = dao.getDraft()
            assertNotNull(persisted)
            assertEquals("Uber: Airport", persisted.merchantName)
            assertEquals("450.0", persisted.amountText)
            assertEquals(ExpenseCategory.TRAVEL.name, persisted.categoryName)
        }

    @Test
    fun `a persisted draft from a previous session is offered as resumableDraft on init`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            dao.upsertDraft(
                DraftExpenseEntity(
                    categoryName = ExpenseCategory.FOOD.name,
                    amountText = "199.0",
                    merchantName = "Cafe Coffee Day",
                    note = "team snacks",
                    receiptImagePath = null,
                    updatedAt = 42L,
                ),
            )
            val vm = ExpenseViewModel(ExpenseRepository(dao))
            advanceUntilIdle()
            assertEquals("Cafe Coffee Day", vm.state.value.resumableDraft?.merchantName)
        }

    @Test
    fun `init offers no resumableDraft when nothing was ever saved`() =
        runTest {
            val vm = ExpenseViewModel(ExpenseRepository(FakeDraftExpenseDao()))
            advanceUntilIdle()
            assertNull(vm.state.value.resumableDraft)
        }

    @Test
    fun `ResumeDraft loads the persisted draft into the form and clears the offer`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            dao.upsertDraft(
                DraftExpenseEntity(
                    categoryName = ExpenseCategory.ACCOMMODATION.name,
                    amountText = "8900.0",
                    merchantName = "Taj Hotel",
                    note = "client visit",
                    receiptImagePath = "content://media/picked/9",
                    updatedAt = 100L,
                ),
            )
            val vm = ExpenseViewModel(ExpenseRepository(dao))
            advanceUntilIdle()
            vm.onAction(ExpenseAction.ResumeDraft)

            assertEquals(ExpenseCategory.ACCOMMODATION, vm.state.value.form.category)
            assertEquals("8900.0", vm.state.value.form.amountText)
            assertEquals("Taj Hotel", vm.state.value.form.merchantName)
            assertEquals("client visit", vm.state.value.form.note)
            assertEquals("content://media/picked/9", vm.state.value.form.receiptImagePath)
            assertNull(vm.state.value.resumableDraft)
        }

    @Test
    fun `DiscardDraft clears both the persisted draft and the resumable offer`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            dao.upsertDraft(
                DraftExpenseEntity(categoryName = null, amountText = "10", merchantName = "A", note = "", receiptImagePath = null, updatedAt = 1L),
            )
            val repository = ExpenseRepository(dao)
            val vm = ExpenseViewModel(repository)
            advanceUntilIdle()
            assertNotNull(vm.state.value.resumableDraft)

            vm.onAction(ExpenseAction.DiscardDraft)
            advanceUntilIdle()

            assertNull(vm.state.value.resumableDraft)
            assertEquals(null, dao.getDraft())
        }

    @Test
    fun `DismissResumeDraft clears the offer but leaves the draft persisted`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            dao.upsertDraft(
                DraftExpenseEntity(categoryName = null, amountText = "10", merchantName = "A", note = "", receiptImagePath = null, updatedAt = 1L),
            )
            val vm = ExpenseViewModel(ExpenseRepository(dao))
            advanceUntilIdle()

            vm.onAction(ExpenseAction.DismissResumeDraft)

            assertNull(vm.state.value.resumableDraft)
            assertNotNull(dao.getDraft())
        }

    // ── P1.6: tiered policy engine (PolicyMockData.outcomeForExpenseAmount) ────

    @Test
    fun `SubmitExpense below 1000 resolves to SUCCESS with no violations`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("249.50"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        assertEquals(SubmissionStatus.SUCCESS, vm.state.value.lastSubmissionStatus)
        assertTrue(vm.state.value.lastSubmissionViolations.isEmpty())
    }

    @Test
    fun `SubmitExpense between 1000 and 5000 resolves to REIMBURSABLE_ADJUSTED`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
        vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
        vm.onAction(ExpenseAction.SetAmount("1500.0"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            awaitItem()
        }
        assertEquals(SubmissionStatus.REIMBURSABLE_ADJUSTED, vm.state.value.lastSubmissionStatus)
        assertTrue(vm.state.value.lastSubmissionViolations.isEmpty())
    }

    // V27 P27.E.3: a tiered-policy-blocking outcome no longer submits straight through — the
    // first SubmitExpense emits ShowPolicySheet and leaves lastSubmissionStatus untouched;
    // ConfirmSubmitDespitePolicy (the sheet's "Submit Anyway") is what actually performs the
    // submit and updates state, mirroring the ModalBottomSheet flow the real screen drives.

    @Test
    fun `SubmitExpense between 5000 and 10000 shows the policy sheet with a violation`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Ola Cabs"))
        vm.onAction(ExpenseAction.SetAmount("7500.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            val effect = awaitItem()
            assertTrue(effect is ExpenseEffect.ShowPolicySheet)
            assertEquals(1, (effect as ExpenseEffect.ShowPolicySheet).violations.size)
        }
        assertEquals(SubmissionStatus.SUCCESS, vm.state.value.lastSubmissionStatus) // untouched until confirmed
    }

    @Test
    fun `ConfirmSubmitDespitePolicy after a POLICY_VIOLATION sheet performs the submit`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Ola Cabs"))
        vm.onAction(ExpenseAction.SetAmount("7500.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
            vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            assertTrue(awaitItem() is ExpenseEffect.NavigateToSuccess)
        }
        assertEquals(SubmissionStatus.POLICY_VIOLATION, vm.state.value.lastSubmissionStatus)
        assertEquals(1, vm.state.value.lastSubmissionViolations.size)
    }

    @Test
    fun `SubmitExpense between 10000 and 25000 resolves to NEEDS_APPROVAL once confirmed`() = runTest {
        // TRAVEL (not ACCOMMODATION/OFFICE_SUPPLIES) so this doesn't also need the requiresGst
        // custom form filled in — that gate is covered separately below.
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Taj Travels"))
        vm.onAction(ExpenseAction.SetAmount("15000.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
            vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            awaitItem()
        }
        assertEquals(SubmissionStatus.NEEDS_APPROVAL, vm.state.value.lastSubmissionStatus)
        assertTrue(vm.state.value.lastSubmissionViolations.isEmpty())
    }

    @Test
    fun `SubmitExpense above 25000 resolves to HARD_STOP once confirmed`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("IndiGo Airlines"))
        vm.onAction(ExpenseAction.SetAmount("30000.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
            vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            awaitItem()
        }
        assertEquals(SubmissionStatus.HARD_STOP, vm.state.value.lastSubmissionStatus)
        assertEquals(1, vm.state.value.lastSubmissionViolations.size)
    }

    @Test
    fun `ResetForm clears the last submission status and violations`() = runTest {
        val vm = viewModel()
        vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.TRAVEL))
        vm.onAction(ExpenseAction.SetMerchant("Ola Cabs"))
        vm.onAction(ExpenseAction.SetAmount("7500.0"))
        vm.onAction(ExpenseAction.SetOfficeCode("1345"))
        vm.effect.test {
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
            vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
            awaitItem()
        }
        vm.onAction(ExpenseAction.ResetForm)
        assertEquals(SubmissionStatus.SUCCESS, vm.state.value.lastSubmissionStatus)
        assertTrue(vm.state.value.lastSubmissionViolations.isEmpty())
    }

    // ── P1.8: edit-after-submit / resubmit flow ────────────────────────────────

    @Test
    fun `OpenEdit pre-fills the form from an existing record and marks it as editing`() {
        val repository = ExpenseRepository()
        val vm = ExpenseViewModel(repository)
        val rejected = repository.getById("EXP-007")
        assertNotNull(rejected)

        vm.onAction(ExpenseAction.OpenEdit("EXP-007"))

        val form = vm.state.value.form
        assertTrue(form.isEditing)
        assertEquals("EXP-007", form.editingId)
        assertEquals(rejected.category, form.category)
        assertEquals(rejected.merchantName, form.merchantName)
        assertEquals(rejected.note, form.note)
        assertEquals(2, form.step)
    }

    @Test
    fun `OpenEdit with an unknown id is a no-op`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.OpenEdit("does-not-exist"))
        assertEquals(false, vm.state.value.form.isEditing)
        assertEquals(null, vm.state.value.form.editingId)
    }

    @Test
    fun `SubmitExpense after OpenEdit updates the same record id instead of minting a new one`() =
        runTest {
            val repository = ExpenseRepository()
            val vm = ExpenseViewModel(repository)
            val before = repository.getAll().size

            vm.onAction(ExpenseAction.OpenEdit("EXP-007"))
            // EXP-007 is FOOD; 5200 falls in FOOD's POLICY_VIOLATION range (5000-9999.99) so this
            // resubmit also exercises the V27 P27.E.3 policy-sheet gate before landing on Success.
            vm.onAction(ExpenseAction.SetAmount("5200.0"))
            vm.onAction(ExpenseAction.SetNote("Resubmitted with updated receipt"))
            vm.effect.test {
                vm.onAction(ExpenseAction.SubmitExpense)
                assertTrue(awaitItem() is ExpenseEffect.ShowPolicySheet)
                vm.onAction(ExpenseAction.ConfirmSubmitDespitePolicy)
                val effect = awaitItem()
                assertTrue(effect is ExpenseEffect.NavigateToSuccess)
                assertEquals("EXP-007", (effect as ExpenseEffect.NavigateToSuccess).id)
            }

            assertEquals(before, repository.getAll().size)
            val updated = repository.getById("EXP-007")
            assertNotNull(updated)
            assertEquals(5200.0, updated.amountRupees)
            assertEquals("Resubmitted with updated receipt", updated.note)
            assertEquals(ExpenseStatus.PENDING, updated.status)
        }

    @Test
    fun `SubmitExpense without OpenEdit still mints a new EXP-NEW id (create path unaffected)`() =
        runTest {
            val repository = ExpenseRepository()
            val vm = ExpenseViewModel(repository)
            vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
            vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
            vm.onAction(ExpenseAction.SetAmount("249.50"))
            vm.effect.test {
                vm.onAction(ExpenseAction.SubmitExpense)
                awaitItem()
            }
            assertTrue(vm.state.value.lastSubmittedId.startsWith("EXP-NEW-"))
        }

    @Test
    fun `SubmitExpense clears the persisted draft on success`() =
        runTest {
            val dao = FakeDraftExpenseDao()
            dao.upsertDraft(
                DraftExpenseEntity(categoryName = null, amountText = "10", merchantName = "A", note = "", receiptImagePath = null, updatedAt = 1L),
            )
            val vm = ExpenseViewModel(ExpenseRepository(dao))
            vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
            vm.onAction(ExpenseAction.SetMerchant("Cafe Coffee Day"))
            vm.onAction(ExpenseAction.SetAmount("249.50"))
            vm.effect.test {
                vm.onAction(ExpenseAction.SubmitExpense)
                awaitItem()
            }
            assertNull(dao.getDraft())
            assertNull(vm.state.value.resumableDraft)
        }

    // ── P1.9: rejection reason + resubmit-after-rejection ──────────────────────

    @Test
    fun `EXP-007 is seeded REJECTED with a real rejection reason`() {
        val rejected = ExpenseRepository().getById("EXP-007")
        assertNotNull(rejected)
        assertEquals(ExpenseStatus.REJECTED, rejected.status)
        val reason = rejected.rejectionReason
        assertNotNull(reason)
        assertTrue(reason.isNotBlank())
    }

    @Test
    fun `OpenDetail on a rejected record surfaces its rejection reason`() {
        val vm = viewModel()
        vm.onAction(ExpenseAction.OpenDetail("EXP-007"))
        val detail = vm.state.value.detailState
        assertTrue(detail is ScreenState.Content<ExpenseRecord>)
        val record = (detail as ScreenState.Content<ExpenseRecord>).data
        assertEquals(ExpenseStatus.REJECTED, record.status)
        assertNotNull(record.rejectionReason)
    }

    @Test
    fun `non-rejected records have no rejection reason`() {
        val repository = ExpenseRepository()
        val approved = repository.getById("EXP-001")
        assertNotNull(approved)
        assertEquals(ExpenseStatus.APPROVED, approved.status)
        assertNull(approved.rejectionReason)
    }

    // P2.1/P2.2/P2.3 bulk expense entry grid coverage lives in ExpenseBulkEntryViewModelTest.kt
    // (split out to keep this class focused — detekt LargeClass).
}
