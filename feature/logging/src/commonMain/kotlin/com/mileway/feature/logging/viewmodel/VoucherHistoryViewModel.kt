package com.mileway.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.VoucherStatus
import com.mileway.core.ui.mvi.ScreenState
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.ui.model.SubmittedVoucher
import com.mileway.feature.tracking.repository.VoucherRepository
import com.siddharth.kmp.common.UiText
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** SP.1: voucher-history tabs (the first tab is "All"). */
val VOUCHER_HISTORY_TABS: List<VoucherStatus?> = listOf(null) + VoucherStatus.entries

data class VoucherHistoryUiState(
    val tabIndex: Int = 0,
    val query: String = "",
    val list: ScreenState<List<SubmittedVoucher>> = ScreenState.Loading,
)

sealed interface VoucherHistoryAction {
    data object Refresh : VoucherHistoryAction

    data class SelectTab(val index: Int) : VoucherHistoryAction

    data class SetQuery(val query: String) : VoucherHistoryAction

    /** P3.6: withdraws a DRAFT voucher (gated by [VoucherRepository.withdraw]); a no-op for any other status. */
    data class Withdraw(val voucherNumber: String) : VoucherHistoryAction
}

sealed interface VoucherHistoryEffect {
    /**
     * A withdraw attempt threw (e.g. a local DB I/O error) rather than completing. Previously this
     * ran fire-and-forget in [viewModelScope] with nothing collecting a failure — an exception here
     * would either crash silently or leave the voucher looking withdrawn when it wasn't. Named the
     * same way the check-in/log-miles/expense submit flows already surface a local-write failure.
     */
    data class ShowError(val message: UiText) : VoucherHistoryEffect
}

/**
 * SP.1/P3.1: reducer for the voucher history surface. Collects the shared, Room-backed
 * [VoucherHistoryRepository] (the same store `feature/tracking`'s Create Voucher writes to),
 * filters by the selected status tab + free-text query, and exposes a [ScreenState] the shared
 * `HistoryListScaffold` renders.
 */
class VoucherHistoryViewModel(
    private val repository: VoucherHistoryRepository,
    private val voucherRepository: VoucherRepository,
) : BaseViewModel<VoucherHistoryUiState, VoucherHistoryEffect, VoucherHistoryAction>(VoucherHistoryUiState()) {
    private var reloadJob: Job? = null

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
        reload()
    }

    override fun onAction(action: VoucherHistoryAction) {
        when (action) {
            VoucherHistoryAction.Refresh -> reload()
            is VoucherHistoryAction.SelectTab -> {
                setState { copy(tabIndex = action.index) }
                reload()
            }
            is VoucherHistoryAction.SetQuery -> {
                setState { copy(query = action.query) }
                reload()
            }
            is VoucherHistoryAction.Withdraw -> {
                viewModelScope.launch {
                    runCatching { voucherRepository.withdraw(action.voucherNumber) }
                        .onFailure { emitEffect(VoucherHistoryEffect.ShowError(UiText.of("Couldn't withdraw this voucher — try again"))) }
                }
            }
        }
    }

    /** Cancels any in-flight collector before starting a new one — tab/query changes replace, not stack. */
    private fun reload() {
        val status = VOUCHER_HISTORY_TABS.getOrNull(currentState.tabIndex)
        reloadJob?.cancel()
        reloadJob =
            viewModelScope.launch {
                repository.observeVouchers(status).collectLatest { rows ->
                    val q = currentState.query.trim()
                    val filtered =
                        rows.filter {
                            q.isEmpty() ||
                                it.id.contains(q, ignoreCase = true) ||
                                it.serviceTag.contains(q, ignoreCase = true) ||
                                it.office.contains(q, ignoreCase = true)
                        }
                    setState { copy(list = ScreenState.Content(filtered)) }
                }
            }
    }
}
