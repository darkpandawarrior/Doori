package com.miletracker.feature.payments.di

import com.miletracker.core.data.search.SearchProvider
import com.miletracker.feature.payments.repository.PaymentsRepository
import com.miletracker.feature.payments.search.PaymentsSearchProvider
import com.miletracker.feature.payments.viewmodel.CreatePaymentViewModel
import com.miletracker.feature.payments.viewmodel.PaymentsHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val paymentsModule =
    module {
        single { PaymentsRepository() }
        viewModelOf(::CreatePaymentViewModel)
        viewModelOf(::PaymentsHistoryViewModel)
        // PM: payments contribution to master search (getAll<SearchProvider>() picks it up).
        single<SearchProvider> { PaymentsSearchProvider(get()) }
    }
