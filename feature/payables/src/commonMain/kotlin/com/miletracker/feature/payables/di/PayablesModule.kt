package com.miletracker.feature.payables.di

import com.miletracker.feature.payables.repository.InvoiceRepository
import com.miletracker.feature.payables.repository.PayablesRepository
import com.miletracker.feature.payables.viewmodel.CreateInvoiceViewModel
import com.miletracker.feature.payables.viewmodel.PayablesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val payablesModule =
    module {
        single { PayablesRepository() }
        // PB.1: create-invoice flow (offline rotating-status submit).
        single { InvoiceRepository() }
        viewModel { PayablesViewModel(get()) }
        viewModel { CreateInvoiceViewModel(get()) }
    }
