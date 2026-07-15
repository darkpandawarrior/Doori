package com.mileway.core.data.util

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

object CommonUtils {
    fun formatDistance(distanceKm: Double): String = "${distanceKm.fmt1d()} km"

    fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / 60_000
        return if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
    }

    fun formatSpeed(speedKmh: Double): String = "${speedKmh.fmt1d()} km/h"

    fun roundToTwoDecimals(value: Double): Double = (value * 100.0).roundToInt() / 100.0

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
        val factor = 10.0.pow(decimalPlaces)
        return (value * factor).roundToInt() / factor
    }

    fun formatDistanceMeters(meters: Double): String = if (meters < 1000) "${meters.roundToInt()}m" else "${(meters / 1000.0).fmt1d()} km"

    fun metersToKm(meters: Double): Double = roundToTwoDecimals(meters / 1000.0)

    fun kmToMeters(km: Double): Double = km * 1000.0

    fun isValidLatLng(
        lat: Double,
        lng: Double,
    ): Boolean = abs(lat) <= 90.0 && abs(lng) <= 180.0 && !(lat == 0.0 && lng == 0.0)
}
