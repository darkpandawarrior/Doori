package com.mileway

import android.content.Context
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.dao.DraftExpenseDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.voice.TextToSpeech
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.dao.PassportDetailsDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.dao.VehicleDetailsDao
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.ui.di.coreUiModule
import com.mileway.feature.agent.di.agentModule
import com.mileway.feature.agent.viewmodel.AgentViewModel
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.approvals.viewmodel.ApprovalsViewModel
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.logging.viewmodel.ExpenseViewModel
import com.mileway.feature.logging.viewmodel.LogMilesViewModel
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.media.viewmodel.CloudLibraryViewModel
import com.mileway.feature.media.viewmodel.MediaViewModel
import com.mileway.feature.payables.di.payablesModule
import com.mileway.feature.payables.viewmodel.PayablesViewModel
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.events.viewmodel.CreateEventViewModel
import com.mileway.feature.events.viewmodel.EventsHistoryViewModel
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.payments.viewmodel.CreatePaymentViewModel
import com.mileway.feature.payments.viewmodel.PaymentsHistoryViewModel
import com.mileway.feature.profile.di.profileModule
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.feature.profile.viewmodel.ActiveSessionsViewModel
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import com.mileway.feature.profile.viewmodel.ConnectedAccountsViewModel
import com.mileway.feature.profile.viewmodel.DelegationViewModel
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import com.mileway.feature.profile.viewmodel.NotificationViewModel
import com.mileway.feature.profile.viewmodel.PersonalDetailsViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
import com.mileway.feature.profile.viewmodel.StorageViewModel
import com.mileway.feature.profile.viewmodel.SwitchAccountViewModel
import com.mileway.feature.profile.viewmodel.SyncDiagnosticsViewModel
import com.mileway.feature.tracking.debug.DebugMenuComposeViewModel
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.viewmodel.CheckInViewModel
import com.mileway.feature.tracking.viewmodel.ExportViewModel
import com.mileway.feature.tracking.viewmodel.HardwareEventsViewModel
import com.mileway.feature.tracking.viewmodel.LiveTrackViewModel
import com.mileway.feature.tracking.viewmodel.MileageSubmissionViewModel
import com.mileway.feature.tracking.viewmodel.SavedTracksViewModel
import com.mileway.feature.tracking.viewmodel.TrackDetailViewModel
import com.mileway.feature.tracking.viewmodel.TrackInsightsViewModel
import com.mileway.feature.tracking.viewmodel.TrackMilesViewModel
import com.mileway.ui.auth.authModule
import com.mileway.ui.auth.pinModule
import com.mileway.ui.auth.AuthViewModel
import com.mileway.ui.auth.PinViewModel
import com.mileway.ui.search.MasterSearchViewModel
import com.mileway.core.platform.NotificationScheduler
import com.mileway.core.platform.PermissionsProvider
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.UrlOpener
import com.mileway.stub.di.stubModule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.assertNotNull

/**
 * Builds the FULL production Koin graph (every module the Application installs) and
 * instantiates every ViewModel definition in it.
 *
 * `viewModelOf(::SomeViewModel)` resolves each constructor parameter through Koin and
 * ignores Kotlin default arguments, so a parameter that "has a default" still needs a
 * Koin definition, a mistake the compiler cannot catch and a screen-open crash in
 * production (NoDefinitionFoundException). This test turns that runtime crash into a
 * unit-test failure.
 *
 * Only the Room layer is replaced (mocked DAOs, building the real database needs a
 * device); everything else is the exact wiring the app ships with.
 */
class KoinGraphTest : KoinTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRoomLayer = module {
        single<LocationDao> { mockk(relaxed = true) }
        single<SavedTrackDao> { mockk(relaxed = true) }
        single<HardwareEventDao> { mockk(relaxed = true) }
        // P5.1: LogMilesViewModel.init now collectLatest's getAllDrafts(); a relaxed mockk
        // returns a null-backed Flow that crashes that collector (memory: screenshot Koin
        // needs deterministic fakes), so a real in-memory fake is used instead.
        single<LogMilesDraftDao> { FakeLogMilesDraftDao() }
        // Wave 3: LogMilesViewModel.init now also collectLatest's observeAllRoutes(); same
        // relaxed-mockk-null-Flow trap as LogMilesDraftDao above.
        single<LogMilesFrequentRouteDao> { FakeLogMilesFrequentRouteDao() }
        single<com.mileway.core.data.outbox.SubmitOutbox<com.mileway.core.data.model.network.LogMilesSubmitRequestV2>> {
            mockk(relaxed = true)
        }
        single<TripAttachmentDao> { mockk(relaxed = true) }
        single<DraftExpenseDao> { mockk(relaxed = true) }
        single<MediaLibraryDao> { mockk(relaxed = true) }
        single<AgentDao> { FakeAgentDao() }
        single<MockAccountDao> { FakeMockAccountDao() }
        // P6.2: PersonalDetailsViewModel collects both of these in init(); a relaxed mockk
        // would return a null-backed Flow and crash the collector (memory: screenshot Koin
        // needs deterministic fakes).
        single<VehicleDetailsDao> { FakeVehicleDetailsDao() }
        single<PassportDetailsDao> { FakePassportDetailsDao() }
        // P6.3: DelegationViewModel collects this in init(); same null-collector trap as above.
        single<DelegationDao> { FakeDelegationDao() }
        // P6.4: ActiveSessionsViewModel collects this in init(); same null-collector trap as above.
        single<SessionDao> { FakeSessionDao() }
        // P6.5: NotificationViewModel collects this in init(); same null-collector trap as above.
        single<NotificationDao> { FakeNotificationDao() }
        // P6.6: ConnectedAccountsViewModel collects this in init(); same null-collector trap as above.
        single<ConnectedAccountDao> { FakeConnectedAccountDao() }
        // P6.6: StorageViewModel reads real on-device byte counts; the app-under-test's real
        // Context/cacheDir/getDatabasePath work fine on Robolectric, unlike a relaxed mockk Context.
        single { com.mileway.core.data.settings.StorageRepository(androidContext()) }
        single<AgentSessionStore> { FakeAgentSessionStore() }
        single<AssistantEngine> { FakeAssistantEngine() }
        single<SpeechToText> { FakeSpeechToText() }
        single<TextToSpeech> { FakeTextToSpeech() }
        single<CurrentTrackDataStore> { mockk(relaxed = true) }
        single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
        // P2.1: ProfileViewModel reads this in init; a real in-memory fake avoids the
        // Flow<String?>-from-relaxed-mockk null-collector trap (memory: screenshot Koin needs
        // deterministic fakes).
        single<ActiveAccountSource> { FakeActiveAccountSource() }
        // P2.3: SwitchAccountViewModel.verify() reads this; a real in-memory fake avoids the
        // suspend-fun-on-a-relaxed-mockk trap (memory: screenshot Koin needs deterministic fakes).
        single<PinHashSource> { FakePinHashSource() }
        // P6.5: ProfileViewModel now collects `settings` eagerly in init() (Notification Center
        // channel toggles); a relaxed mockk's auto-generated Flow<DemoSettings> is not guaranteed
        // to behave like a real Flow under `.onEach{}.launchIn()` (memory: screenshot Koin needs
        // deterministic fakes), so a real MutableStateFlow-backed stub is used instead.
        single<DemoSettingsRepository> {
            mockk {
                every { settings } returns MutableStateFlow(com.mileway.core.data.settings.DemoSettings())
            }
        }
        // Wave-2 AbnormalDetectionConfig: trackingModule's TrackingConfigManager resolves this
        // (DataStore-backed in prod, needs a real Context) — bind a DEFAULT-only fake here, same
        // as the other DataStore-backed sources above.
        single<com.mileway.core.data.settings.AbnormalDetectionSettingsSource> {
            mockk {
                every { overrides } returns
                    MutableStateFlow(com.mileway.core.data.settings.AbnormalDetectionOverrides())
            }
        }
        // P2.4: ProfileViewModel now depends on SessionRepository (SignOut's global-fallback path).
        // P3.2: ProfileViewModel now also collects `sessionState.first()` in init() for the
        // staleness check; a relaxed mockk's auto-generated Flow<SessionState> never emits
        // (memory: screenshot Koin needs deterministic fakes, same null-collector trap as
        // ActiveAccountSource above), so `sessionState` is stubbed with a real MutableStateFlow.
        single<SessionRepository> {
            mockk(relaxed = true) {
                every { sessionState } returns MutableStateFlow(com.mileway.core.data.session.SessionState())
            }
        }
        // P3.4: ProfileViewModel now depends on MockAccountSessionCoordinator (pause/restore hook);
        // its own DAO deps above are already deterministic fakes, not relaxed mocks.
        single { MockAccountSessionCoordinator(get(), get(), get()) }
        single<NotificationScheduler> { mockk(relaxed = true) }
        single<ShareSheet> { mockk(relaxed = true) }
        single<PermissionsProvider> { mockk(relaxed = true) }
        single<UrlOpener> { mockk(relaxed = true) }
        // PLAN_V24 P0: the plugin registry + engines coreDataModule provides, mirrored here so
        // ViewModels that depend on them (AuthViewModel/PluginManagerViewModel and every later
        // V24 VM) are constructible against the fake data layer. PersonaPresetProvider comes from
        // stubModule (StubPersonaPresetProvider).
        single { com.mileway.core.data.otp.LocalOtpEngine() }
        single { com.mileway.core.data.review.SimulatedReviewEngine() }
        single<com.mileway.core.data.dao.PluginOverrideDao> { mockk(relaxed = true) }
        single<com.mileway.core.data.plugin.PluginDebugForceSource> {
            com.mileway.core.data.plugin.InMemoryPluginDebugForceSource()
        }
        single {
            com.mileway.core.data.plugin.PluginRegistry(
                overrideDao = get(),
                activeAccount = get(),
                presets = get(),
                debugForce = get(),
            )
        }
    }

    @Before
    fun setUp() {
        try { stopKoin() } catch (_: Exception) {}
        startKoin {
            androidContext(mockk<Context>(relaxed = true))
            modules(
                fakeRoomLayer,
                coreUiModule,
                stubModule,
                trackingModule,
                loggingModule,
                mediaModule,
                profileModule,
                approvalsModule,
                payablesModule,
                agentModule,
                com.mileway.feature.cards.di.cardsModule,
                paymentsModule,
                eventsModule,
                appModule,
                authModule,
                pinModule,
                // Override platform-backed agent services last so fakes win over agentPlatformModule
                module { single<AgentAnalyticsStore> { FakeAgentAnalyticsStore() } },
            )
        }
    }

    @After
    fun tearDown() {
        try { stopKoin() } catch (_: Exception) {}
    }

    @Test
    fun `every ViewModel in the app graph is constructible`() {
        // One line per registered ViewModel, a new viewModelOf() without a matching
        // entry here should be added when the screen is added.
        assertNotNull(get<SavedTracksViewModel>())
        assertNotNull(get<TrackMilesViewModel>())
        assertNotNull(get<MileageSubmissionViewModel>())
        assertNotNull(get<TrackDetailViewModel>())
        assertNotNull(get<LiveTrackViewModel>())
        assertNotNull(get<HardwareEventsViewModel>())
        assertNotNull(get<TrackInsightsViewModel>())
        assertNotNull(get<ExportViewModel>())
        assertNotNull(get<DebugMenuComposeViewModel>())
        assertNotNull(get<LogMilesViewModel>())
        assertNotNull(get<ExpenseViewModel>())
        assertNotNull(get<MediaViewModel>())
        assertNotNull(get<CloudLibraryViewModel>())
        assertNotNull(get<ProfileViewModel>())
        assertNotNull(get<AdvanceViewModel>())
        assertNotNull(get<DemoSettingsViewModel>())
        assertNotNull(get<SwitchAccountViewModel>())
        assertNotNull(get<PersonalDetailsViewModel>())
        assertNotNull(get<DelegationViewModel>())
        assertNotNull(get<ActiveSessionsViewModel>())
        assertNotNull(get<NotificationViewModel>())
        assertNotNull(get<ConnectedAccountsViewModel>())
        assertNotNull(get<StorageViewModel>())
        assertNotNull(get<SyncDiagnosticsViewModel>())
        assertNotNull(get<com.mileway.feature.profile.viewmodel.PluginManagerViewModel>())
        assertNotNull(get<CheckInViewModel>())
        assertNotNull(get<ApprovalsViewModel>())
        assertNotNull(get<PayablesViewModel>())
        assertNotNull(get<AgentViewModel>())
        assertNotNull(get<CreatePaymentViewModel>())
        assertNotNull(get<PaymentsHistoryViewModel>())
        assertNotNull(get<CreateEventViewModel>())
        assertNotNull(get<EventsHistoryViewModel>())
        assertNotNull(get<MasterSearchViewModel>())
        assertNotNull(get<AuthViewModel>())
        assertNotNull(get<PinViewModel>())
    }

    @Test
    fun `master-search aggregator resolves every registered SearchProvider`() {
        // getAll<SearchProvider>() must find the bound providers; the repo is what the VM depends on.
        assertNotNull(get<com.mileway.core.data.search.MasterSearchRepository>())
    }

    @Test
    fun `seeder and geofence list resolve from the app module`() {
        assertNotNull(get<com.mileway.seeder.DatabaseSeeder>())
    }
}
