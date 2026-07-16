package com.mileway.feature.whatsnew.di

import com.mileway.core.data.whatsnew.WhatsNewVersionProvider
import com.mileway.feature.whatsnew.data.BundledWhatsNewRepository
import com.mileway.feature.whatsnew.data.WhatsNewRepository
import org.koin.dsl.module

/**
 * PLAN_V36 P1 — `:feature:whatsnew`'s Koin module. Only the bundled repository binding for now;
 * list/detail ViewModels are added in P3/P4.
 */
val whatsNewFeatureModule =
    module {
        single<WhatsNewRepository> { BundledWhatsNewRepository() }
        single<WhatsNewVersionProvider> { get<WhatsNewRepository>() }
    }
