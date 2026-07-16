package com.mileway.feature.whatsnew.di

import com.mileway.core.data.whatsnew.WhatsNewVersionProvider
import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.viewmodel.WhatsNewListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * PLAN_V36 P1/P3 — `:feature:whatsnew`'s Koin module. The bundled repository binding plus the
 * list screen's ViewModel; the detail ViewModel is added in P4.
 */
val whatsNewFeatureModule =
    module {
        single<WhatsNewRepository> { BundledWhatsNewRepository() }
        single<WhatsNewVersionProvider> { get<WhatsNewRepository>() }
        viewModelOf(::WhatsNewListViewModel)
    }
