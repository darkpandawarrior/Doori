package com.mileway.core.data.domain.policy

import kotlinx.serialization.Serializable
import kotlin.time.Instant

private const val ISO_DATE_LENGTH = 10
private const val YEAR_DIGITS = 4
private const val RATE_SCALE = 1_000L
private const val HALF_RATE_SCALE = RATE_SCALE / 2

/** Unit of whole-distance inputs. No implicit conversion between miles and kilometres is performed. */
@Serializable
enum class MileageDistanceUnit { MILE, KILOMETRE }

/** Exact nonnegative multiplication for money and distance, rejecting overflow. */
internal fun checkedRateProduct(
    left: Long,
    right: Long,
): Long {
    require(left >= 0 && right >= 0) { "Money and distance must be nonnegative" }
    require(right == 0L || left <= Long.MAX_VALUE / right) { "Rate multiplication overflow" }
    return left * right
}

/** Exact nonnegative addition, rejecting overflow. */
internal fun checkedRateSum(
    left: Long,
    right: Long,
): Long {
    require(left >= 0 && right >= 0 && left <= Long.MAX_VALUE - right) { "Rate addition overflow" }
    return left + right
}

/** Rounds thousandths of a minor unit half-up once, after all distance bands have been added. */
internal fun roundRateMinor(thousandthsMinor: Long): Long {
    val minor = thousandthsMinor / RATE_SCALE
    val increment = if (thousandthsMinor % RATE_SCALE >= HALF_RATE_SCALE) 1L else 0L
    return minor + increment
}

/** Strict ISO date, represented as midnight UTC for comparison with submission epoch millis. */
internal fun rateDateMillis(date: String): Long {
    require(date.length == ISO_DATE_LENGTH) { "Rate dates must use yyyy-MM-dd" }
    val instant = Instant.parse("${date}T00:00:00Z")
    require(instant.toString().substringBefore('T') == date) { "Invalid rate date" }
    return instant.toEpochMilliseconds()
}

/**
 * Rates are thousandths of a minor currency unit per [distanceUnit], for example 72.5 cents is 72_500.
 * Without a band boundary this is a flat rate. A band boundary requires an above-band rate.
 * Distance inputs are whole units; callers must explicitly convert their recorded distance first.
 */
@Serializable
data class AnnualDistanceStepDown(
    val firstRateThousandthsMinor: Long,
    val distanceUnit: MileageDistanceUnit = MileageDistanceUnit.MILE,
    val firstBandDistanceUnits: Long? = null,
    val aboveBandRateThousandthsMinor: Long? = null,
) {
    init {
        require(firstRateThousandthsMinor >= 0) { "Mileage rate must be nonnegative" }
        require((firstBandDistanceUnits == null) == (aboveBandRateThousandthsMinor == null)) { "A band needs both a boundary and a rate" }
        firstBandDistanceUnits?.let { require(it > 0) { "Band distance must be positive" } }
        aboveBandRateThousandthsMinor?.let {
            require(it in 0..firstRateThousandthsMinor) { "Above-band rate must be a nonnegative step down" }
        }
    }

    /** Splits a crossing trip and rounds the total half-up once to minor units. Rejects overflow. */
    fun amountMinor(
        distanceUnits: Long,
        annualDistanceBeforeUnits: Long = 0,
    ): Long {
        checkedRateSum(annualDistanceBeforeUnits, distanceUnits)
        val boundary = firstBandDistanceUnits
        val firstDistance = if (boundary == null) distanceUnits else minOf(distanceUnits, (boundary - annualDistanceBeforeUnits).coerceAtLeast(0))
        val firstAmount = checkedRateProduct(firstDistance, firstRateThousandthsMinor)
        val aboveAmount = checkedRateProduct(distanceUnits - firstDistance, aboveBandRateThousandthsMinor ?: firstRateThousandthsMinor)
        return roundRateMinor(checkedRateSum(firstAmount, aboveAmount))
    }
}

/** Calendar periods used by the cited mileage mirrors. Dates are evaluated in UTC. */
@Serializable
enum class MileageAnnualPeriod(
    val startsOn: String,
) {
    CALENDAR_YEAR("01-01"),
    UK_TAX_YEAR("04-06"),
    ;

    /** Inclusive start date of the annual period containing [submittedAtMillis]. */
    fun startFor(submittedAtMillis: Long): String {
        val date = Instant.fromEpochMilliseconds(submittedAtMillis).toString().substringBefore('T')
        rateDateMillis(date)
        val year = date.substringBefore('-').toInt() - if (date.substringAfter('-') < startsOn) 1 else 0
        return "${year.toString().padStart(YEAR_DIGITS, '0')}-$startsOn"
    }
}

/** Cumulative business distance across all vehicles, tagged with its annual period start date. */
@Serializable
data class AnnualMileageDistance(
    val periodStart: String,
    val distanceUnits: Long,
) {
    init {
        rateDateMillis(periodStart)
        require(distanceUnits >= 0) { "Annual distance must be nonnegative" }
    }
}
