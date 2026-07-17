package com.mileway.feature.tracking.di

import com.mileway.core.data.search.SearchProvider
import com.mileway.core.network.NetworkMonitor
import com.mileway.core.network.config.ConfigProvider
import com.mileway.core.network.netlog.NetworkLogStore
import com.mileway.feature.tracking.debug.NetworkLogViewModel
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
import com.mileway.feature.tracking.repository.VehiclePricingCache
import com.mileway.feature.tracking.repository.VehiclePricingCacheStore
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.repository.VoucherRepository
import com.mileway.feature.tracking.search.TrackingSearchProvider
import com.mileway.feature.tracking.service.AppSyncTrigger
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.MilesSubmitSyncer
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.SubmissionNotificationThrottler
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.service.TrackingStatePublisher
import com.mileway.feature.tracking.service.realLocationSend
import com.mileway.feature.tracking.service.realMilesSubmitSend
import com.mileway.feature.tracking.viewmodel.CheckInHistoryViewModel
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.DestinationModeViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SosViewModel
import com.mileway.feature.tracking.viewmodel.SyncStatusViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import com.mileway.feature.tracking.watch.WatchFacade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock

/** P-B.3: iOS Koin tracking module — mirrors the Android trackingModule DI bindings. */
val trackingModule =
    module {
        // ── Core tracking plumbing ─────────────────────────────────────────────
        single {
            TrackingConfigManager(
                configProvider = get<ConfigProvider>(),
                abnormalDetectionOverrides = get<com.mileway.core.data.settings.AbnormalDetectionSettingsSource>().overrides,
            )
        }
        single { TrackingStatePublisher() }
        single<TrackingServiceApi> { get<TrackingStatePublisher>() }
        single<TrackingController> {
            IosTrackingController(
                locationTracker = get(),
                statePublisher = get(),
                trackRepository = get(),
                // PLAN_V33 C3/iOS pass: real distance accumulation persists accepted points the same
                // way the Android LocationTrackingService does (via LocationBatcher -> LocationRepository).
                locationRepository = get(),
            )
        }

        // V21 §3 Wave 4: debug network log ring buffer — mirrors the Android trackingModule binding.
        single { NetworkLogStore() }

        // ── Repositories (all in commonMain) ──────────────────────────────────
        single { SavedTrackRepository(get()) }
        single { LocationRepository(get()) }
        // PLAN_V33 A6: offline read-cache backing vehiclesState().
        single { VehiclePricingCacheStore() }
        single<VehiclePricingCache> { get<VehiclePricingCacheStore>() }
        single { VehiclePricingRepository(api = get(), cache = get(), isOnline = NetworkMonitor::isConnectedNow) }
        single { LogMilesSubmissionRepository(get()) }
        single { CurrentTrackRepository(get()) }
        single { HardwareEventRepository(get()) }
        single { TripAttachmentRepository(get()) }
        single { VoucherRepository(get()) }
        // PLAN_V29 P29.S.1: tracking's contribution to master search (F0.5 registry) — the two of
        // the 5 previously-dead SearchEntityType providers this module owns (Mileage/Check-in).
        single<SearchProvider>(named("tracking")) { TrackingSearchProvider(get(), get()) }
        single { SubmissionNotificationThrottler(now = { Clock.System.now().toEpochMilliseconds() }) }
        // Wave 4 §2.3 / PLAN_V33 A4: local sync-status engine over the location outbox — mirrors
        // the Android trackingModule's real-send wiring (see RealLocationSend doc). PLAN_V33 C3:
        // MilewayNetworkApi now resolves via stubModule.ios (must be included by the composition
        // root — see shared/iosMain's MilewayAppViewController/SharedViewController); this stays a
        // no-op FakeTrackingNetworkApi until NetworkBackendFlags.useRealBackend flips.
        single {
            LocationDataSyncer(
                locationDao = get(),
                outbox = get(),
                now = { Clock.System.now().toEpochMilliseconds() },
                send = realLocationSend(api = get(), locationDao = get()),
            )
        }
        // PLAN_V33 A5: durable trip *submission* — mirrors the LocationDataSyncer wiring above.
        // Same caveat as that binding: no-op until NetworkBackendFlags.useRealBackend is flipped.
        single {
            MilesSubmitSyncer(
                outbox = get(),
                trackRepository = get(),
                now = { Clock.System.now().toEpochMilliseconds() },
                send = realMilesSubmitSend(api = get()),
            )
        }

        // PLAN_V34 P1: app-scoped flush triggers — mirrors the Android trackingModule binding (see
        // AppSyncTrigger doc). Started from MilewayAppViewController after initKoin.
        single {
            AppSyncTrigger(
                syncer = get(),
                milesSyncer = get(),
                currentTrackRepo = get(),
                isConnectedFlow = NetworkMonitor.isConnectedFlow,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
            )
        }

        // ── Shared utilities ──────────────────────────────────────────────────
        single { RouteAnalyzer() }

        // DL.5: same watch-domain facade Android's trackingModule binds — the iOS URL-scheme deep-link
        // entry point (DeepLinkActionBridge) dispatches Start/Stop/Pause/Discard through it.
        single { WatchFacade(snapshotPublisher = get(), trackRepository = get(), trackingController = get()) }

        // P-C.4: reconciliation bridge — scene-active hook writes; ViewModel observes.
        single { ReconciliationResultHolder() }
        single { SessionReconciliationPolicy(currentTrackSource = get(), savedTrackRepository = get()) }

        // ── ViewModels (commonMain only; androidMain-exclusive VMs omitted) ───
        viewModelOf(::SavedTracksViewModel)
        viewModelOf(::SyncStatusViewModel)
        viewModelOf(::MultiSessionRestoreViewModel)
        // PLAN_V24 P11.3: the head-home destination panel VM (all deps bound in coreDataModule).
        viewModelOf(::DestinationModeViewModel)
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
                // P3.3: bound by coreDataModule (SessionRepository -> SessionSource).
                sessionSource = getOrNull() ?: com.mileway.feature.tracking.viewmodel.NoSessionSource,
                // P3.5: bound by coreDataModule (ActiveAccountStore -> ActiveAccountSource) and the
                // MockAccountDao registered there.
                activeAccountSource = getOrNull() ?: com.mileway.feature.tracking.viewmodel.NoActiveAccountSource,
                mockAccountDao = getOrNull(),
                // P7.3: bound by coreDataModule (DelegationSessionController -> DelegationSessionSource).
                delegationSource = getOrNull() ?: com.mileway.core.data.session.NoDelegationSessionSource,
                // P11.1: per-km policy-rate overlay; getOrNull() keeps graphs that omit core:data building.
                vehicleRateRepo = getOrNull(),
                // P11.2: garage active-vehicle default; getOrNull() keeps omitting graphs building.
                garageRepo = getOrNull(),
                // P11.3: head-home destination tag at trip start; getOrNull() keeps omitting graphs building.
                destinationModeRepo = getOrNull(),
                // PLAN_V33 C6: bound by platformModule; getOrNull() keeps graphs that omit it on the
                // VM's own unknown-battery default (never blocks a start).
                batteryStatusReader = getOrNull() ?: com.mileway.feature.tracking.viewmodel.UnknownBatteryStatusReader,
            )
        }
        viewModelOf(::MileageSubmissionViewModel)
        viewModelOf(::TrackDetailViewModel)
        viewModelOf(::RoutePointsViewModel)
        viewModelOf(::CheckInHistoryViewModel)
        viewModelOf(::LiveTrackViewModel)
        viewModelOf(::CreateVoucherViewModel)
        // TrackMilesScreen's SosBottomSheet (shown inline when driverEmergencyModeEnabled is on,
        // no navigation involved) needs this — mirrors Android TrackingModule's explicit binding
        // (not viewModelOf, so SosViewModel's Clock default is honored). EmergencyContactsRepository
        // + NotificationDao both resolve from coreDataModule's iOS graph.
        viewModel { SosViewModel(get(), get()) }
        // No HttpClient wired yet (app is offline/:stub) — getOrNull() keeps the tester graceful.
        viewModel { NetworkLogViewModel(store = get(), httpClient = getOrNull()) }
        viewModel { params ->
            TrackingSuccessViewModel(
                args = params.get(),
                vehiclePricingRepository = get(),
                voucherRepository = get(),
            )
        }
    }
