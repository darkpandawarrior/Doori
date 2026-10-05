package com.mileway.feature.logging.policy

import com.mileway.core.network.holiday.HolidayCheck

/** Advisory only: callers must not gate submission or money math on this check. */
class HolidayFlagUseCase(
    private val check: suspend (String, String) -> HolidayCheck = { _, _ -> HolidayCheck.Offline },
) {
    suspend operator fun invoke(date: String, countryCode: String): HolidayCheck = check(date, countryCode)
}

/** Shows the check's scope and the provider's own date, including gaps and regional uncertainty. */
fun HolidayCheck.reviewMessage(date: String, countryCode: String): String {
    val country = if (countryCode == "IN") "India (IN)" else countryCode
    val scope = "$country on $date (Nager.Date)"
    return when (this) {
        is HolidayCheck.Holiday ->
            "Holiday check: review public holiday for $scope: " +
                holidays.joinToString("; ") {
                    val region = if (it.global) "" else " (regional: ${it.counties?.joinToString() ?: "scope unavailable"}; confirm claim location)"
                    "${it.name}, ${it.date}$region"
                } + ". Advisory only; amount unchanged."
        HolidayCheck.NotHoliday -> "Holiday check: no public holiday for $scope"
        HolidayCheck.NoData -> "Holiday check: no public-holiday data for $scope"
        HolidayCheck.Offline -> "Holiday check: unavailable/offline for $scope; review manually"
        HolidayCheck.InvalidInput -> "Holiday check: enter a valid date and two-letter country code"
    }
}
