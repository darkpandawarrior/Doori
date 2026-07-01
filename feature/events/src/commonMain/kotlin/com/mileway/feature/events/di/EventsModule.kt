package com.mileway.feature.events.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.events.repository.EventsRepository
import com.mileway.feature.events.search.EventsSearchProvider
import com.mileway.feature.events.viewmodel.CreateEventViewModel
import com.mileway.feature.events.viewmodel.EventsHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val eventsModule =
    module {
        single { EventsRepository() }
        viewModelOf(::CreateEventViewModel)
        viewModelOf(::EventsHistoryViewModel)
        // EV: events contribution to master search (getAll<SearchProvider>() picks it up).
        single<SearchProvider> { EventsSearchProvider(get()) }
    }
