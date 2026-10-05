package com.mileway.feature.cards.di

import com.mileway.core.data.claim.ReportRepository
import com.mileway.core.data.claim.saveStatementMatches
import com.mileway.core.data.claim.wasStatementImported
import com.mileway.feature.cards.data.CardsMockDataProvider
import com.mileway.feature.cards.data.CardsMockDataProviderFactory
import com.mileway.feature.cards.import.StatementImportViewModel
import com.mileway.feature.cards.import.StatementImporter
import com.mileway.feature.cards.payout.payoutBeneficiaryStore
import com.mileway.feature.cards.security.CardSecurityManager
import com.mileway.feature.cards.viewmodel.CardDetailViewModel
import com.mileway.feature.cards.viewmodel.CardKycViewModel
import com.mileway.feature.cards.viewmodel.CardRequestViewModel
import com.mileway.feature.cards.viewmodel.CardsHomeViewModel
import kotlinx.coroutines.flow.first
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Cards feature DI, locale-aware mock provider + the MVI ViewModels. */
val cardsModule: Module =
    module {
        single<CardsMockDataProvider> { CardsMockDataProviderFactory.provider() }
        single { CardSecurityManager() }
        single { payoutBeneficiaryStore() }
        single {
            val reports = get<ReportRepository>()
            StatementImporter(
                loadReports = { reports.observeByEmployee(it).first() },
                batchExists = reports::wasStatementImported,
                saveMatches = { batch, snapshots, matches -> reports.saveStatementMatches(batch, snapshots, matches) },
            )
        }
        viewModel { StatementImportViewModel(get(), get(), get()) }
        viewModelOf(::CardsHomeViewModel)
        viewModelOf(::CardDetailViewModel)
        viewModelOf(::CardRequestViewModel)
        // PLAN_V24 P4.3: card KYC wizard (LocalOtpEngine resolved from core:data's module).
        viewModelOf(::CardKycViewModel)
    }
