package com.mileway.feature.tracking.di

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
import com.mileway.feature.tracking.repository.VehiclePricingRepository
import com.mileway.feature.tracking.repository.VoucherRepository
import com.mileway.feature.tracking.service.LocationDataSyncer
import com.mileway.feature.tracking.service.ReconciliationResultHolder
import com.mileway.feature.tracking.service.SessionReconciliationPolicy
import com.mileway.feature.tracking.service.SubmissionNotificationThrottler
import com.mileway.feature.tracking.service.TrackingServiceApi
import com.mileway.feature.tracking.service.TrackingStatePublisher
import com.mileway.feature.tracking.viewmodel.CheckInHistoryViewModel
import com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.MultiSessionRestoreViewModel
import com.mileway.feature.tracking.viewmodel.RoutePointsViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.SyncStatusViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.feature.tracking.viewmodel.TrackingSuccessViewModel
import com.mileway.feature.tracking.watch.WatchFacade
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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
            )
        }

        // V21 §3 Wave 4: debug network log ring buffer — mirrors the Android trackingModule binding.
        single { NetworkLogStore() }

        // ── Repositories (all in commonMain) ──────────────────────────────────
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
            )
        }
        viewModelOf(::MileageSubmissionViewModel)
        viewModelOf(::TrackDetailViewModel)
        viewModelOf(::RoutePointsViewModel)
        viewModelOf(::CheckInHistoryViewModel)
        viewModelOf(::LiveTrackViewModel)
        viewModelOf(::CreateVoucherViewModel)
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
