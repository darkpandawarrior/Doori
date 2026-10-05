package com.mileway.feature.cards.import

import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.FxRate
import com.mileway.core.data.domain.claim.FxRateSource
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.StatementImportEntity
import com.mileway.core.data.session.sha256Hex
import com.mileway.core.forms.ExpenseFieldContext
import com.mileway.core.forms.field.PercentageSplitInput
import com.mileway.core.forms.field.percentageAllocations
import com.mileway.core.network.fx.FxRatePinner
import com.mileway.feature.cards.match.CardMatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Local I/O seams keep matching tests independent of Room and platform settings. */
class StatementImporter(
    private val loadReports: suspend (String) -> List<Report>,
    private val batchExists: suspend (String) -> Boolean,
    private val saveMatches: suspend (StatementImportEntity, List<Report>, Map<String, ExpenseLine>) -> Boolean,
    private val pinner: FxRatePinner = FxRatePinner(),
    private val matcher: CardMatcher = CardMatcher(),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()

    data class RowResult(
        val rowId: String,
        val lineId: String? = null,
        val reason: String? = null,
    )

    data class Result(
        val rows: List<RowResult>,
        val alreadyImported: Boolean = false,
    ) {
        val matched: Int get() = rows.count { it.lineId != null }
    }

    suspend fun import(
        employeeId: String,
        fileName: String,
        text: String,
    ): Result =
        mutex.withLock {
            require(employeeId.isNotBlank() && fileName.isNotBlank()) { "Sign in and supply a statement name" }
            val parsed = StatementParser.parse(text)
            val batchId = "statement:${sha256Hex("$employeeId:$text") }"
            if (batchExists(batchId)) return@withLock Result(emptyList(), alreadyImported = true)
            val reports = loadReports(employeeId)
            require(reports.all { it.employeeId == employeeId }) { "Statement cannot match another employee's claims" }
            val used = reports.flatMap { it.lines }.mapNotNull { it.cardMatchId }.toSet()
            val candidates =
                reports
                    .filter { it.state in EditableStates }
                    .flatMap { it.lines }
                    .filterIsInstance<ExpenseLine>()
                    .toMutableList()
            val updates = mutableMapOf<String, ExpenseLine>()
            val outcomes =
                parsed.rows.map { row ->
                    val transactionId = "card:${sha256Hex("$employeeId:${row.id}") }"
                    when {
                        transactionId in used -> RowResult(row.id, reason = "Transaction already matched")
                        else -> matchRow(row, transactionId, candidates, updates)
                    }
                }
            val batch = StatementImportEntity(batchId, parsed.source, fileName, nowMillis(), parsed.rows.size, "DONE:${updates.size}")
            val saved = saveMatches(batch, reports, updates)
            Result(if (saved) outcomes else emptyList(), alreadyImported = !saved)
        }

    private suspend fun matchRow(
        row: StatementRow,
        transactionId: String,
        candidates: MutableList<ExpenseLine>,
        updates: MutableMap<String, ExpenseLine>,
    ): RowResult {
        val result = matcher.match(row, candidates)
        val line = result.line ?: return RowResult(row.id, reason = result.reason)
        if (line.currency != "INR" && row.currency != "INR") {
            return RowResult(row.id, reason = "Foreign card match requires billed INR for the card rate")
        }
        val matched = line.copy(cardMatchId = transactionId)
        val pinned =
            if (line.currency == "INR") {
                matched
            } else {
                pinner.pin(
                    matched,
                    cardRate =
                        FxRate(
                            row.amountMinor.toDouble() / line.amountMinor,
                            line.currency,
                            sourceDate = row.postingDate,
                            source = FxRateSource.CARD_MATCHED,
                        ),
                )
            }
        val context = ExpenseFieldContext(pinned.amountMinor, pinned.currency, cardMatchedAmountMinor = pinned.amountMinor)
        val splits =
            if (pinned.splits.isEmpty()) {
                emptyList()
            } else {
                percentageAllocations(
                    context.anchorAmountMinor,
                    pinned.splits.map {
                        PercentageSplitInput(
                            it.target,
                            it.targetId,
                            (it.percentageBasisPoints / 100).toString() + "." + (it.percentageBasisPoints % 100).toString().padStart(2, '0'),
                        )
                    },
                ) ?: return RowResult(row.id, reason = "Existing splits cannot reconcile to the matched anchor")
            }
        updates[line.id] = pinned.copy(splits = splits)
        candidates.removeAll { it.id == line.id }
        return RowResult(row.id, line.id)
    }

    private companion object {
        val EditableStates = setOf(ReportLifecycleState.DRAFT, ReportLifecycleState.RECALLED, ReportLifecycleState.SENT_BACK)
    }
}
