package com.mileway.feature.logging.policy

import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.network.holiday.HolidayCheck
import com.mileway.core.network.holiday.PublicHoliday
import com.mileway.core.ui.mvi.dataOrNull
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseRecord
import com.mileway.feature.logging.model.ExpenseStatus
import com.mileway.feature.logging.model.formatExpenseAmount
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.viewmodel.ExpenseAction
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HolidayReviewTest {
    @Test
    fun countryIsExplicitAndTheHolidayFlagNeverMutatesTheClaim() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val records = ExpenseRepository()
                val record = foreignRecord()
                records.insert(record)
                val holiday = HolidayCheck.Holiday(listOf(PublicHoliday("2026-04-03", "Good Friday", "GB", true, types = listOf("Public"))))
                val calls = mutableListOf<Pair<String, String>>()
                val vm =
                    ExpenseViewModel(
                        records,
                        holidayFlags =
                            HolidayFlagUseCase { date, country ->
                                calls += date to country
                                if (country == "IN") HolidayCheck.NoData else holiday
                            },
                    )
                vm.onAction(ExpenseAction.OpenDetail(record.id))
                assertEquals(HolidayCheck.NoData, vm.state.value.holidayCheck)
                assertTrue(HolidayCheck.NoData.reviewMessage("2026-04-03", "IN").contains("no public-holiday data for India"))
                vm.onAction(ExpenseAction.CheckHoliday(" gb "))
                assertEquals(holiday, vm.state.value.holidayCheck)
                assertEquals(listOf("2026-04-03" to "IN", "2026-04-03" to "GB"), calls)
                assertEquals(record, records.getById(record.id))
                assertEquals(record, vm.state.value.detailState.dataOrNull)
                assertEquals(emptyList(), vm.state.value.lastSubmissionViolations)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun allFallbacksAreVisibleAndRegionalFlagsAskForLocationConfirmation() {
        val date = "2026-01-02"
        assertTrue(HolidayCheck.NotHoliday.reviewMessage(date, "GB").contains("no public holiday"))
        assertTrue(HolidayCheck.Offline.reviewMessage(date, "GB").contains("unavailable/offline"))
        assertTrue(HolidayCheck.InvalidInput.reviewMessage(date, "bad").contains("valid date"))
        val regional = HolidayCheck.Holiday(listOf(PublicHoliday(date, "2 January", "GB", false, listOf("GB-SCT"), listOf("Public"))))
        val message = regional.reviewMessage(date, "GB")
        assertTrue(message.contains("GB-SCT; confirm claim location"))
        assertTrue(message.contains("2 January, 2026-01-02"))
        assertTrue(message.contains("Advisory only; amount unchanged"))
    }

    @Test
    fun legacyMoneyDisplayRoundsFractionalCentsAndSavedMinorUnitsWin() {
        for ((major, minor) in listOf(0.29 to 29L, 4.35 to 435L)) {
            assertEquals(formatMinorCurrency(minor, "USD"), formatExpenseAmount(major, "USD"))
        }
        assertEquals(formatMinorCurrency(29, "USD"), formatExpenseAmount(4.35, "USD", amountMinor = 29))
    }

    @Test
    fun fxLineShowsProviderDateInsteadOfPinTimeAndNeverInventsAnOfflineRate() {
        val record = foreignRecord()
        assertEquals(record.fxRate?.description(), record.fxProvenanceLabel())
        assertTrue(record.fxProvenanceLabel()!!.contains("rate date 2026-04-02"))
        assertTrue(record.fxProvenanceLabel()!!.contains("Frankfurter"))
        assertTrue(record.copy(fxRate = null).fxProvenanceLabel()!!.contains("unavailable/offline"))
        assertTrue(record.copy(fxRatePinnedAt = null).fxProvenanceLabel()!!.contains("No FX pin"))
        assertNull(record.copy(currencyCode = "INR").fxProvenanceLabel())
        val manual = record.copy(fxRate = FxRate(85.0, "USD", source = FxRateSource.MANUAL_APPROXIMATE))
        assertTrue(manual.fxProvenanceLabel()!!.contains("Manual (approximate)"))
        assertTrue(manual.fxProvenanceLabel()!!.contains("rate date unavailable (manual)"))
    }

    private fun foreignRecord() =
        ExpenseRecord(
            id = "holiday-foreign",
            category = ExpenseCategory.FOOD,
            merchantName = "Cafe",
            amountRupees = 10.0,
            status = ExpenseStatus.PENDING,
            dateMs = Instant.parse("2026-04-03T12:00:00Z").toEpochMilliseconds(),
            currencyCode = "USD",
            amountMinor = 1_000,
            fxRate = FxRate(90.0, "USD", sourceDate = "2026-04-02"),
            fxRatePinnedAt = Instant.parse("2026-04-05T12:00:00Z").toEpochMilliseconds(),
        )
}
