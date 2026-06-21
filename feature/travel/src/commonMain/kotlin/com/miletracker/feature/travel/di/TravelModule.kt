package com.miletracker.feature.travel.di

import com.miletracker.feature.travel.repository.TravelCreateRepository
import com.miletracker.feature.travel.repository.TravelRepository
import com.miletracker.feature.travel.viewmodel.CreateBusViewModel
import com.miletracker.feature.travel.viewmodel.CreateFlightViewModel
import com.miletracker.feature.travel.viewmodel.CreateHotelViewModel
import com.miletracker.feature.travel.viewmodel.CreateTripViewModel
import com.miletracker.feature.travel.viewmodel.TravelViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val travelModule =
    module {
        single { TravelRepository() }
        // TR.2+: shared offline rotating-status create store for the travel create suite.
        single { TravelCreateRepository() }
        viewModelOf(::TravelViewModel)
        viewModelOf(::CreateTripViewModel)
        viewModelOf(::CreateFlightViewModel)
        viewModelOf(::CreateBusViewModel)
        viewModelOf(::CreateHotelViewModel)
    }
