package com.mileway.feature.logging.perdiem

import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.policy.PerDiemRateTable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/** Maximum inclusive calendar-day range accepted by per-diem capture. */
const val MaxPerDiemRangeDays = 31

/** Calendar-day generation; rates are resolved at the start of each day in the capture timezone. */
object BatchDayRangeGenerator {
    data class SkippedDay(
        val date: LocalDate,
        val reason: String,
    )

    data class Result(
        val lines: List<PerDiemLine> = emptyList(),
        val skipped: List<SkippedDay> = emptyList(),
        val error: String? = null,
    )

    fun generate(
        start: LocalDate,
        end: LocalDate,
        table: PerDiemRateTable,
        currency: String,
        timeZone: TimeZone,
        batchId: String,
    ): Result {
        val length = end.toEpochDays().toLong() - start.toEpochDays() + 1
        if (length <= 0) return Result(error = "End date must be on or after start date")
        if (length > MaxPerDiemRangeDays) return Result(error = "Choose at most $MaxPerDiemRangeDays days")
        if (currency.isBlank()) return Result(error = "Choose a rate card with a currency")
        val lines = mutableListOf<PerDiemLine>()
        val skipped = mutableListOf<SkippedDay>()
        repeat(length.toInt()) { offset ->
            val date = LocalDate.fromEpochDays(start.toEpochDays() + offset)
            val atMillis = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val effective = table.rates.any { it.effectiveFrom <= atMillis }
            val rate = if (effective) table.rateFor(atMillis) else 0L
            when {
                !effective -> skipped += SkippedDay(date, "No per-diem rate in effect on this day")
                rate <= 0 -> skipped += SkippedDay(date, "The daily rate must be positive")
                else -> lines += PerDiemLine("$batchId:$date", rate, currency, days = 1, dailyRateMinor = rate, incurredOn = date.toString())
            }
        }
        return Result(lines, skipped)
    }
}
