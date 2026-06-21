package com.miletracker.feature.cards.di

import com.miletracker.feature.cards.data.CardsMockDataProvider
import com.miletracker.feature.cards.data.CardsMockDataProviderFactory
import com.miletracker.feature.cards.security.CardSecurityManager
import com.miletracker.feature.cards.viewmodel.CardDetailViewModel
import com.miletracker.feature.cards.viewmodel.CardRequestViewModel
import com.miletracker.feature.cards.viewmodel.CardsHomeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Cards feature DI, locale-aware mock provider + the three MVI ViewModels. */
val cardsModule: Module =
    module {
        single<CardsMockDataProvider> { CardsMockDataProviderFactory.provider() }
        single { CardSecurityManager() }
        viewModelOf(::CardsHomeViewModel)
        viewModelOf(::CardDetailViewModel)
        viewModelOf(::CardRequestViewModel)
    }
