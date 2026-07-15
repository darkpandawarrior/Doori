package com.mileway.feature.tracking.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.network.netlog.NetworkLogStore
import com.mileway.core.platform.NotificationChannels
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
import com.mileway.feature.tracking.search.TrackingSearchProvider
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.MilesSubmitSyncer
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.SubmissionNotificationThrottler
import com.mileway.feature.tracking.service.realLocationSend
import com.mileway.feature.tracking.service.realMilesSubmitSend
import com.mileway.feature.tracking.viewmodel.CheckInHistoryViewModel
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.DestinationModeViewModel
import com.mileway.feature.tracking.viewmodel.ExportViewModel
import com.mileway.feature.tracking.viewmodel.HardwareEventsViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SosViewModel
import com.mileway.feature.tracking.viewmodel.SyncStatusViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackInsightsViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import com.mileway.feature.tracking.watch.WatchFacade
import com.siddharth.kmp.appshell.AndroidNotificationScheduler
import com.siddharth.kmp.appshell.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
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
        // PLAN_V29 P29.S.1: tracking's contribution to master search (F0.5 registry) — the two of
        // the 5 previously-dead SearchEntityType providers this module owns (Mileage/Check-in).
        single<SearchProvider>(named("tracking")) { TrackingSearchProvider(get(), get()) }
        single { SubmissionNotificationThrottler(now = { Clock.System.now().toEpochMilliseconds() }) }
        // Wave 4 §2.3 / PLAN_V33 A4: local sync-status engine over the location outbox — `send` is
        // now the real Ktor-backed send (see RealLocationSend doc); with the default `:stub`
        // MilewayNetworkApi binding this still resolves to SUCCESS every time, so demo-mode sync
        // behavior is unchanged until NetworkBackendFlags.useRealBackend is flipped on.
        single {
            LocationDataSyncer(
                locationDao = get(),
                outbox = get(),
                now = { Clock.System.now().toEpochMilliseconds() },
                send = realLocationSend(api = get(), locationDao = get()),
            )
        }
        // PLAN_V33 A5: durable trip *submission* — mirrors the LocationDataSyncer wiring above but
        // drains the submit outbox (SubmitOutbox<TripDraft>, bound in CoreDataModule) instead.
        single {
            MilesSubmitSyncer(
                outbox = get(),
                trackRepository = get(),
                now = { Clock.System.now().toEpochMilliseconds() },
                send = realMilesSubmitSend(api = get()),
            )
        }
        single { LocationTrackingController(androidContext()) }
        single<TrackingController> { get<LocationTrackingController>() }
        // Same channel as PlatformModule.android.kt's binding (NotificationChannels.GENERAL) — this
        // module's copy wins Koin's last-registration-wins override, so it must match, not diverge to
        // :app-shell's own default channel id.
        single<NotificationScheduler> {
            AndroidNotificationScheduler(androidContext(), channelId = NotificationChannels.GENERAL, channelName = "General")
        }

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
        // PLAN_V24 P11.3: the head-home destination panel VM (all deps bound in coreDataModule).
        viewModelOf(::DestinationModeViewModel)
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
                // P7.3: bound by coreDataModule (DelegationSessionController -> DelegationSessionSource);
                // omitted graphs fall back to the VM's never-acting default.
                delegationSource = getOrNull() ?: com.mileway.core.data.session.NoDelegationSessionSource,
                // P11.1: per-km policy-rate overlay; getOrNull() keeps graphs that omit core:data building.
                vehicleRateRepo = getOrNull(),
                // P11.2: garage active-vehicle default; getOrNull() keeps omitting graphs building.
                garageRepo = getOrNull(),
                // P11.3: head-home destination tag at trip start; getOrNull() keeps omitting graphs building.
                destinationModeRepo = getOrNull(),
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
        // PLAN_V24 P3.5: SOS sheet — explicit (not viewModelOf) so the Clock default is honored;
        // EmergencyContactsRepository + NotificationDao resolve from core:data's graph.
        viewModel { SosViewModel(get(), get()) }
        // P10.3: PluginRegistry (core:data) resolved for the fine-tuning readout; getOrNull() keeps
        // graphs that omit core:data (screenshot harness) building.
        viewModel { DebugMenuComposeViewModel(get(), get(), getOrNull()) }
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
