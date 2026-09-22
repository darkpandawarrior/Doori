package com.mileway.core.data.settings

private const val BytesPerKb = 1_024L
private const val BytesPerMb = BytesPerKb * BytesPerKb

/** One decimal place, so the rounding scale is 10 — named because `* 10` and `/ 10` are not obvious. */
private const val OneDecimalScale = 10L

/**
 * PLAN_V22 P6.6: KMP-safe (no JVM-only `String.format`) formatter for
 * [com.mileway.core.data.settings.StorageRepository]'s byte counts, shown on Preferences' Storage
 * tile/sheet subtitle.
 */
fun formatStorageBytes(bytes: Long): String =
    when {
        bytes >= BytesPerMb -> "${oneDecimal(bytes, BytesPerMb)} MB"
        bytes >= BytesPerKb -> "${oneDecimal(bytes, BytesPerKb)} KB"
        else -> "$bytes B"
    }

/** Rounds [bytes] / [divisor] to one decimal place (half-up) without `String.format`. */
private fun oneDecimal(
    bytes: Long,
    divisor: Long,
): String {
    val tenths = kotlin.math.round(bytes / divisor.toDouble() * OneDecimalScale).toLong()
    return "${tenths / OneDecimalScale}.${tenths % OneDecimalScale}"
}
