package com.mileway.feature.tracking.di

import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.network.netlog.NetworkLogStore
import com.mileway.core.platform.AndroidNotificationScheduler
import com.mileway.core.platform.NotificationScheduler
import com.mileway.feature.tracking.debug.DebugMenuComposeViewModel
import com.mileway.feature.tracking.debug.NetworkLogViewModel
import com.mileway.feature.tracking.insights.RouteAnalyzer
import com.mileway.feature.tracking.manager.LocationTrackingController
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
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.SubmissionNotificationThrottler
import com.mileway.feature.tracking.viewmodel.CheckInHistoryViewModel
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.ExportViewModel
import com.mileway.feature.tracking.viewmodel.HardwareEventsViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SyncStatusViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackInsightsViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import com.mileway.feature.tracking.watch.WatchFacade
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.time.Clock

val trackingModule =
    module {
        single {
            TrackingConfigManager(
                configProvider = get<ConfigProvider>(),
                abnormalDetectionOverrides = get<com.mileway.core.data.settings.AbnormalDetectionSettingsSource>().overrides,
            )
        }
        // V21 §3 Wave 4: debug network log ring buffer, shared by NetworkLogPlugin (installed on
        // whichever HttpClient the host wires) and the NetworkLogScreen debug UI.
        single { NetworkLogStore() }
        single { SavedTrackRepository(get()) }
        single { LocationRepository(get()) }
        single { VehiclePricingRepository(get()) }
        single { LogMilesSubmissionRepository(get()) }
        single { CurrentTrackRepository(get()) }
        single { HardwareEventRepository(get()) }
        single { TripAttachmentRepository(get()) }
        single { VoucherRepository(get()) }
        single { SubmissionNotificationThrottler(now = { Clock.System.now().toEpochMilliseconds() }) }
        // Wave 4 §2.3: local-only sync-status engine over the location outbox — see class doc.
        single {
            LocationDataSyncer(
                locationDao = get(),
                outbox = get(),
                now = { Clock.System.now().toEpochMilliseconds() },
            )
        }
        single { LocationTrackingController(androidContext()) }
        single<TrackingController> { get<LocationTrackingController>() }
        single<NotificationScheduler> { AndroidNotificationScheduler(androidContext()) }

        // C.2b/C.3: live tracking telemetry shared from the foreground service to the ViewModel.
        single { com.mileway.feature.tracking.service.TrackingStatePublisher() }
        single<com.mileway.feature.tracking.service.TrackingServiceApi> {
            get<com.mileway.feature.tracking.service.TrackingStatePublisher>()
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

        // P1.2/P2.4: the shared watch-domain facade (Wear OS Compose today; the headless watchOS
        // KMP framework later) — resolves through the same SnapshotPublisher/SavedTrackRepository/
        // TrackingController bindings above, so :wear's WearAppGraph gets it for free.
        single { WatchFacade(snapshotPublisher = get(), trackRepository = get(), trackingController = get()) }

        viewModelOf(::SavedTracksViewModel)
        viewModelOf(::SyncStatusViewModel)
        viewModelOf(::MultiSessionRestoreViewModel)
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
                hardwareEventRepo = get(),
                geoCheckInLocations = getOrNull() ?: emptyList(),
                trackingServiceApi = get(),
                locationNameResolver = getOrNull() ?: com.mileway.core.platform.OfflineLocationNameResolver(),
                reconciliationHolder = get(),
                // P3.3: bound by coreDataModule (SessionRepository -> SessionSource); omitted graphs
                // (e.g. the screenshot Koin harness) fall back to the VM's own default.
                sessionSource = getOrNull() ?: com.mileway.feature.tracking.viewmodel.NoSessionSource,
                // P3.5: bound by coreDataModule (ActiveAccountStore -> ActiveAccountSource) and the
                // MockAccountDao registered there; omitted graphs fall back to the VM's own defaults.
                activeAccountSource = getOrNull() ?: com.mileway.feature.tracking.viewmodel.NoActiveAccountSource,
                mockAccountDao = getOrNull(),
            )
        }
        viewModelOf(::MileageSubmissionViewModel)
        viewModelOf(::TrackDetailViewModel)
        viewModelOf(::RoutePointsViewModel)
        viewModelOf(::CheckInHistoryViewModel)
        viewModelOf(::LiveTrackViewModel)
        viewModelOf(::HardwareEventsViewModel)
        viewModelOf(::TrackInsightsViewModel)
        viewModelOf(::ExportViewModel)
        viewModelOf(::DebugMenuComposeViewModel)
        // No HttpClient wired yet (app is offline/:stub) — getOrNull() keeps the tester graceful.
        viewModel { NetworkLogViewModel(store = get(), httpClient = getOrNull()) }
        viewModelOf(::CreateVoucherViewModel)
        // Takes the nav-supplied TrackingSuccessArgs as a runtime parameter; repos resolve from graph.
        viewModel { params ->
            TrackingSuccessViewModel(
                args = params.get(),
                vehiclePricingRepository = get(),
                voucherRepository = get(),
            )
        }

        // Per-trip scope, open on trip-start, close on trip-end.
        // Anything declared here is released when the scope closes.
        scope<TrackingScope> {
            scoped { get<com.mileway.core.data.session.CurrentTrackDataStore>() }
        }
    }
