package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P1.5: persists a single in-progress [com.mileway.feature.logging.viewmodel.ExpenseFormState]
 * so `ExpenseAction.SaveDraft` survives app kill/relaunch. Mirrors the form's fields 1:1 rather
 * than reusing `ExpenseRecord` — a draft is not yet a submitted record (no id/status/date) and
 * `category`/`amountText` stay as the raw, possibly-invalid strings the form held, so resuming
 * restores exactly what the user typed rather than a validated/parsed shape.
 *
 * Single-row table by design: Mileway's expense entry flow is one linear wizard, not the
 * multi-row bulk grid P2.1 adds later, so there is at most one draft in flight at a time. A
 * fixed [draftId] keeps `upsert` idempotent without needing a lookup query first.
 */
@Entity(tableName = "draft_expenses")
data class DraftExpenseEntity(
    @PrimaryKey
    val draftId: String = SINGLETON_ID,
    val categoryName: String?,
    val amountText: String,
    val merchantName: String,
    val note: String,
    val receiptImagePath: String?,
    val updatedAt: Long,
    /** P27.E.15: the currency the amount was entered in (static local table only — no live FX). */
    val currencyCode: String = "INR",
) {
    companion object {
        const val SINGLETON_ID: String = "expense_draft_singleton"
    }
}
