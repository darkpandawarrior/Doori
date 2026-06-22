package com.miletracker.feature.tracking.di

import com.miletracker.core.network.config.ConfigProvider
import com.miletracker.feature.tracking.insights.RouteAnalyzer
import com.miletracker.feature.tracking.manager.IosTrackingController
import com.miletracker.feature.tracking.manager.TrackingConfigManager
import com.miletracker.feature.tracking.manager.TrackingController
import com.miletracker.feature.tracking.repository.CurrentTrackRepository
import com.miletracker.feature.tracking.repository.HardwareEventRepository
import com.miletracker.feature.tracking.repository.LocationRepository
import com.miletracker.feature.tracking.repository.LogMilesSubmissionRepository
import com.miletracker.feature.tracking.repository.SavedTrackRepository
import com.miletracker.feature.tracking.repository.TripAttachmentRepository
import com.miletracker.feature.tracking.repository.VehiclePricingRepository
import com.miletracker.feature.tracking.repository.VoucherRepository
import com.miletracker.feature.tracking.service.ReconciliationResultHolder
import com.miletracker.feature.tracking.service.SessionReconciliationPolicy
import com.miletracker.feature.tracking.service.TrackingServiceApi
import com.miletracker.feature.tracking.service.TrackingStatePublisher
import com.miletracker.feature.tracking.viewmodel.CreateVoucherViewModel
import com.miletracker.feature.tracking.viewmodel.LiveTrackViewModel
import com.miletracker.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.miletracker.feature.tracking.viewmodel.RoutePointsViewModel
import com.miletracker.feature.tracking.viewmodel.SavedTracksViewModel
import com.miletracker.feature.tracking.viewmodel.TrackDetailViewModel
import com.miletracker.feature.tracking.viewmodel.TrackMilesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** P-B.3: iOS Koin tracking module — mirrors the Android trackingModule DI bindings. */
val trackingModule =
    module {
        // ── Core tracking plumbing ─────────────────────────────────────────────
        single { TrackingConfigManager(get<ConfigProvider>()) }
        single { TrackingStatePublisher() }
        single<TrackingServiceApi> { get<TrackingStatePublisher>() }
        single<TrackingController> {
            IosTrackingController(
                locationTracker = get(),
                statePublisher = get(),
                trackRepository = get(),
            )
        }

        // ── Repositories (all in commonMain) ──────────────────────────────────
        single { SavedTrackRepository(get()) }
        single { LocationRepository(get()) }
        single { VehiclePricingRepository(get()) }
        single { LogMilesSubmissionRepository(get()) }
        single { CurrentTrackRepository(get()) }
        single { HardwareEventRepository(get()) }
        single { TripAttachmentRepository(get()) }
        single { VoucherRepository() }

        // ── Shared utilities ──────────────────────────────────────────────────
        single { RouteAnalyzer() }

        // P-C.4: reconciliation bridge — scene-active hook writes; ViewModel observes.
        single { ReconciliationResultHolder() }
        single { SessionReconciliationPolicy(currentTrackSource = get(), savedTrackRepository = get()) }

        // ── ViewModels (commonMain only; androidMain-exclusive VMs omitted) ───
        viewModelOf(::SavedTracksViewModel)
        viewModel {
            TrackMilesViewModel(
                configManager = get(),
                vehicleRepo = get(),
                trackRepo = get(),
                trackingController = get(),
                currentTrackRepo = get(),
                locationRepo = get(),
                geoCheckInLocations = getOrNull() ?: emptyList(),
                trackingServiceApi = get(),
                locationNameResolver = getOrNull() ?: com.miletracker.core.platform.OfflineLocationNameResolver(),
                reconciliationHolder = get(),
            )
        }
        viewModelOf(::MileageSubmissionViewModel)
        viewModelOf(::TrackDetailViewModel)
        viewModelOf(::RoutePointsViewModel)
        viewModelOf(::LiveTrackViewModel)
        viewModelOf(::CreateVoucherViewModel)
    }
