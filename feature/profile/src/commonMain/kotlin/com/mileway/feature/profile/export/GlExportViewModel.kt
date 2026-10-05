package com.mileway.feature.profile.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.dao.GlMappingDao
import com.mileway.core.data.domain.claim.Report
import com.mileway.core.data.model.db.GlMappingEntity
import com.mileway.core.network.api.NetworkBackendFlags
import com.mileway.core.platform.ShareSheet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Clock

class GlExportViewModel(
    private val observeReports: () -> Flow<List<Report>>,
    private val mappings: GlMappingDao,
    private val shareSheet: ShareSheet,
    private val uploadCsv: suspend (String) -> Boolean,
    private val clock: Clock = Clock.System,
) : ViewModel() {
    constructor(
        reports: ReportRepository,
        mappings: GlMappingDao,
        shareSheet: ShareSheet,
        uploadCsv: suspend (String) -> Boolean,
    ) : this(reports::observeAll, mappings, shareSheet, uploadCsv)

    data class State(
        val lines: List<GlExportLine> = emptyList(),
        val mappings: List<GlMappingEntity> = emptyList(),
        val unmapped: List<GlExportLine> = emptyList(),
        val loading: Boolean = true,
        val dataReady: Boolean = false,
        val busy: Boolean = false,
        val realBackend: Boolean = NetworkBackendFlags.useRealBackend,
        val error: String? = null,
        val message: String? = null,
    ) {
        val canExport: Boolean get() = dataReady && !loading && !busy && lines.isNotEmpty() && unmapped.isEmpty()
    }

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    init {
        load()
    }

    @Suppress("TooGenericExceptionCaught")
    fun load() {
        observation?.cancel()
        mutableState.value = state.value.copy(loading = true, dataReady = false, error = null)
        observation =
            viewModelScope.launch {
                try {
                    combine(observeReports(), mappings.observeAll()) { claims, codes -> glExportLines(claims) to codes }
                        .collect { (lines, codes) ->
                            mutableState.value = state.value.copy(
                                lines = lines,
                                mappings = codes,
                                unmapped = unmappedGlLines(lines, codes),
                                loading = false,
                                dataReady = true,
                            )
                        }
                } catch (failure: Exception) {
                    if (failure is CancellationException) throw failure
                    mutableState.value = state.value.copy(loading = false, dataReady = false, error = "Unable to load accounting lines. Try again.")
                }
            }
    }

    @Suppress("TooGenericExceptionCaught")
    fun saveMapping(
        key: String,
        code: String,
        costCenter: String,
    ) {
        if (state.value.busy || !state.value.dataReady) return
        if (code.isBlank() || state.value.lines.none { it.key == key }) {
            mutableState.value = state.value.copy(error = "Enter a GL account code for this category.")
            return
        }
        mutableState.value = state.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                mappings.upsert(GlMappingEntity(key, code.trim(), costCenter.trim().takeIf { it.isNotEmpty() }, clock.now().toEpochMilliseconds()))
                mutableState.value = state.value.copy(busy = false, message = "GL mapping saved locally.")
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.value = state.value.copy(busy = false, error = "Unable to save the GL mapping. Try again.")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun export() {
        if (!state.value.canExport) return
        mutableState.value = state.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                // Re-read persisted values at export time so a newly unmapped line blocks export.
                val lines = glExportLines(observeReports().first())
                val codes = mappings.observeAll().first()
                val csv = glExportCsv(lines, codes)
                val realBackend = NetworkBackendFlags.useRealBackend
                mutableState.value = state.value.copy(lines = lines, mappings = codes, unmapped = unmappedGlLines(lines, codes), realBackend = realBackend)
                val message =
                    when {
                        csv == null -> "Export blocked. Map every line before exporting."
                        !realBackend -> {
                            shareSheet.share(text = csv, subject = "GL-coded accounting export")
                            "CSV shared. No accounting upload was attempted."
                        }
                        uploadCsv(csv) -> "Accounting backend accepted the CSV."
                        else -> "Accounting upload failed. No success was recorded."
                    }
                mutableState.value = state.value.copy(busy = false, message = message)
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.value = state.value.copy(busy = false, error = "Export failed. Try again.")
            }
        }
    }
}
