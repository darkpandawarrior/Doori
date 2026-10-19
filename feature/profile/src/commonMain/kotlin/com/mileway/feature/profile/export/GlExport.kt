package com.mileway.feature.profile.export

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.GlMappingEntity
import com.mileway.core.network.accounting.AccountingExportLine

/** Fixed gl_mappings keys for line types that carry no category. */
const val GL_KEY_MILEAGE = "MILEAGE"
const val GL_KEY_PER_DIEM = "PER_DIEM"

/** Key used for an expense whose category is blank, so it is listed and can be mapped. */
const val GL_KEY_UNCATEGORIZED = "UNCATEGORIZED"

/** Only reports past approval are exported. */
val GL_EXPORTABLE_STATES: Set<ReportLifecycleState> =
    setOf(ReportLifecycleState.APPROVED, ReportLifecycleState.APPROVED_FOR_PAYMENT, ReportLifecycleState.PAID)

const val GL_CSV_HEADER = "report_id,line_id,type,category_key,gl_account,cost_center,amount_minor,currency"

/** A line in an exportable report, with the mapping key it posts under. Advances are not exported. */
data class GlLine(
    val reportId: String,
    val lineId: String,
    val type: String,
    val key: String,
    val amountMinor: Long,
    val currency: String,
)

/** A line with no usable GL code. The export is blocked while any exist. */
data class UnmappedLine(
    val reportId: String,
    val lineId: String,
    val key: String,
    val amountMinor: Long,
    val currency: String,
)

/** The gl_mappings key of a line, or null for a line type that is not exported. */
fun glKey(line: ClaimLine): String? =
    when (line) {
        is ExpenseLine -> line.category.trim().ifEmpty { GL_KEY_UNCATEGORIZED }
        is MileageLine -> GL_KEY_MILEAGE
        is PerDiemLine -> GL_KEY_PER_DIEM
        is AdvanceLine -> null
    }

private fun ClaimLine.typeName(): String =
    when (this) {
        is ExpenseLine -> "expense"
        is MileageLine -> "mileage"
        is PerDiemLine -> "per_diem"
        is AdvanceLine -> "advance"
    }

/** Every exportable line, in report then line order. */
fun glLines(reports: List<Report>): List<GlLine> =
    reports
        .filter { it.state in GL_EXPORTABLE_STATES }
        .flatMap { report ->
            report.lines.mapNotNull { line ->
                glKey(line)?.let { GlLine(report.id, line.id, line.typeName(), it, line.amountMinor, line.currency) }
            }
        }

private fun Map<String, GlMappingEntity>.codeFor(key: String): GlMappingEntity? = this[key]?.takeIf { it.glAccountCode.isNotBlank() }

/** Pure validation: the lines whose key has no non-blank GL code in [mappings]. */
fun unmappedLines(
    lines: List<GlLine>,
    mappings: Map<String, GlMappingEntity>,
): List<UnmappedLine> =
    lines
        .filter { mappings.codeFor(it.key) == null }
        .map { UnmappedLine(it.reportId, it.lineId, it.key, it.amountMinor, it.currency) }

/** The coded lines, or null while any line is unmapped (the export is blocked). */
fun codedLines(
    lines: List<GlLine>,
    mappings: Map<String, GlMappingEntity>,
): List<AccountingExportLine>? {
    if (lines.isEmpty() || unmappedLines(lines, mappings).isNotEmpty()) return null
    return lines.map { line ->
        val mapping = requireNotNull(mappings.codeFor(line.key))
        AccountingExportLine(
            reportId = line.reportId,
            lineId = line.lineId,
            type = line.type,
            categoryKey = line.key,
            glAccountCode = mapping.glAccountCode,
            costCenter = mapping.costCenter,
            amountMinor = line.amountMinor,
            currency = line.currency,
        )
    }
}

/** CSV with integer minor units and a currency column. Line-level fields only: no coordinates. */
fun glCsv(lines: List<AccountingExportLine>): String =
    buildString {
        append(GL_CSV_HEADER)
        lines.forEach { line ->
            append('\n')
            append(
                listOf(
                    csvText(line.reportId),
                    csvText(line.lineId),
                    csvText(line.type),
                    csvText(line.categoryKey),
                    csvText(line.glAccountCode),
                    csvText(line.costCenter.orEmpty()),
                    line.amountMinor.toString(),
                    csvText(line.currency),
                ).joinToString(","),
            )
        }
    }

/** Quotes when needed and defuses spreadsheet formulas by prefixing a single quote. */
internal fun csvText(raw: String): String {
    val safe = if (raw.isNotEmpty() && raw[0] in "=+-@\t\r") "'$raw" else raw
    return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}
