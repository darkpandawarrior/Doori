package com.mileway.feature.logging.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.logging.repository.CardsTxnHistoryRepository
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.repository.LogMilesDraftRepository
import com.mileway.feature.logging.repository.LogMilesServiceRepository
import com.mileway.feature.logging.repository.SettlementHistoryRepository
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.usecase.LogMilesSubmitUseCase
import com.mileway.feature.logging.viewmodel.CardsTxnHistoryViewModel
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.mileway.feature.logging.viewmodel.SettlementHistoryViewModel
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loggingModule =
    module {
        single { LogMilesServiceRepository(get()) }
        single { LogMilesDraftRepository(get()) }
        single { ExpenseRepository() }
        // SP.1/SP.2/SP.3: voucher + settlement + cards-txn history (offline fakes + MVI VMs).
        single { VoucherHistoryRepository() }
        single { SettlementHistoryRepository() }
        single { CardsTxnHistoryRepository() }
        factory { LogMilesSubmitUseCase(get()) }
        viewModel { LogMilesViewModel(get(), get(), get(), get()) }
        viewModel { ExpenseViewModel(get()) }
        viewModel { VoucherHistoryViewModel(get()) }
        viewModel { SettlementHistoryViewModel(get()) }
        viewModel { CardsTxnHistoryViewModel(get()) }
        // SP.4: Spends contribution to master search (F0.5 registry).
        single<SearchProvider> {
            com.mileway.feature.logging.search.ExpensesSearchProvider(get(), get(), get(), get())
        }
    }
