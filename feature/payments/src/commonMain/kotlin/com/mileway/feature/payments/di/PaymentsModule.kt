package com.mileway.feature.payments.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.payments.repository.PaymentsRepository
import com.mileway.feature.payments.search.PaymentsSearchProvider
import com.mileway.feature.payments.viewmodel.CreatePaymentViewModel
import com.mileway.feature.payments.viewmodel.PaymentsHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsModule =
    module {
        single { PaymentsRepository() }
        viewModelOf(::CreatePaymentViewModel)
        viewModelOf(::PaymentsHistoryViewModel)
        // PM: payments contribution to master search (getAll<SearchProvider>() picks it up).
        // PLAN_V29 P29.S.1: named qualifier — Koin's single<T> is keyed by (type, qualifier), so
        // every module's SearchProvider binding needs a distinct name or they silently override
        // each other and getAll<SearchProvider>() only ever sees the last-loaded one.
        single<SearchProvider>(named("payments")) { PaymentsSearchProvider(get()) }
    }
