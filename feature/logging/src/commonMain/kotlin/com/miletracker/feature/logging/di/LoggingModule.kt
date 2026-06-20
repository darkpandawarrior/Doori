package com.miletracker.feature.logging.di

import com.miletracker.feature.logging.repository.CardsTxnHistoryRepository
import com.miletracker.feature.logging.repository.ExpenseRepository
import com.miletracker.feature.logging.repository.LogMilesDraftRepository
import com.miletracker.feature.logging.repository.LogMilesServiceRepository
import com.miletracker.feature.logging.repository.SettlementHistoryRepository
import com.miletracker.feature.logging.repository.VoucherHistoryRepository
import com.miletracker.feature.logging.usecase.LogMilesSubmitUseCase
import com.miletracker.feature.logging.viewmodel.CardsTxnHistoryViewModel
import com.miletracker.feature.logging.viewmodel.ExpenseViewModel
import com.miletracker.feature.logging.viewmodel.LogMilesViewModel
import com.miletracker.feature.logging.viewmodel.SettlementHistoryViewModel
import com.miletracker.feature.logging.viewmodel.VoucherHistoryViewModel
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
    }
