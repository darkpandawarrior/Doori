package com.mileway.core.data.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/** Latitude runs -90..90 and longitude -180..180; anything outside is not a coordinate. */
private const val MaxLatitude = 90.0
private const val MaxLongitude = 180.0

/** Metres in a kilometre. Shared across core:data so each file does not spell 1000 again. */
internal const val MetresPerKm = 1_000.0
private const val MinutesPerHour = 60

/** `roundToTwoDecimals` scales by this, rounds, and scales back. */
private const val TwoDecimalFactor = 100.0

private const val DecimalBase = 10.0

object CommonUtils {
    fun formatDistance(distanceKm: Double): String = "${distanceKm.fmt1d()} km"

    fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / MillisPerMinute
        return if (minutes < MinutesPerHour) {
            "${minutes}m"
        } else {
            "${minutes / MinutesPerHour}h ${minutes % MinutesPerHour}m"
        }
    }

    fun formatSpeed(speedKmh: Double): String = "${speedKmh.fmt1d()} km/h"

    fun roundToTwoDecimals(value: Double): Double = (value * TwoDecimalFactor).roundToInt() / TwoDecimalFactor

    fun toTitleCase(input: String): String =
        input.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

    fun capitalizeFirstLetter(input: String): String = if (input.isEmpty()) input else input[0].uppercase() + input.substring(1)

    fun formatCurrencyAmount(
        amount: Double,
        currencySymbol: String = "₹",
    ): String = "$currencySymbol${amount.fmt2d()}"

    fun roundValueUsingFormatter(
        value: Double,
        decimalPlaces: Int = 2,
    ): Double {
        val factor = DecimalBase.pow(decimalPlaces)
        return (value * factor).roundToInt() / factor
    }

    fun formatDistanceMeters(meters: Double): String = if (meters < MetresPerKm) "${meters.roundToInt()}m" else "${(meters / MetresPerKm).fmt1d()} km"

    fun metersToKm(meters: Double): Double = roundToTwoDecimals(meters / MetresPerKm)

    fun kmToMeters(km: Double): Double = km * MetresPerKm

    fun isValidLatLng(
        lat: Double,
        lng: Double,
    ): Boolean = abs(lat) <= MaxLatitude && abs(lng) <= MaxLongitude && !(lat == 0.0 && lng == 0.0)
}
