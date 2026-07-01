package com.mileway.feature.payables.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.payables.repository.GinRepository
import com.mileway.feature.payables.repository.InvoiceRepository
import com.mileway.feature.payables.repository.ParkingRepository
import com.mileway.feature.payables.repository.PayablesHistoryRepository
import com.mileway.feature.payables.repository.PayablesRepository
import com.mileway.feature.payables.search.PayablesSearchProvider
import com.mileway.feature.payables.viewmodel.CreateGinViewModel
import com.mileway.feature.payables.viewmodel.CreateInvoiceViewModel
import com.mileway.feature.payables.viewmodel.CreateParkingViewModel
import com.mileway.feature.payables.viewmodel.PayablesHistoryViewModel
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val payablesModule =
    module {
        single { PayablesRepository() }
        // PB.1: create-invoice flow (offline rotating-status submit).
        single { InvoiceRepository() }
        // PB.2: create-GIN flow (offline rotating-status submit).
        single { GinRepository() }
        // PB.3: create Park In/Out gate-event flow (offline rotating-status submit).
        single { ParkingRepository() }
        // PB.4: unified payables history (Invoice/PR/GIN/ParkInOut/ASN), also the PB.5 search source.
        single { PayablesHistoryRepository() }
        // PB.5: payables contribution to master search (getAll<SearchProvider>() picks it up).
        single<SearchProvider> { PayablesSearchProvider(get()) }
        viewModel { PayablesViewModel(get()) }
        viewModel { CreateInvoiceViewModel(get()) }
        viewModel { CreateGinViewModel(get()) }
        viewModel { CreateParkingViewModel(get()) }
        viewModel { PayablesHistoryViewModel(get()) }
    }
