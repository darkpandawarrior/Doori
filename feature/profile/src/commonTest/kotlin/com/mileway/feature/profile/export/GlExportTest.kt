package com.mileway.feature.profile.export

import com.mileway.core.data.dao.GlMappingDao
import com.mileway.core.data.domain.claim.AdvanceLine
import com.mileway.core.data.domain.claim.ExpenseLine
import com.mileway.core.data.domain.claim.MileageLine
import com.mileway.core.data.domain.claim.PerDiemLine
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.model.db.GlMappingEntity
import com.mileway.core.network.accounting.AccountingExportClient
import com.mileway.core.platform.ShareSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeGlMappingDao : GlMappingDao {
    val rows = MutableStateFlow<Map<String, GlMappingEntity>>(emptyMap())

    override fun observeAll(): Flow<List<GlMappingEntity>> = rows.map { it.values.toList() }

    override suspend fun get(category: String): GlMappingEntity? = rows.value[category]

    override suspend fun upsert(entity: GlMappingEntity) {
        rows.value = rows.value + (entity.category to entity)
    }
}

private class RecordingShareSheet : ShareSheet {
    var text: String? = null

    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) {
        this.text = text
    }
}

private fun map(
    key: String,
    code: String,
    cost: String? = null,
) = GlMappingEntity(key, code, cost, 1L)

private val approved =
    Report(
        "r1",
        "emp",
        listOf(
            ExpenseLine("e1", 12_500, "INR", merchant = "Cafe", category = "Meals"),
            MileageLine("m1", 4_000, "INR", distanceKm = 20.0, vehicleKey = "car", sourceTripId = "trip-1"),
            PerDiemLine("p1", 90_000, "INR", days = 3, dailyRateMinor = 30_000),
            AdvanceLine("a1", 1_000, "INR", advanceId = "adv"),
        ),
        ReportLifecycleState.APPROVED,
    )
private val draft = Report("r2", "emp", listOf(ExpenseLine("e2", 100, "INR", merchant = "X", category = "Travel")), ReportLifecycleState.DRAFT)

class GlExportTest {
    @Test
    fun `keys are category for expenses and fixed constants for mileage and per diem`() {
        assertEquals("MILEAGE", GL_KEY_MILEAGE)
        assertEquals("PER_DIEM", GL_KEY_PER_DIEM)
        val lines = glLines(listOf(approved))
        assertEquals(listOf("Meals", GL_KEY_MILEAGE, GL_KEY_PER_DIEM), lines.map { it.key })
        assertEquals(listOf("m1"), lines.filter { it.key == GL_KEY_MILEAGE }.map { it.lineId })
    }

    @Test
    fun `only approved approved for payment and paid reports are exported and advances are skipped`() {
        val states = ReportLifecycleState.entries.associateWith { glLines(listOf(approved.copy(state = it))).size }
        assertEquals(GL_EXPORTABLE_STATES, states.filterValues { it > 0 }.keys)
        assertTrue(glLines(listOf(draft)).isEmpty())
        assertFalse(glLines(listOf(approved)).any { it.lineId == "a1" })
    }

    @Test
    fun `blank category is listed under a mappable key`() {
        val blank = approved.copy(lines = listOf(ExpenseLine("e", 1, "INR", merchant = "m", category = "  ")))
        assertEquals(GL_KEY_UNCATEGORIZED, glLines(listOf(blank)).single().key)
    }

    @Test
    fun `unmapped lines list exactly the lines without a non blank code`() {
        val lines = glLines(listOf(approved))
        val partial = mapOf("Meals" to map("Meals", "6200"), GL_KEY_MILEAGE to map(GL_KEY_MILEAGE, "  "))
        val unmapped = unmappedLines(lines, partial)
        assertEquals(listOf("m1", "p1"), unmapped.map { it.lineId })
        assertEquals(listOf("r1", "r1"), unmapped.map { it.reportId })
        assertEquals(4_000L, unmapped.first().amountMinor)
        assertNull(codedLines(lines, partial))
        assertNull(codedLines(emptyList(), partial))
    }

    @Test
    fun `a coded export round trips through the mapping table`() {
        val mappings =
            mapOf(
                "Meals" to map("Meals", "6200", "CC-9"),
                GL_KEY_MILEAGE to map(GL_KEY_MILEAGE, "6100"),
                GL_KEY_PER_DIEM to map(GL_KEY_PER_DIEM, "6150"),
            )
        val coded = assertNotNull(codedLines(glLines(listOf(approved)), mappings))
        val rows = parseCsv(glCsv(coded))
        assertEquals(GL_CSV_HEADER.split(","), rows.first())
        val body = rows.drop(1)
        assertEquals(3, body.size)
        body.forEach { row ->
            val mapping = mappings.getValue(row[3])
            assertEquals(mapping.glAccountCode, row[4])
            assertEquals(mapping.costCenter.orEmpty(), row[5])
        }
        assertEquals(listOf("12500", "4000", "90000"), body.map { it[6] })
    }

    @Test
    fun `csv rows carry no coordinates or address columns`() {
        val coded = assertNotNull(codedLines(glLines(listOf(approved)), mapOf("Meals" to map("Meals", "1"), GL_KEY_MILEAGE to map(GL_KEY_MILEAGE, "2"), GL_KEY_PER_DIEM to map(GL_KEY_PER_DIEM, "3"))))
        val csv = glCsv(coded)
        val header = csv.lineSequence().first().split(",")
        assertTrue(header.none { it.contains("lat") || it.contains("lng") || it.contains("lon") || it.contains("address") })
        assertFalse(csv.contains("trip-1"))
        val mileageRow = parseCsv(csv).single { it[2] == "mileage" }
        assertEquals(header.size, mileageRow.size)
    }

    @Test
    fun `csv quotes commas and defuses formulas`() {
        assertEquals("\"a,b\"", csvText("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", csvText("say \"hi\""))
        assertEquals("'=SUM(A1)", csvText("=SUM(A1)"))
        assertEquals("plain", csvText("plain"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    class ViewModelTests {
        @BeforeTest
        fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

        @AfterTest
        fun tearDown() = Dispatchers.resetMain()

        private fun vm(
            dao: FakeGlMappingDao,
            share: RecordingShareSheet,
            backend: Boolean = false,
        ) = GlExportViewModel(MutableStateFlow(listOf(approved, draft)), dao, share, AccountingExportClient(), { backend })

        @Test
        fun `export is blocked until every line is mapped and then shares the csv`() =
            runTest {
                val dao = FakeGlMappingDao()
                val share = RecordingShareSheet()
                val model = vm(dao, share)
                advanceUntilIdle()
                assertEquals(3, model.state.value.unmapped.size)
                assertFalse(model.state.value.canExport)
                model.export()
                advanceUntilIdle()
                assertNull(share.text)
                assertNotNull(model.state.value.error)

                model.editDraft("Meals", "6200")
                model.map("Meals")
                model.editDraft(GL_KEY_MILEAGE, "6100")
                model.map(GL_KEY_MILEAGE)
                advanceUntilIdle()
                assertEquals(listOf(GL_KEY_PER_DIEM), model.state.value.unmapped.map { it.key })
                model.editDraft(GL_KEY_PER_DIEM, " 6150 ")
                model.map(GL_KEY_PER_DIEM)
                advanceUntilIdle()
                assertTrue(model.state.value.unmapped.isEmpty())
                assertTrue(model.state.value.canExport)

                model.export()
                advanceUntilIdle()
                val rows = parseCsv(assertNotNull(share.text))
                assertEquals(setOf("6200", "6100", "6150"), rows.drop(1).map { it[4] }.toSet())
                assertEquals("6150", dao.get(GL_KEY_PER_DIEM)?.glAccountCode)
            }

        @Test
        fun `a blank draft is rejected and writes nothing`() =
            runTest {
                val dao = FakeGlMappingDao()
                val model = vm(dao, RecordingShareSheet())
                advanceUntilIdle()
                model.editDraft("Meals", "  ")
                model.map("Meals")
                advanceUntilIdle()
                assertTrue(dao.rows.value.isEmpty())
                assertNotNull(model.state.value.error)
            }
    }
}

/** Minimal RFC 4180 reader for the round-trip assertions. */
internal fun parseCsv(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> {
                cell.append('"')
                i++
            }
            c == '"' -> quoted = !quoted
            !quoted && c == ',' -> {
                row.add(cell.toString())
                cell.clear()
            }
            !quoted && c == '\n' -> {
                row.add(cell.toString())
                cell.clear()
                rows.add(row)
                row = mutableListOf()
            }
            else -> cell.append(c)
        }
        i++
    }
    row.add(cell.toString())
    rows.add(row)
    return rows
}
