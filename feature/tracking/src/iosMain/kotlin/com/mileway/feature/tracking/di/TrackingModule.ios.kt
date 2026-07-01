package com.mileway.feature.tracking.di

import com.mileway.core.network.config.ConfigProvider
import com.mileway.feature.tracking.insights.RouteAnalyzer
import com.mileway.feature.tracking.manager.IosTrackingController
import com.mileway.feature.tracking.manager.TrackingConfigManager
import com.mileway.feature.tracking.manager.TrackingController
import com.mileway.feature.tracking.repository.CurrentTrackRepository
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.mileway.feature.tracking.repository.LocationRepository
import com.mileway.feature.tracking.repository.LogMilesSubmissionRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import com.mileway.feature.tracking.repository.TripAttachmentRepository
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.repository.VoucherRepository
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.service.TrackingStatePublisher
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
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
                locationNameResolver = getOrNull() ?: com.mileway.core.platform.OfflineLocationNameResolver(),
                reconciliationHolder = get(),
            )
        }
        viewModelOf(::MileageSubmissionViewModel)
        viewModelOf(::TrackDetailViewModel)
        viewModelOf(::RoutePointsViewModel)
        viewModelOf(::LiveTrackViewModel)
        viewModelOf(::CreateVoucherViewModel)
    }
