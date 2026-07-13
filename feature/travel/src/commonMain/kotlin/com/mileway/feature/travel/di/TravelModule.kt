package com.mileway.feature.travel.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.feature.travel.repository.TravelCreateRepository
import com.mileway.feature.travel.repository.TravelHistoryRepository
import com.mileway.feature.travel.repository.TravelRepository
import com.mileway.feature.travel.search.TravelSearchProvider
import com.mileway.feature.travel.viewmodel.BookingHistoryViewModel
import com.mileway.feature.travel.viewmodel.CreateBusViewModel
import com.mileway.feature.travel.viewmodel.CreateFlightViewModel
import com.mileway.feature.travel.viewmodel.CreateHotelViewModel
import com.mileway.feature.travel.viewmodel.CreateMjpViewModel
import com.mileway.feature.travel.viewmodel.CreateTripViewModel
import com.mileway.feature.travel.viewmodel.CreateVisaViewModel
import com.mileway.feature.travel.viewmodel.TravelViewModel
import com.mileway.feature.travel.viewmodel.TripHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val travelModule =
    module {
        single { TravelRepository() }
        // TR.2+: shared offline rotating-status create store for the travel create suite.
        single { TravelCreateRepository() }
        // TR.8: offline trip + booking history store (also the TR.9 search source).
        single { TravelHistoryRepository() }
        viewModelOf(::TravelViewModel)
        viewModelOf(::CreateTripViewModel)
        viewModelOf(::CreateFlightViewModel)
        viewModelOf(::CreateBusViewModel)
        viewModelOf(::CreateHotelViewModel)
        viewModelOf(::CreateMjpViewModel)
        viewModelOf(::CreateVisaViewModel)
        viewModelOf(::TripHistoryViewModel)
        viewModelOf(::BookingHistoryViewModel)
        // TR.9: travel contribution to master search (getAll<SearchProvider>() picks it up).
        // PLAN_V29 P29.S.1: named qualifier so this doesn't silently override another module's
        // SearchProvider binding — see TrackingSearchProvider's doc for the full why.
        single<SearchProvider>(named("travel")) { TravelSearchProvider(get()) }
    }
