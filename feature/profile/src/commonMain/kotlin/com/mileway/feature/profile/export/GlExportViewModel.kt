package com.mileway.feature.profile.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.GlMappingDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.GlMappingEntity
import com.mileway.core.network.accounting.AccountingExportClient
import com.mileway.core.network.accounting.AccountingExportResult
import com.mileway.core.network.api.NetworkBackendFlags
import com.mileway.core.platform.ShareSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Pre-export GL validation. Combines approved reports with the gl_mappings table, lists unmapped
 * lines, upserts a mapping from the screen, and exports only when nothing is unmapped.
 */
class GlExportViewModel(
    reports: Flow<List<Report>>,
    private val mappings: GlMappingDao,
    private val shareSheet: ShareSheet,
    private val client: AccountingExportClient,
    private val useRealBackend: () -> Boolean = { NetworkBackendFlags.useRealBackend },
    private val clock: Clock = Clock.System,
) : ViewModel() {
    data class State(
        val lines: List<GlLine> = emptyList(),
        val unmapped: List<UnmappedLine> = emptyList(),
        val drafts: Map<String, String> = emptyMap(),
        val realBackend: Boolean = false,
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null,
    ) {
        val canExport: Boolean get() = !busy && lines.isNotEmpty() && unmapped.isEmpty()
    }

    private val mutableState = MutableStateFlow(State(realBackend = useRealBackend()))
    val state = mutableState.asStateFlow()
    private var mappingSnapshot: Map<String, GlMappingEntity> = emptyMap()

    init {
        viewModelScope.launch {
            combine(reports, mappings.observeAll()) { claims, rows -> glLines(claims) to rows.associateBy { it.category } }
                .collect { (lines, byKey) ->
                    mappingSnapshot = byKey
                    mutableState.update {
                        it.copy(lines = lines, unmapped = unmappedLines(lines, byKey), realBackend = useRealBackend())
                    }
                }
        }
    }

    fun editDraft(
        key: String,
        code: String,
    ) = mutableState.update { it.copy(drafts = it.drafts + (key to code), error = null) }

    /** Upserts the draft code for [key]; the unmapped list shrinks when the table flow re-emits. */
    fun map(key: String) {
        val code = state.value.drafts[key].orEmpty().trim()
        if (code.isEmpty()) {
            mutableState.update { it.copy(error = "Enter a GL code for $key") }
            return
        }
        viewModelScope.launch {
            mappings.upsert(GlMappingEntity(key, code, mappingSnapshot[key]?.costCenter, clock.now().toEpochMilliseconds()))
            mutableState.update { it.copy(drafts = it.drafts - key, message = "Mapped $key", error = null) }
        }
    }

    // Transport failures are mapped to a result by the client; this guards the share-sheet call.
    @Suppress("TooGenericExceptionCaught")
    fun export() {
        val current = state.value
        val coded = codedLines(current.lines, mappingSnapshot)
        if (coded == null) {
            mutableState.update { it.copy(error = "Map every line to a GL code before exporting") }
            return
        }
        mutableState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (useRealBackend()) {
                    when (val result = client.submit(coded)) {
                        is AccountingExportResult.Accepted ->
                            mutableState.update { it.copy(busy = false, message = "Sent ${result.lineCount} lines to accounting") }
                        is AccountingExportResult.Rejected ->
                            mutableState.update { it.copy(busy = false, error = "Accounting rejected the export (${result.status})") }
                        is AccountingExportResult.Unavailable ->
                            mutableState.update { it.copy(busy = false, error = "Accounting is unavailable: ${result.reason}") }
                    }
                } else {
                    shareSheet.share(text = glCsv(coded), subject = "GL coded claims export")
                    mutableState.update { it.copy(busy = false, message = "Shared CSV with ${coded.size} lines") }
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(busy = false, error = failure.message ?: "Export failed") }
            }
        }
    }
}
