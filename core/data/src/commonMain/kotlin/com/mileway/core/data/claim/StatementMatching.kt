package com.mileway.core.data.claim

import com.mileway.core.data.dao.StatementImportDao
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.StatementImportEntity

/** Checks the persisted batch receipt without exposing Room to feature modules. */
suspend fun ReportRepository.wasStatementImported(id: String): Boolean = statementImports.get(id) != null

/** Saves the import receipt and all matches in one Room transaction, with version checks. */
suspend fun ReportRepository.saveStatementMatches(
    batch: StatementImportEntity,
    snapshots: List<Report>,
    matches: Map<String, ExpenseLine>,
    imports: StatementImportDao = statementImports,
): Boolean {
    val saved =
        atomic {
            if (imports.get(batch.id) != null) return@atomic null
            require(matches.keys.all { id -> snapshots.any { report -> report.lines.any { it.id == id } } })
            val used = imports.matchedTransactionIds().toSet()
            require(
                matches.values
                    .map { it.cardMatchId }
                    .distinct()
                    .size == matches.size,
            )
            require(matches.values.all { it.cardMatchId != null && it.cardMatchId !in used }) { "Statement transaction already matched" }
            val changed =
                snapshots.filter { report -> report.lines.any { it.id in matches } }.map { report ->
                    require(report.state in setOf(ReportLifecycleState.DRAFT, ReportLifecycleState.SENT_BACK, ReportLifecycleState.RECALLED))
                    report.lines.forEach { original -> matches[original.id]?.let { validateStatementMatch(original, it) } }
                    write(report.copy(lines = report.lines.map { matches[it.id] ?: it }))
                }
            imports.upsert(batch)
            changed
        } ?: return false
    saved.forEach { enqueue(it) }
    return true
}
