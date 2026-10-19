package com.mileway.feature.profile.export

import com.mileway.core.data.model.db.GlMappingEntity
import com.mileway.core.network.api.NetworkBackendFlags
import com.mileway.core.platform.ShareSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GlExportViewModelTest {
    @Test
    fun exportStaysBlockedUntilMappingsPersistAndLocalModeNeverUploads() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val original = NetworkBackendFlags.useRealBackend
            NetworkBackendFlags.useRealBackend = false
            try {
                val reports = MutableStateFlow(listOf(accountingReport()))
                val dao = MemoryGlMappingDao()
                val share = RecordingShareSheet()
                var uploads = 0
                val viewModel =
                    GlExportViewModel({ reports }, dao, share, {
                        uploads++
                        true
                    })
                runCurrent()
                assertEquals(4, viewModel.state.value.unmapped.size)
                viewModel.export()
                runCurrent()
                assertTrue(share.csv.isEmpty())
                viewModel.saveMapping("FOOD", " ", "")
                assertTrue(viewModel.state.value.error != null)
                dao.failWrites = true
                viewModel.saveMapping("FOOD", "6100", "")
                assertEquals(4, viewModel.state.value.unmapped.size)
                assertFalse(viewModel.state.value.busy)
                dao.failWrites = false
                for (key in accountingReport().lines.map { it.glMappingKey() }) {
                    viewModel.saveMapping(key, "6100", "CC")
                }
                runCurrent()
                assertTrue(viewModel.state.value.canExport)
                viewModel.export()
                runCurrent()
                assertEquals(1, share.csv.size)
                assertEquals(0, uploads)
                assertEquals("6100", dao.get(MILEAGE_GL_KEY)?.glAccountCode)
                assertEquals(5, parseCsv(share.csv.single()).size)
                assertEquals("CSV shared. No accounting upload was attempted.", viewModel.state.value.message)
                dao.upsert(GlMappingEntity("FOOD", "", null, 0))
                runCurrent()
                assertFalse(viewModel.state.value.canExport)
                viewModel.export()
                runCurrent()
                assertEquals(1, share.csv.size)
            } finally {
                NetworkBackendFlags.useRealBackend = original
                Dispatchers.resetMain()
            }
        }

    @Test
    fun realModeUploadsAndFailureNeverReportsSuccessOrSharesCsv() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val original = NetworkBackendFlags.useRealBackend
            NetworkBackendFlags.useRealBackend = true
            try {
                val reports = MutableStateFlow(listOf(accountingReport()))
                val dao = MemoryGlMappingDao()
                accountingReport().lines.forEach { dao.upsert(GlMappingEntity(it.glMappingKey(), "6100", null, 0)) }
                val share = RecordingShareSheet()
                var accepted = false
                var uploaded = ""
                val viewModel =
                    GlExportViewModel({ reports }, dao, share, { csv ->
                        uploaded = csv
                        accepted
                    })
                runCurrent()
                viewModel.export()
                runCurrent()
                assertEquals("Accounting upload failed. No success was recorded.", viewModel.state.value.message)
                assertFalse(viewModel.state.value.busy)
                assertEquals(5, parseCsv(uploaded).size)
                accepted = true
                viewModel.export()
                runCurrent()
                assertEquals("Accounting backend accepted the CSV.", viewModel.state.value.message)
                assertTrue(share.csv.isEmpty())
            } finally {
                NetworkBackendFlags.useRealBackend = original
                Dispatchers.resetMain()
            }
        }

    @Test
    fun exportRevalidatesFreshReportsRatherThanTheRenderedSnapshot() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                val dao = MemoryGlMappingDao()
                accountingReport().lines.forEach { dao.upsert(GlMappingEntity(it.glMappingKey(), "6100", null, 0)) }
                val share = RecordingShareSheet()
                var current = accountingReport()
                val viewModel =
                    GlExportViewModel(
                        observeReports = { MutableStateFlow(listOf(current)) },
                        mappings = dao,
                        shareSheet = share,
                        uploadCsv = { error("must not upload") },
                    )
                runCurrent()
                assertTrue(viewModel.state.value.canExport)
                current =
                    current.copy(
                        lines =
                            current.lines +
                                com.mileway.core.data.domain.claim.ExpenseLine(
                                    "new",
                                    100,
                                    "INR",
                                    merchant = "Cafe",
                                    category = "NEW_CATEGORY",
                                ),
                    )
                viewModel.export()
                runCurrent()
                assertEquals(
                    listOf("new"),
                    viewModel.state.value.unmapped
                        .map { it.lineId },
                )
                assertTrue(share.csv.isEmpty())
                assertFalse(viewModel.state.value.canExport)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun unreadableReportsBlockExportAndCanBeRetried() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            try {
                var fail = true
                val viewModel =
                    GlExportViewModel(
                        observeReports = { flow { if (fail) error("unreadable") else emit(listOf(accountingReport())) } },
                        mappings = MemoryGlMappingDao(),
                        shareSheet = RecordingShareSheet(),
                        uploadCsv = { error("must not upload") },
                    )
                runCurrent()
                assertFalse(viewModel.state.value.canExport)
                assertFalse(viewModel.state.value.dataReady)
                assertEquals("Unable to load accounting lines. Try again.", viewModel.state.value.error)
                fail = false
                viewModel.load()
                runCurrent()
                assertTrue(viewModel.state.value.dataReady)
                assertEquals(4, viewModel.state.value.unmapped.size)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class RecordingShareSheet : ShareSheet {
    val csv = mutableListOf<String>()

    override fun share(
        text: String,
        subject: String?,
        fileUri: String?,
    ) {
        csv.add(text)
    }
}
