package com.mileway.feature.payments.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.payments.repository.PaymentsRepository
import com.mileway.feature.payments.search.PaymentsSearchProvider
import com.mileway.feature.payments.viewmodel.CreatePaymentViewModel
import com.mileway.feature.payments.viewmodel.PaymentsHistoryViewModel
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
