package com.mileway.core.common

import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

// KMP-safe formatting helpers (no JVM-only `String.format` / `java.util.Date`), absorbed from the
// openMF KMP template's `FormatNumber`/`FormatDate` approach so UI code can live in commonMain.

/** Two-digit zero-padded string for an Int (e.g. 5 -> "05"). */
fun Int.pad2(): String = if (this in 0..9) "0$this" else this.toString()

/** Decimal formatting without `String.format`. Rounds half-up to [places] digits. */
fun Double.formatDecimal(places: Int): String {
    if (places <= 0) return this.roundToLong().toString()
    var factor = 1L
    repeat(places) { factor *= 10 }
    val negative = this < 0
    val scaled = abs(this * factor).roundToLong()
    val intPart = scaled / factor
    val fracPart = (scaled % factor).toString().padStart(places, '0')
    return (if (negative && (intPart != 0L || fracPart.any { it != '0' })) "-" else "") + "$intPart.$fracPart"
}

/** Whole-number thousands-comma grouping, no `String.format` (e.g. 12345.6 -> "12,345"). */
fun Double.formatGrouped(): String {
    val whole = this.roundToLong()
    val grouped = abs(whole).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (whole < 0) "-$grouped" else grouped
}

/** 24-hour time, e.g. (9, 5) -> "09:05". */
fun formatTime24h(
    hour: Int,
    minute: Int,
): String = "${hour.pad2()}:${minute.pad2()}"

/** 12-hour time with AM/PM, e.g. (13, 5) -> "1:05 PM". */
fun formatTime12h(
    hour: Int,
    minute: Int,
): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 =
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    return "$h12:${minute.pad2()} $period"
}

private val shortMonthNames =
    listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

/** Friendly date, e.g. "Jun 19, 2026". */
fun LocalDate.toFriendlyString(): String = "${shortMonthNames[month.ordinal]} $day, $year"
