package com.mileway.feature.cards.import

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.session.SessionSource
import com.mileway.core.network.payout.PayoutBeneficiaryStore
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface StatementImportAction {
    data class Import(
        val fileName: String,
        val text: String,
    ) : StatementImportAction

    data class SaveBeneficiary(
        val vpa: String,
    ) : StatementImportAction

    data object ClearBeneficiary : StatementImportAction
}

data class StatementImportState(
    val busy: Boolean = false,
    val result: StatementImporter.Result? = null,
    val error: String? = null,
    val beneficiarySaved: Boolean = false,
)

/** Imports only local text for the active employee; payout addresses never enter logs or state. */
class StatementImportViewModel(
    private val importer: StatementImporter,
    private val beneficiary: PayoutBeneficiaryStore,
    private val session: SessionSource,
    private val importDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel<StatementImportState, Unit, StatementImportAction>(StatementImportState(beneficiarySaved = beneficiary.read() != null)) {
    override fun onAction(action: StatementImportAction) {
        if (state.value.busy) return
        setState { copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                when (action) {
                    is StatementImportAction.Import -> {
                        val employee = requireNotNull(session.sessionState.first().employeeCode) { "Sign in before importing a statement" }
                        val result = withContext(importDispatcher) { importer.import(employee, action.fileName, action.text) }
                        setState { copy(result = result) }
                    }
                    is StatementImportAction.SaveBeneficiary -> {
                        beneficiary.store(action.vpa)
                        setState { copy(beneficiarySaved = true) }
                    }
                    StatementImportAction.ClearBeneficiary -> {
                        beneficiary.clear()
                        setState { copy(beneficiarySaved = false) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                setState { copy(error = "Check the statement columns, dates, amounts and payout address; then retry.") }
            } catch (_: Exception) {
                setState { copy(error = "Unable to save locally. Reload the claims and retry.") }
            } finally {
                setState { copy(busy = false) }
            }
        }
    }
}
