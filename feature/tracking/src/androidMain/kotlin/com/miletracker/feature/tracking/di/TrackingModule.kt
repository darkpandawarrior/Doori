package com.miletracker.feature.tracking.di

import com.miletracker.core.network.config.ConfigProvider
import com.miletracker.core.platform.AndroidNotificationScheduler
import com.miletracker.core.platform.NotificationScheduler
import com.miletracker.feature.tracking.debug.DebugMenuComposeViewModel
import com.miletracker.feature.tracking.insights.RouteAnalyzer
import com.miletracker.feature.tracking.manager.LocationTrackingController
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
import com.miletracker.feature.tracking.viewmodel.CreateVoucherViewModel
import com.miletracker.feature.tracking.viewmodel.ExportViewModel
import com.miletracker.feature.tracking.viewmodel.HardwareEventsViewModel
import com.miletracker.feature.tracking.viewmodel.LiveTrackViewModel
import com.miletracker.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.miletracker.feature.tracking.viewmodel.RoutePointsViewModel
import com.miletracker.feature.tracking.viewmodel.SavedTracksViewModel
import com.miletracker.feature.tracking.viewmodel.TrackDetailViewModel
import com.miletracker.feature.tracking.viewmodel.TrackInsightsViewModel
import com.miletracker.feature.tracking.viewmodel.TrackMilesViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val trackingModule =
    module {
        single { TrackingConfigManager(get<ConfigProvider>()) }
        single { SavedTrackRepository(get()) }
        single { LocationRepository(get()) }
        single { VehiclePricingRepository(get()) }
        single { LogMilesSubmissionRepository(get()) }
        single { CurrentTrackRepository(get()) }
        single { HardwareEventRepository(get()) }
        single { TripAttachmentRepository(get()) }
        single { VoucherRepository() }
        single { LocationTrackingController(androidContext()) }
        single<TrackingController> { get<LocationTrackingController>() }
        single<NotificationScheduler> { AndroidNotificationScheduler(androidContext()) }

        // C.2b/C.3: live tracking telemetry shared from the foreground service to the ViewModel.
        single { com.miletracker.feature.tracking.service.TrackingStatePublisher() }
        single<com.miletracker.feature.tracking.service.TrackingServiceApi> {
            get<com.miletracker.feature.tracking.service.TrackingStatePublisher>()
        }

        // viewModelOf resolves every constructor parameter through Koin, Kotlin default
        // arguments are NOT applied, so the analyzer needs an explicit definition.
        single { RouteAnalyzer() }

        // P-C.4: reconciliation bridge — app-startup writes here; ViewModel observes.
        single { ReconciliationResultHolder() }
        single {
            SessionReconciliationPolicy(
                currentTrackSource = get(),
                savedTrackRepository = get(),
            )
        }

        viewModelOf(::SavedTracksViewModel)
        // Explicit definition (not viewModelOf): LocationNameResolver is bound in platformModule,
        // which some graphs (e.g. the screenshot harness) omit. getOrNull() lets the VM fall back to
        // its own OfflineLocationNameResolver default instead of failing instance creation.
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
        viewModelOf(::HardwareEventsViewModel)
        viewModelOf(::TrackInsightsViewModel)
        viewModelOf(::ExportViewModel)
        viewModelOf(::DebugMenuComposeViewModel)
        viewModelOf(::CreateVoucherViewModel)

        // Per-trip scope, open on trip-start, close on trip-end.
        // Anything declared here is released when the scope closes.
        scope<TrackingScope> {
            scoped { get<com.miletracker.core.data.session.CurrentTrackDataStore>() }
        }
    }
