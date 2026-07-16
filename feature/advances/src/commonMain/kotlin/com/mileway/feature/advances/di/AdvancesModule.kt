package com.mileway.feature.advances.di

import com.mileway.feature.advances.data.AdvancesRepository
import com.mileway.feature.advances.data.AdvancesRequestStore
import com.mileway.feature.advances.data.MockAdvancesRepository
import com.mileway.feature.advances.data.MockQrCardsRepository
import com.mileway.feature.advances.data.QrCardsRepository
import com.mileway.feature.advances.viewmodel.AdvancesHomeViewModel
import com.mileway.feature.advances.viewmodel.AskAdvanceViewModel
import com.mileway.feature.advances.viewmodel.PettyCardDetailViewModel
import com.mileway.feature.advances.viewmodel.QrCardDetailViewModel
import com.mileway.feature.advances.viewmodel.QrRequestViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Advance-wallet feature DI (PLAN_V35.P3 repos + P4 screens/ViewModels). Both mock repos share
 * one [AdvancesRequestStore] so petty and QR submits land in the same open-requests timeline.
 * Pure Kotlin (no platform-specific binding, unlike CardsModule's locale factory), so this lives
 * in commonMain rather than androidMain.
 */
val advancesModule: Module =
    module {
        single { AdvancesRequestStore() }
        single<AdvancesRepository> { MockAdvancesRepository(get()) }
        single<QrCardsRepository> { MockQrCardsRepository(get()) }
        viewModel { AdvancesHomeViewModel(get(), get()) }
        viewModel { PettyCardDetailViewModel(get()) }
        viewModel { QrCardDetailViewModel(get()) }
        viewModel { AskAdvanceViewModel(get()) }
        viewModel { QrRequestViewModel(get()) }
    }
