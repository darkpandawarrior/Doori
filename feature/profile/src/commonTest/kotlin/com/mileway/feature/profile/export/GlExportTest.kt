package com.mileway.feature.profile.export

import com.mileway.core.data.dao.GlMappingDao
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.GlMappingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun accountingReport() =
    Report(
        "r1",
        "employee",
        listOf(
            ExpenseLine("expense", 1250, "INR", merchant = "Private merchant", category = "FOOD"),
            MileageLine("mileage", 2500, "INR", sourceTripId = "private-trip", distanceKm = 10.0, vehicleKey = "car"),
            PerDiemLine("diem", 5000, "USD", days = 2, dailyRateMinor = 2500),
            AdvanceLine("advance", -1000, "INR", advanceId = "a1", reconciled = true),
        ),
        ReportLifecycleState.APPROVED,
    )

class GlExportTest {
    @Test
    fun onlyEligibleReportsEnterValidationAndAllClaimTypesHaveStableKeys() {
        val reports = ReportLifecycleState.entries.map { accountingReport().copy(id = it.name, state = it) }
        val lines = glExportLines(reports)
        assertEquals(setOf("APPROVED", "APPROVED_FOR_PAYMENT", "PAID"), lines.map { it.reportId }.toSet())
        assertEquals(12, lines.size)
        assertEquals(listOf("FOOD", "MILEAGE", "PER_DIEM", "ADVANCE"), accountingReport().lines.map { it.glMappingKey() })
        assertEquals(listOf("FOOD", MILEAGE_GL_KEY, PER_DIEM_GL_KEY, ADVANCE_GL_KEY), accountingReport().lines.map { it.glMappingKey() })
        assertEquals(lines, unmappedGlLines(lines, emptyList()))
        assertNull(glExportCsv(lines, emptyList()))
        assertNull(glExportCsv(emptyList(), emptyList()))
    }

    @Test
    fun mappingRoundTripExportsThePersistedCodesAndNoTripEvidence() =
        runTest {
            val lines = glExportLines(listOf(accountingReport()))
            val dao = MemoryGlMappingDao()
            for ((index, key) in lines.map { it.key }.distinct().withIndex()) {
                dao.upsert(GlMappingEntity(key, "610$index", "CC,$index", index.toLong()))
            }
            dao.upsert(GlMappingEntity("FOOD", "6200", "Meals \"team\"", 100))
            val csv = requireNotNull(glExportCsv(lines, dao.observeAll().first()))
            val rows = parseCsv(csv)
            assertEquals(listOf("report_id", "line_id", "category", "amount_minor", "currency", "gl_account_code", "cost_center"), rows.first())
            rows.drop(1).forEach { row ->
                val mapping = requireNotNull(dao.get(row[2]))
                assertEquals(mapping.glAccountCode, row[5])
                assertEquals(mapping.costCenter, row[6])
                val line = lines.single { it.lineId == row[1] }
                assertEquals(line.amountMinor, row[3].toLong())
                assertEquals(line.currency, row[4])
            }
            assertFalse(csv.contains("private-trip"))
            assertFalse(csv.contains("Private merchant"))
            assertFalse(rows.first().any { it.contains("lat") || it.contains("lng") || it.contains("address") })
            assertEquals(7, rows.single { it[1] == "mileage" }.size)
            dao.upsert(GlMappingEntity(MILEAGE_GL_KEY, "  ", null, 101))
            assertEquals(listOf("mileage"), unmappedGlLines(lines, dao.observeAll().first()).map { it.lineId })
            assertNull(glExportCsv(lines, dao.observeAll().first()))
        }

    @Test
    fun csvQuotesNewlinesAndPreservesMappingValuesExactly() {
        val line = GlExportLine("=report", "line", "Food,\"team\"\nmeal", 123, "INR")
        val csv = requireNotNull(glExportCsv(listOf(line), listOf(GlMappingEntity(line.key, "-6100", "@center", 0))))
        val row = parseCsv(csv).last()
        assertEquals("=report", row[0])
        assertEquals(line.key, row[2])
        assertEquals("-6100", row[5])
        assertEquals("@center", row[6])
        assertTrue(csv.endsWith("\r\n"))
    }
}

/** Test parser independent of the exporter, including escaped quotes and embedded newlines. */
internal fun parseCsv(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var index = 0
    while (index < csv.length) {
        val char = csv[index]
        when {
            char == '"' && quoted && csv.getOrNull(index + 1) == '"' -> {
                field.append('"')
                index++
            }
            char == '"' -> quoted = !quoted
            char == ',' && !quoted -> {
                row.add(field.toString())
                field.clear()
            }
            char == '\n' && !quoted -> {
                row.add(field.toString().removeSuffix("\r"))
                rows.add(row)
                row = mutableListOf()
                field.clear()
            }
            else -> field.append(char)
        }
        index++
    }
    check(!quoted)
    return rows
}

internal class MemoryGlMappingDao : GlMappingDao {
    val rows = MutableStateFlow<List<GlMappingEntity>>(emptyList())
    var failWrites = false

    override fun observeAll() = rows

    override suspend fun get(category: String) = rows.value.firstOrNull { it.category == category }

    override suspend fun upsert(entity: GlMappingEntity) {
        check(!failWrites)
        rows.value = rows.value.filterNot { it.category == entity.category } + entity
    }
}
