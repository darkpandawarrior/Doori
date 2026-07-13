package com.mileway.feature.events.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.events.repository.EventsRepository
import com.mileway.feature.events.search.EventsSearchProvider
import com.mileway.feature.events.viewmodel.CreateEventViewModel
import com.mileway.feature.events.viewmodel.EventsHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val eventsModule =
    module {
        single { EventsRepository() }
        viewModelOf(::CreateEventViewModel)
        viewModelOf(::EventsHistoryViewModel)
        // EV: events contribution to master search (getAll<SearchProvider>() picks it up).
        // PLAN_V29 P29.S.1: named qualifier so this doesn't silently override another module's
        // SearchProvider binding — see TrackingSearchProvider's doc for the full why.
        single<SearchProvider>(named("events")) { EventsSearchProvider(get()) }
    }
