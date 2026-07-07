package com.mileway.core.data.di

import com.mileway.core.data.database.MilewayDatabase
import com.mileway.core.data.database.buildMilewayDatabase
import com.mileway.core.data.location.SavedLocationsSource
import com.mileway.core.data.location.SavedLocationsStore
import com.mileway.core.data.model.display.InMemorySnapshotPublisher
import com.mileway.core.data.model.display.SnapshotPublisher
import com.mileway.core.data.model.network.LogMilesSubmitRequestV2
import com.mileway.core.data.otp.LocalOtpEngine
import com.mileway.core.data.outbox.LocationBatch
import com.mileway.core.data.outbox.LocationBatchOutbox
import com.mileway.core.data.outbox.RoomSubmitOutbox
import com.mileway.core.data.outbox.SubmitOutbox
import com.mileway.core.data.outbox.TripDraft
import com.mileway.core.data.plugin.EmptyPersonaPresetProvider
import com.mileway.core.data.plugin.PersonaPresetProvider
import com.mileway.core.data.plugin.PluginDebugForceSource
import com.mileway.core.data.plugin.PluginDebugForceStore
import com.mileway.core.data.plugin.PluginRegistry
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.ActiveAccountStore
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.MockPostLoginInitializer
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.PinHashStore
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.settings.AbnormalDetectionSettingsDataStore
import com.mileway.core.data.settings.AbnormalDetectionSettingsSource
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.core.data.settings.AgentSessionStoreImpl
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.watch.SnapshotCache
import com.mileway.core.data.watch.SnapshotCacheStore
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val coreDataModule =
    module {
        single<MilewayDatabase> { buildMilewayDatabase() }
        single { get<MilewayDatabase>().locationDao() }
        single { get<MilewayDatabase>().savedTrackDao() }
        single { get<MilewayDatabase>().hardwareEventDao() }
        single { get<MilewayDatabase>().logMilesDraftDao() }
        single { get<MilewayDatabase>().logMilesFrequentRouteDao() }
        single { get<MilewayDatabase>().tripAttachmentDao() }
        single { get<MilewayDatabase>().mediaLibraryDao() }
        single { get<MilewayDatabase>().submitDraftDao() }
        single { get<MilewayDatabase>().agentDao() }
        single { get<MilewayDatabase>().draftExpenseDao() }
        single { get<MilewayDatabase>().voucherDao() }
        single { get<MilewayDatabase>().mockAccountDao() }
        single { get<MilewayDatabase>().vehicleDetailsDao() }
        single { get<MilewayDatabase>().passportDetailsDao() }
        single { get<MilewayDatabase>().delegationDao() }
        single { get<MilewayDatabase>().sessionDao() }
        single { get<MilewayDatabase>().notificationDao() }
        single { get<MilewayDatabase>().connectedAccountDao() }
        single { get<MilewayDatabase>().pluginOverrideDao() }
        // PLAN_V24 P0.1: the Plugin Registry — single feature-composition mechanism. The PRESET
        // layer binds to EmptyPersonaPresetProvider until P0.2 supplies the real personas.
        single { PluginDebugForceStore() }
        single<PluginDebugForceSource> { get<PluginDebugForceStore>() }
        single<PersonaPresetProvider> { EmptyPersonaPresetProvider }
        // PLAN_V24 P0.4: the one OTP simulator, injected wherever a purpose needs a code.
        single { LocalOtpEngine() }
        single {
            PluginRegistry(
                overrideDao = get(),
                activeAccount = get(),
                presets = get(),
                debugForce = get(),
            )
        }
        // P7.1: local, no-network post-login profile bootstrap (see MockPostLoginInitializer doc).
        single { MockPostLoginInitializer(get()) }
        single { SessionRepository(get()) }
        single<SessionSource> { get<SessionRepository>() }
        single { CurrentTrackDataStore(get()) }
        single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
        single { ActiveAccountStore() }
        single<ActiveAccountSource> { get<ActiveAccountStore>() }
        single { PinHashStore() }
        single<PinHashSource> { get<PinHashStore>() }
        single { DemoSettingsRepository() }
        // Location switching: recents/favorites/saved places (offline, DataStore-backed).
        single<SavedLocationsSource> { SavedLocationsStore() }
        single { AbnormalDetectionSettingsDataStore() }
        single<AbnormalDetectionSettingsSource> { get<AbnormalDetectionSettingsDataStore>() }
        // P3.4: pause/restore hook run before ProfileViewModel.CommitAccountSwitch flips the
        // active-account pointer.
        single { MockAccountSessionCoordinator(get(), get(), get()) }
        single<AgentSessionStore> { AgentSessionStoreImpl() }
        single { Json { ignoreUnknownKeys = true } }
        single<SubmitOutbox<TripDraft>> { RoomSubmitOutbox(get(), get(), TripDraft.serializer()) }
        // Wave 3: Log Miles submit goes through the durable outbox too (LogMilesSubmitUseCase).
        single<SubmitOutbox<LogMilesSubmitRequestV2>> { RoomSubmitOutbox(get(), get(), LogMilesSubmitRequestV2.serializer()) }
        // Wave 4 §2.3: location batches drained by LocationDataSyncer go through the same outbox.
        single<LocationBatchOutbox> { RoomSubmitOutbox(get(), get(), LocationBatch.serializer()) }
        // L.1: in-process snapshot channel for glanceable surfaces (publish + observe).
        single { InMemorySnapshotPublisher() }
        single<SnapshotPublisher> { get<InMemorySnapshotPublisher>() }
        // P6.1: cross-process snapshot cache for the widget/extension process.
        single { SnapshotCacheStore() }
        single<SnapshotCache> { get<SnapshotCacheStore>() }
    }
