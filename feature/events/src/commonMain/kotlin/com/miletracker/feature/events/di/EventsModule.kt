package com.miletracker.feature.events.di

import com.miletracker.core.data.search.SearchProvider
import com.miletracker.feature.events.repository.EventsRepository
import com.miletracker.feature.events.search.EventsSearchProvider
import com.miletracker.feature.events.viewmodel.CreateEventViewModel
import com.miletracker.feature.events.viewmodel.EventsHistoryViewModel
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
