package com.mileway.feature.profile.export

import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ClaimLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.GlMappingEntity

const val MILEAGE_GL_KEY = "MILEAGE"
const val PER_DIEM_GL_KEY = "PER_DIEM"
const val ADVANCE_GL_KEY = "ADVANCE"

data class GlExportLine(
    val reportId: String,
    val lineId: String,
    val key: String,
    val amountMinor: Long,
    val currency: String,
)

fun ClaimLine.glMappingKey(): String =
    when (this) {
        is ExpenseLine -> category
        is MileageLine -> MILEAGE_GL_KEY
        is PerDiemLine -> PER_DIEM_GL_KEY
        is AdvanceLine -> ADVANCE_GL_KEY
    }

/** Only reports ready for accounting enter validation, including every line in those reports. */
fun glExportLines(reports: List<Report>): List<GlExportLine> =
    reports
        .filter {
            it.state == ReportLifecycleState.APPROVED ||
                it.state == ReportLifecycleState.APPROVED_FOR_PAYMENT ||
                it.state == ReportLifecycleState.PAID
        }.flatMap { report ->
            report.lines.map { GlExportLine(report.id, it.id, it.glMappingKey(), it.amountMinor, it.currency) }
        }.sortedWith(compareBy({ it.reportId }, { it.lineId }))

fun unmappedGlLines(
    lines: List<GlExportLine>,
    mappings: List<GlMappingEntity>,
): List<GlExportLine> {
    val codes = mappings.associateBy { it.category }
    return lines.filter { codes[it.key]?.glAccountCode.isNullOrBlank() }
}

/** All-or-nothing CSV. No route, address, merchant or trip evidence is serialized. */
fun glExportCsv(
    lines: List<GlExportLine>,
    mappings: List<GlMappingEntity>,
): String? {
    if (lines.isEmpty() || unmappedGlLines(lines, mappings).isNotEmpty()) return null
    val codes = mappings.associateBy { it.category }
    val rows =
        lines.map { line ->
            val mapping = codes.getValue(line.key)
            listOf(
                csvText(line.reportId),
                csvText(line.lineId),
                csvText(line.key),
                line.amountMinor.toString(),
                csvText(line.currency),
                csvText(mapping.glAccountCode),
                csvText(mapping.costCenter.orEmpty()),
            ).joinToString(",")
        }
    return "report_id,line_id,category,amount_minor,currency,gl_account_code,cost_center\r\n" + rows.joinToString("\r\n") + "\r\n"
}

private fun csvText(value: String): String {
    val formulaLike = value.firstOrNull() in listOf('\t', '\r') || value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')
    val safe = if (formulaLike) "'$value" else value
    return "\"${safe.replace("\"", "\"\"")}\""
}
