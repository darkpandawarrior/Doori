package com.mileway.core.ui.text

/**
 * Short month names, shared.
 *
 * This existed as `private val MONTHS = arrayOf("Jan", …)` in sixteen files across five feature
 * modules — a fresh array per file, three of them reallocated on every recomposition because they
 * were declared inside the composable. One list, read-only, allocated once.
 *
 * Deliberately takes an `Int` rather than a `LocalDateTime`: `kotlinx-datetime` is an
 * `implementation` dependency of `:core:ui`, so keeping the type out of the signature means no
 * consumer inherits a transitive API surface for three words of formatting.
 *
 * ponytail: English-only, matching every call site it replaced. Upgrade path: when Mileway ships a
 * second locale this becomes a `stringArrayResource` lookup, and it is now one place to change.
 */
private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Short name for a **1-based** [monthNumber], as `kotlinx.datetime.LocalDateTime.monthNumber`
 * reports it. Throws on anything outside 1..12 — a month number out of range is a bug upstream,
 * not something to paper over with a blank string.
 */
fun monthName(monthNumber: Int): String = MonthNames[monthNumber - 1]
