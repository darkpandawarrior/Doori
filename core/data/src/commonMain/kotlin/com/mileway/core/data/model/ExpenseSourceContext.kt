package com.mileway.core.data.model

/**
 * P25.A4.1: how an expense-entry flow was reached. Lives in `core:data` (not `feature:logging`)
 * so `feature:tracking`/`feature:events`/`feature:cards`/`feature:profile` can each construct one
 * and hand it to the expense flow without depending on `feature:logging` — the whole point of
 * putting a cross-feature linkage type in a shared module instead of the feature that consumes it.
 *
 * [None] and [Regular] both mean "no linkage," kept distinct because `core:ui`'s
 * `ExpenseContextSummaryCard` (P25.A5.2) early-returns on both but the expense flow can still
 * distinguish "opened blank" from "opened blank because a caller explicitly asked for a regular
 * entry." Every other variant carries the id(s) the entry flow needs to prefill/lock fields
 * against (filled in by V27's `ExpenseViewModel.openWithContext`).
 */
sealed class ExpenseSourceContext {
    /** No context was supplied at all (e.g. a bare deep link). */
    data object None : ExpenseSourceContext()

    /** A plain, unlinked expense entry — the default "Add Expense" CTA. */
    data object Regular : ExpenseSourceContext()

    /** Re-opening an already-submitted expense for editing. */
    data class Edit(val expenseId: String) : ExpenseSourceContext()

    /** Logging an expense against a completed trip. */
    data class Trip(val tripId: String) : ExpenseSourceContext()

    /** Logging an expense that draws down a trip's advance. */
    data class TripAdvance(val tripId: String, val advanceId: String) : ExpenseSourceContext()

    /** Logging an expense against an event. */
    data class Event(val eventId: String) : ExpenseSourceContext()

    /** Claiming a card transaction as an expense — fields get locked/capped against [transactionId]. */
    data class Card(val cardId: String, val transactionId: String) : ExpenseSourceContext()

    /** Logging an expense against a standalone (non-trip) advance. */
    data class Advance(val advanceId: String) : ExpenseSourceContext()

    /** Creating an expense from an attachment on an approval clarification chat message. */
    data class Message(val clarificationId: String, val attachmentUrl: String) : ExpenseSourceContext()

    /** Creating an expense from a scanned document's OCR result. */
    data class Scanner(val prefill: ScannerPrefill) : ExpenseSourceContext()
}

/**
 * A flattened, expense-entry-shaped snapshot of a `core:ai` `DocumentAnalysis` result — deliberately
 * NOT a dependency on `core:ai`'s `DocumentAnalysis` itself. `core:data` sits underneath nearly every
 * other module (see this module's dependents); `core:ai` sits underneath none today. Depending
 * downward from `core:data` onto a specific pipeline module inverts that and buys nothing an OCR
 * pass doesn't already produce as plain strings. V27 maps `DocumentAnalysis` → [ScannerPrefill] at
 * the scanner→expense-entry boundary instead. Mirrors [OdometerAnalysisSnapshot]'s precedent of
 * flattening another module's richer shape into a lightweight, dependency-free record here.
 *
 * Fields are raw/unvalidated strings (mirrors `DraftExpenseEntity.amountText`'s convention) since
 * the expense form re-validates on submit regardless of extraction confidence.
 */
data class ScannerPrefill(
    val merchant: String?,
    val amountText: String?,
    val currency: String?,
    val dateEpochMs: Long?,
    val category: String?,
    val overallConfidence: Float,
    val duplicateWarning: String?,
)
