package com.miletracker.feature.travel.di

import com.miletracker.core.data.search.SearchProvider
import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.repository.TravelHistoryRepository
import com.miletracker.feature.travel.repository.TravelRepository
import com.miletracker.feature.travel.search.TravelSearchProvider
import com.miletracker.feature.travel.viewmodel.BookingHistoryViewModel
import com.miletracker.feature.travel.viewmodel.CreateBusViewModel
import com.miletracker.feature.travel.viewmodel.CreateFlightViewModel
import com.miletracker.feature.travel.viewmodel.CreateHotelViewModel
import com.miletracker.feature.travel.viewmodel.CreateMjpViewModel
import com.miletracker.feature.travel.viewmodel.CreateTripViewModel
import com.miletracker.feature.travel.viewmodel.CreateVisaViewModel
import com.miletracker.feature.travel.viewmodel.TravelViewModel
import com.miletracker.feature.travel.viewmodel.TripHistoryViewModel
import org.koin.core.module.dsl.viewModelOf
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
        single<SearchProvider> { TravelSearchProvider(get()) }
    }
