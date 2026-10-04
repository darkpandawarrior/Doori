package com.mileway

import app.cash.turbine.test
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.SplitTarget
import com.mileway.core.data.session.SessionKind
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.session.SessionState
import com.mileway.core.forms.FormFieldValue
import com.mileway.core.forms.field.PercentageSplitInput
import com.mileway.core.forms.itemization.ItemizedLineInput
import com.mileway.feature.logging.catalog.ExpenseCustomFormCatalog
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.report.ReportGroupingViewModel
import com.mileway.feature.logging.report.ReportJourneyStore
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseEffect
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailedExpenseJourneyTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cardSplitsRejectAClaimBelowTheMatchedAnchor() =
        runTest {
            val vm = ExpenseViewModel(ExpenseRepository())
            vm.onAction(
                ExpenseAction.OpenWithContext(
                    com.mileway.core.data.model.ExpenseSourceContext.Card(
                        "card",
                        "match",
                        merchantName = "Cafe",
                        transactionAmountRupees = 100.0,
                    ),
                ),
            )
            vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
            vm.onAction(ExpenseAction.SetAmount("50.00"))
            vm.onAction(
                ExpenseAction.SetFormValue(
                    ExpenseCustomFormCatalog.SPLITS,
                    FormFieldValue.PercentageSplit(
                        listOf(
                            PercentageSplitInput(SplitTarget.PROJECT, "A", "100"),
                        ),
                    ),
                ),
            )
            vm.onAction(ExpenseAction.SubmitExpense)
            assertTrue(
                vm.state.value.form.errors
                    .containsKey(com.mileway.feature.logging.validation.ExpenseFormValidator.FIELD_AMOUNT),
            )
            assertEquals("", vm.state.value.lastSubmittedId)
        }

    @Test
    fun detailsSurviveSubmitEditGroupingAndThePersistedReportCodec() =
        runTest {
            val repository = ExpenseRepository()
            val vm = ExpenseViewModel(repository)
            vm.onAction(ExpenseAction.SelectCategory(ExpenseCategory.FOOD))
            vm.onAction(ExpenseAction.SetMerchant("New Cafe"))
            vm.onAction(ExpenseAction.SetAmount("10.01"))
            val splits =
                FormFieldValue.PercentageSplit(
                    listOf(
                        PercentageSplitInput(SplitTarget.COST_CENTER, "Sales", "50"),
                        PercentageSplitInput(SplitTarget.PERSON, "Alex", "50"),
                    ),
                )
            vm.onAction(ExpenseAction.SetFormValue(ExpenseCustomFormCatalog.SPLITS, splits))
            vm.onAction(ExpenseAction.SetFormValue(ExpenseCustomFormCatalog.ATTENDEES, FormFieldValue.AttendeeList(listOf("Alex", "Jordan"))))
            vm.onAction(
                ExpenseAction.SetFormValue(
                    ExpenseCustomFormCatalog.ITEMIZED,
                    FormFieldValue.ItemizedLines(
                        listOf(
                            ItemizedLineInput("Meals", "6.00"),
                            ItemizedLineInput("Tax", "4.01"),
                        ),
                    ),
                ),
            )
            advanceUntilIdle()
            vm.effect.test {
                vm.onAction(ExpenseAction.SaveDraft)
                assertTrue(awaitItem() is ExpenseEffect.ShowToast)
                advanceUntilIdle()
                assertEquals(null, repository.loadDraft())
                vm.onAction(ExpenseAction.SubmitExpense)
                advanceUntilIdle()
                assertTrue(awaitItem() is ExpenseEffect.NavigateToSuccess)
            }
            val id = vm.state.value.lastSubmittedId
            val record = repository.getById(id)!!
            assertEquals(1001L, record.amountMinor)
            assertEquals(1001L, record.splits.sumOf { it.amountMinor })
            assertEquals(2, record.attendees.size)
            assertEquals(1001L, record.itemized.sumOf { it.amountMinor })
            vm.onAction(ExpenseAction.OpenEdit(id))
            assertEquals(
                splits.entries.map {
                    it.targetId
                },
                (vm.state.value.form.formValues[ExpenseCustomFormCatalog.SPLITS] as FormFieldValue.PercentageSplit).entries.map { it.targetId },
            )
            repository.insert(ExpenseRecord("other", ExpenseCategory.FOOD, "Other Cafe", 2.0, ExpenseStatus.PENDING, record.dateMs))
            val stored = MutableStateFlow<List<Report>>(emptyList())
            val store =
                object : ReportJourneyStore {
                    override fun observe(id: String) = stored.map { rows -> rows.find { it.id == id } }

                    override fun observeByEmployee(employeeId: String) = stored

                    override suspend fun save(report: Report): Report {
                        val saved = Json.decodeFromString<Report>(Json.encodeToString(report))
                        stored.value = listOf(saved)
                        return saved
                    }

                    override suspend fun recall(id: String): Report = error("unused")
                }
            val session = mockk<SessionSource>()
            every { session.sessionState } returns MutableStateFlow(SessionState(kind = SessionKind.CREDENTIALS, employeeCode = "employee"))
            val grouping = ReportGroupingViewModel(repository, store, session)
            advanceUntilIdle()
            grouping.toggle(id)
            grouping.toggle("other")
            grouping.createReport()
            advanceUntilIdle()
            val line =
                stored.value
                    .single()
                    .lines
                    .filterIsInstance<ExpenseLine>()
                    .single { it.id == id }
            assertEquals(record.splits, line.splits)
            assertEquals(record.attendees, line.attendees)
            assertEquals(record.itemized, line.itemized)
        }
}
