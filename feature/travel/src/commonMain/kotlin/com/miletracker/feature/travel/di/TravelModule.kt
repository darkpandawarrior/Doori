package com.miletracker.feature.travel.di

import com.miletracker.feature.travel.repository.TravelRepository
import com.miletracker.feature.travel.viewmodel.TravelViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val travelModule =
    module {
        single { TravelRepository() }
        viewModelOf(::TravelViewModel)
    }
