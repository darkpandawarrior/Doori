package com.mileway.feature.whatsnew.di

import com.mileway.core.data.whatsnew.WhatsNewVersionProvider
import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import com.mileway.feature.whatsnew.viewmodel.WhatsNewDetailViewModel
import com.mileway.feature.whatsnew.viewmodel.WhatsNewListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * PLAN_V36 P1/P3/P4 — `:feature:whatsnew`'s Koin module. The bundled repository binding, the list
 * screen's ViewModel, and the detail screen's ViewModel — the latter takes the nav-supplied entry
 * id as a runtime parameter (`viewModel { params -> ... }`), mirroring `feature:events`'
 * `EventDetailViewModel` binding shape.
 */
val whatsNewFeatureModule =
    module {
        single<WhatsNewRepository> { BundledWhatsNewRepository() }
        single<WhatsNewVersionProvider> { get<WhatsNewRepository>() }
        viewModelOf(::WhatsNewListViewModel)
        viewModel { params -> WhatsNewDetailViewModel(entryId = params.get(), repository = get()) }
    }
