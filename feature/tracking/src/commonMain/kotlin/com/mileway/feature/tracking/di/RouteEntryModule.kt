package com.mileway.feature.tracking.di

import com.mileway.core.network.routing.OsrmClient
import com.mileway.core.network.routing.OsrmConfiguration
import com.mileway.feature.tracking.places.SavedPlacesRepository
import com.mileway.feature.tracking.route.PointToPointViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Routing config is deliberately empty until a self-hosted server is entered. */
val routeEntryModule = module {
    single { OsrmConfiguration() }
    single { OsrmClient(get()) }
    single { SavedPlacesRepository(get(), get()) }
    viewModel { PointToPointViewModel(get(), get(), get(), get(), get()) }
}
