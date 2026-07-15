package com.mileway.feature.logging.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.ui.mvi.ScreenState
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.launch

data class VoucherDetailsUiState(
    val voucher: ScreenState<VoucherEntity> = ScreenState.Loading,
)

sealed interface VoucherDetailsAction {
    data class Load(val voucherNumber: String) : VoucherDetailsAction
}

sealed interface VoucherDetailsEffect

/**
 * P27.E.12: voucher drill-down — loads a single row straight off the shared, Room-backed
 * [VoucherDao] (the same store [VoucherHistoryViewModel] lists from) by its primary key
 * ([VoucherEntity.voucherNumber]). [ScreenState.Empty] covers "no such voucher" (e.g. a stale
 * deep link after a withdraw); a thrown DAO error would be a genuine bug, not modeled as
 * recoverable state here.
 */
class VoucherDetailsViewModel(
    private val voucherDao: VoucherDao,
) : BaseViewModel<VoucherDetailsUiState, VoucherDetailsEffect, VoucherDetailsAction>(VoucherDetailsUiState()) {
    override fun onAction(action: VoucherDetailsAction) {
        when (action) {
            is VoucherDetailsAction.Load -> load(action.voucherNumber)
        }
    }

    private fun load(voucherNumber: String) {
        setState { copy(voucher = ScreenState.Loading) }
        viewModelScope.launch {
            val entity = voucherDao.getByNumber(voucherNumber)
            setState { copy(voucher = entity?.let { ScreenState.Content(it) } ?: ScreenState.Empty) }
        }
    }
}
