package com.mileway.core.forms.field

import com.mileway.core.data.domain.claim.Attendee

/** Rejects blank or repeated names so each entered attendee contributes exactly one head. */
fun attendeeError(names: List<String>): String? =
    when {
        names.any { it.isBlank() } -> "Enter a name for every attendee"
        names.map { it.trim().lowercase() }.distinct().size != names.size -> "Each attendee must appear once"
        else -> null
    }

/** Converts validated attendee input into the typed claim payload. */
fun attendeeDetails(names: List<String>): List<Attendee> {
    require(attendeeError(names) == null)
    return names.map { Attendee(it.trim()) }
}

/** Ceil to one minor unit for display; policy evaluation compares the unrounded rational amount. */
fun perHeadAmountMinor(
    totalMinor: Long,
    count: Int,
): Long? = if (count <= 0 || totalMinor < 0) null else totalMinor / count + if (totalMinor % count == 0L) 0 else 1
