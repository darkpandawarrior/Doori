package com.mileway.feature.logging.import

import com.mileway.feature.logging.catalog.ExpenseCategoryCatalog
import com.mileway.feature.logging.model.DraftStatus
import com.mileway.feature.logging.model.ExpenseCategory
import com.mileway.feature.logging.model.ExpenseDraftRow

/**
 * P2.4: pure, offline CSV/TSV bulk-import parser for the bulk expense entry grid (P2.1). The
 * reference app's bulk import leans on live `services.segregated`/`logMiles.limit`/
 * `geocode.reverse` APIs to resolve/validate each row server-side; Mileway's is a fully local
 * equivalent since every one of those lookups already has a local counterpart
 * ([ExpenseCategoryCatalog] from P1.1). No Android/Compose imports here — stays commonMain-only so
 * it can also back a Wear OS or iOS import flow later without change.
 */
object ExpenseCsvImporter {
    private val HEADER_ALIASES =
        mapOf(
            "category" to Column.CATEGORY,
            "amount" to Column.AMOUNT,
            "merchant" to Column.MERCHANT,
            "note" to Column.NOTE,
        )

    private enum class Column { CATEGORY, AMOUNT, MERCHANT, NOTE }

    /**
     * Parses [text] as a CSV or TSV table (delimiter auto-detected from the header line: tab if
     * present, else comma) into one [ExpenseDraftRow] per data line. Expects a `category,amount,
     * merchant,note` header (case-insensitive, any column order; `note` is optional). A malformed
     * row (unparsable amount, or a blank merchant) still produces a row — [DraftStatus.ERROR]
     * rather than a silently dropped line — so the caller can see exactly what needs fixing. An
     * unrecognized category string resolves to [ExpenseCategory.OTHER] rather than failing the row,
     * mirroring how the single-entry form treats an unset category as fixable, not fatal.
     *
     * Returns an empty list when [text] is blank or has no header line.
     */
    fun parse(text: String): List<ExpenseDraftRow> {
        val lines = text.lines().map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headerLine = lines.first()
        val delimiter = if (headerLine.contains('\t')) '\t' else ','
        val header = headerLine.split(delimiter).map { it.trim().lowercase() }
        val columnIndex: Map<Column, Int> =
            header.withIndex().mapNotNull { (index, name) -> HEADER_ALIASES[name]?.let { it to index } }.toMap()

        return lines.drop(1).mapIndexed { rowNumber, line ->
            val cells = line.split(delimiter).map { it.trim() }

            fun cell(column: Column): String = columnIndex[column]?.let { cells.getOrNull(it) }.orEmpty()

            val categoryText = cell(Column.CATEGORY)
            val category = resolveCategory(categoryText)
            val merchantName = cell(Column.MERCHANT)
            val amountText = cell(Column.AMOUNT)
            val amount = amountText.toDoubleOrNull()
            val isMalformed = amount == null || amount <= 0.0 || merchantName.isBlank()

            ExpenseDraftRow(
                id = "csv-row-$rowNumber",
                category = category,
                amountText = amountText,
                merchantName = merchantName,
                note = cell(Column.NOTE),
                status = if (isMalformed) DraftStatus.ERROR else DraftStatus.PENDING,
            )
        }
    }

    /** Resolves a free-text category cell against the catalog's enum name or label; [ExpenseCategory.OTHER] fallback. */
    private fun resolveCategory(rawValue: String): ExpenseCategory {
        if (rawValue.isBlank()) return ExpenseCategory.OTHER
        val normalized = rawValue.trim()
        return ExpenseCategoryCatalog
            .default()
            .map { it.category }
            .firstOrNull { it.name.equals(normalized, ignoreCase = true) || it.label.equals(normalized, ignoreCase = true) }
            ?: ExpenseCategory.OTHER
    }
}
