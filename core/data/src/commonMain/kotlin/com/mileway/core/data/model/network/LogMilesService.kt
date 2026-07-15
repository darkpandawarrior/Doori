package com.mileway.core.data.model.network

// PLAN_V33 A1: every wire DTO formerly in this file (CoordsV2, ExpenseSubmissionResponse,
// SubmissionStatus, Voucher, LogMilesServiceDto, ...) moved to :contract's own
// `model/network/NetworkModels.kt` — same package, so nothing here needed an import edit.
// `LogMilesService` is the one type that stayed: a client-side domain mapping built from
// `LogMilesServiceDto` (see feature:logging's `LogMilesServiceRepository`), not itself a wire DTO,
// so it has no business living in a module shared with a future server.

// ── Domain models ─────────────────────────────────────────────────────────────

data class LogMilesService(
    val id: Long,
    val name: String,
    val glCode: String,
) {
    fun getDisplayString(): String = "$name ($glCode)"
}
