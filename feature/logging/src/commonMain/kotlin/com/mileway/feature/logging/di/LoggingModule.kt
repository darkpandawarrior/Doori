package com.mileway.feature.logging.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.core.platform.OfflineLocationNameResolver
import com.mileway.feature.logging.repository.CardsTxnHistoryRepository
import com.mileway.feature.logging.repository.ExpenseRepository
import com.mileway.feature.logging.repository.LogMilesDraftRepository
import com.mileway.feature.logging.repository.LogMilesFrequentRouteRepository
import com.mileway.feature.logging.repository.LogMilesServiceRepository
import com.mileway.feature.logging.repository.SettlementHistoryRepository
import com.mileway.feature.logging.repository.VoucherHistoryRepository
import com.mileway.feature.logging.usecase.LogMilesSubmitUseCase
import com.mileway.feature.logging.viewmodel.CardsTxnHistoryViewModel
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.mileway.feature.logging.viewmodel.SearchLocationViewModel
import com.mileway.feature.logging.viewmodel.SettlementHistoryViewModel
import com.mileway.feature.logging.viewmodel.VoucherDetailsViewModel
import com.mileway.feature.logging.viewmodel.VoucherHistoryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val loggingModule =
    module {
        single { LogMilesServiceRepository(get()) }
        single { LogMilesDraftRepository(get()) }
        single { LogMilesFrequentRouteRepository(get()) }
        single { ExpenseRepository(get()) }
        // SP.1/SP.2/SP.3: voucher + settlement + cards-txn history (offline fakes + MVI VMs).
        single { VoucherHistoryRepository(get()) }
        single { SettlementHistoryRepository() }
        single { CardsTxnHistoryRepository() }
        factory { LogMilesSubmitUseCase(get(), get()) }
        viewModel { LogMilesViewModel(get(), get(), get(), get(), get()) }
        // P12.3: reviewTracker is optional (getOrNull) — bound in the app/iOS graph, absent in tests.
        viewModel { ExpenseViewModel(get(), getOrNull()) }
        viewModel { VoucherHistoryViewModel(get(), get()) }
        // P27.E.12: voucher drill-down — VoucherDao directly (a read, no derived history fields needed).
        viewModel { VoucherDetailsViewModel(get()) }
        viewModel { SettlementHistoryViewModel(get()) }
        viewModel { CardsTxnHistoryViewModel(get()) }
        // Location switching sheet: SavedLocationsSource (core:data) + optional platform tracker.
        viewModel {
            SearchLocationViewModel(
                savedLocations = get(),
                locationTracker = getOrNull(),
                locationNameResolver = getOrNull() ?: OfflineLocationNameResolver(),
            )
        }
        // SP.4: Spends contribution to master search (F0.5 registry).
        // PLAN_V29 P29.S.1: named qualifier so this doesn't silently override another module's
        // SearchProvider binding — see TrackingSearchProvider's doc for the full why.
        single<SearchProvider>(named("logging")) {
            com.mileway.feature.logging.search.ExpensesSearchProvider(get(), get(), get(), get())
        }
    }
