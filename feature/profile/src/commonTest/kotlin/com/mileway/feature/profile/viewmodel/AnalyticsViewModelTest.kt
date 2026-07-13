package com.mileway.feature.profile.viewmodel

import com.mileway.core.platform.ShareSheet
import com.mileway.feature.profile.analytics.DateRangePreset
import com.mileway.feature.profile.analytics.LeaderboardSort
import com.mileway.stub.AnalyticsMockData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V29 P29.AN.1: covers the [AnalyticsViewModel] reducer that replaced both analytics screens'
 * `remember`-only mock-singleton reads — date-range/filter/leaderboard state and the P29.AN.7 real
 * export artifact + share call.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    private class FakeShareSheet : ShareSheet {
        var lastText: String? = null
        var lastSubject: String? = null

        override fun share(
            text: String,
            subject: String?,
            fileUri: String?,
        ) {
            lastText = text
            lastSubject = subject
        }
    }

    @Test
    fun `initial state is pre-populated from mock data, not empty`() {
        val vm = AnalyticsViewModel(FakeShareSheet())
        assertEquals(AnalyticsMockData.totalSpend, vm.state.value.totalSpend)
        assertTrue(vm.state.value.leaderboard.isNotEmpty())
        assertTrue(vm.state.value.insights.isNotEmpty())
    }

    @Test
    fun `date range change recomputes the windowed series`() =
        runTest {
            val vm = AnalyticsViewModel(FakeShareSheet())
            vm.onAction(AnalyticsAction.DateRangeChanged(DateRangePreset.LAST_7))
            advanceUntilIdle()
            assertEquals(7, vm.state.value.windowedSeries.size)
        }

    @Test
    fun `toggling a category filter narrows filteredActivity`() =
        runTest {
            val vm = AnalyticsViewModel(FakeShareSheet())
            val before = vm.state.value.filteredActivity.size
            vm.onAction(AnalyticsAction.CategoryToggled("Mileage"))
            advanceUntilIdle()
            val after = vm.state.value.filteredActivity
            assertTrue(after.size <= before)
            after.forEach { assertEquals("Mileage", it.category) }

            vm.onAction(AnalyticsAction.ClearFilters)
            advanceUntilIdle()
            assertEquals(before, vm.state.value.filteredActivity.size)
        }

    @Test
    fun `leaderboard sort and search both apply`() =
        runTest {
            val vm = AnalyticsViewModel(FakeShareSheet())
            vm.onAction(AnalyticsAction.LeaderboardSortChanged(LeaderboardSort.ALPHABETICAL))
            advanceUntilIdle()
            val names = vm.state.value.leaderboard.map { it.name }
            assertEquals(names.sorted(), names)

            vm.onAction(AnalyticsAction.LeaderboardQueryChanged("Aisha"))
            advanceUntilIdle()
            assertEquals(listOf("Aisha Khan"), vm.state.value.leaderboard.map { it.name })
        }

    @Test
    fun `opening a category detail populates detail series and merchants`() =
        runTest {
            val vm = AnalyticsViewModel(FakeShareSheet())
            vm.onAction(AnalyticsAction.OpenCategoryDetail("Travel"))
            advanceUntilIdle()
            assertTrue(vm.state.value.detailSeries.isNotEmpty())
            assertTrue(vm.state.value.detailMerchants.isNotEmpty())
        }

    @Test
    fun `selecting a merchant then searching narrows its transaction list`() =
        runTest {
            val vm = AnalyticsViewModel(FakeShareSheet())
            vm.onAction(AnalyticsAction.OpenCategoryDetail("Travel"))
            advanceUntilIdle()
            val merchant = vm.state.value.detailMerchants.first().name
            vm.onAction(AnalyticsAction.SelectMerchant(merchant))
            advanceUntilIdle()
            val all = vm.state.value.merchantTransactions
            assertTrue(all.isNotEmpty())

            vm.onAction(AnalyticsAction.MerchantSearchQueryChanged("no-such-id-xyz"))
            advanceUntilIdle()
            assertTrue(vm.state.value.merchantTransactions.isEmpty())
        }

    @Test
    fun `export generates a csv artifact and calls the real share sheet`() =
        runTest {
            val shareSheet = FakeShareSheet()
            val vm = AnalyticsViewModel(shareSheet)
            vm.onAction(AnalyticsAction.Export("Mileage"))
            advanceUntilIdle()

            assertEquals("Mileage", vm.state.value.exportedForCategory)
            assertTrue(vm.state.value.isExporting.not())
            assertNull(vm.state.value.exportError)
            assertTrue(shareSheet.lastText!!.contains("Mileage analytics export"))
            assertEquals("Mileage analytics export", shareSheet.lastSubject)
        }
}
