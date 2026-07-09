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
import com.mileway.core.data.review.SimulatedReviewEngine
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.ActiveAccountStore
import com.mileway.core.data.session.CredentialSource
import com.mileway.core.data.session.CredentialStore
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.session.DelegationSessionController
import com.mileway.core.data.session.DelegationSessionSource
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.MockPostLoginInitializer
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.PinHashStore
import com.mileway.core.data.session.PinLockoutSource
import com.mileway.core.data.session.PinLockoutStore
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.session.SessionSource
import com.mileway.core.data.settings.AbnormalDetectionSettingsSource
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.core.data.settings.AgentSessionStoreImpl
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.data.settings.RegistryAbnormalDetectionSource
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
        single { get<MilewayDatabase>().paymentWalletDao() }
        single { get<MilewayDatabase>().pluginOverrideDao() }
        single { get<MilewayDatabase>().savedPlaceDao() }
        single { get<MilewayDatabase>().emergencyContactDao() }
        // PLAN_V24 P3.5: shared by the profile management screen and the tracking SOS sheet.
        single { com.mileway.core.data.emergency.EmergencyContactsRepository(get()) }
        single { get<MilewayDatabase>().documentDao() }
        single { get<MilewayDatabase>().referralTxnDao() }
        single { get<MilewayDatabase>().couponDao() }
        single { get<MilewayDatabase>().rewardCardDao() }
        single { get<MilewayDatabase>().campaignDao() }
        // PLAN_V24 P5.4: shared by the profile marketing hub and the HomeScreen marketing strip.
        single { com.mileway.core.data.campaign.CampaignRepository(get()) }
        single { get<MilewayDatabase>().subscriptionDao() }
        // PLAN_V24 P6.2: subscription plans + single active-subscription lifecycle (mock purchase).
        single { com.mileway.core.data.subscription.SubscriptionRepository(get()) }
        single { get<MilewayDatabase>().deletionRequestDao() }
        // PLAN_V24 P7.1: account-deletion lifecycle (REQUESTED→PROCESSING via SimulatedReviewEngine).
        single { com.mileway.core.data.lifecycle.DeletionRequestRepository(get(), get()) }
        // PLAN_V24 P0.1: the Plugin Registry — single feature-composition mechanism. The PRESET
        // layer binds to EmptyPersonaPresetProvider until P0.2 supplies the real personas.
        single { PluginDebugForceStore() }
        single<PluginDebugForceSource> { get<PluginDebugForceStore>() }
        single<PersonaPresetProvider> { EmptyPersonaPresetProvider }
        // PLAN_V24 P0.4: the one OTP simulator, injected wherever a purpose needs a code.
        single { LocalOtpEngine() }
        // PLAN_V24 P0.5: async review simulator (KYC, referral payout, deletion, self-audit).
        single { SimulatedReviewEngine() }
        single {
            PluginRegistry(
                overrideDao = get(),
                activeAccount = get(),
                presets = get(),
                debugForce = get(),
            )
        }
        // PLAN_V24 P11.1: per-km policy-rate source (persona-gated by the registry).
        single { com.mileway.core.data.vehicle.VehicleRateRepository(get()) }
        // P7.1: local, no-network post-login profile bootstrap (see MockPostLoginInitializer doc).
        single { MockPostLoginInitializer(get()) }
        single { SessionRepository(get()) }
        single<SessionSource> { get<SessionRepository>() }
        single { CurrentTrackDataStore(get()) }
        single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
        single { ActiveAccountStore() }
        single<ActiveAccountSource> { get<ActiveAccountStore>() }
        // PLAN_V24 P7.3: session-delegation overlay ("Acting as <name>"), persisted like the session.
        single { DelegationSessionController() }
        single<DelegationSessionSource> { get<DelegationSessionController>() }
        single { PinHashStore() }
        single<PinHashSource> { get<PinHashStore>() }
        single { PinLockoutStore() }
        single<PinLockoutSource> { get<PinLockoutStore>() }
        // PLAN_V24 P1.4: a shared clock singleton (PinViewModel injects it for tiered-lockout timing).
        single<kotlin.time.Clock> { kotlin.time.Clock.System }
        // PLAN_V24 P1.5: mock login credential store (change-password + forgot-password).
        single<CredentialSource> { CredentialStore() }
        single { DemoSettingsRepository() }
        // Location switching: recents/favorites/saved places (offline, DataStore-backed).
        single<SavedLocationsSource> { SavedLocationsStore() }
        // PLAN_V24 P10.3: abnormal-detection thresholds are registry-backed VALUE plugins now, so
        // the override source just projects the registry (one store, two surfaces).
        single<AbnormalDetectionSettingsSource> { RegistryAbnormalDetectionSource(get()) }
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
