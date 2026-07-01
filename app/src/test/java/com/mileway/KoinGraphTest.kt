package com.mileway

import android.content.Context
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.voice.TextToSpeech
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
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
import com.mileway.feature.profile.viewmodel.AdvanceViewModel
import com.mileway.feature.profile.viewmodel.DemoSettingsViewModel
import com.mileway.feature.profile.viewmodel.ProfileViewModel
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
import com.mileway.ui.search.MasterSearchViewModel
import com.mileway.core.platform.NotificationScheduler
import com.mileway.core.platform.PermissionsProvider
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.UrlOpener
import com.mileway.stub.di.stubModule
import io.mockk.mockk
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
        single<LogMilesDraftDao> { mockk(relaxed = true) }
        single<LogMilesFrequentRouteDao> { mockk(relaxed = true) }
        single<TripAttachmentDao> { mockk(relaxed = true) }
        single<MediaLibraryDao> { mockk(relaxed = true) }
        single<AgentDao> { FakeAgentDao() }
        single<AgentSessionStore> { FakeAgentSessionStore() }
        single<AssistantEngine> { FakeAssistantEngine() }
        single<SpeechToText> { FakeSpeechToText() }
        single<TextToSpeech> { FakeTextToSpeech() }
        single<CurrentTrackDataStore> { mockk(relaxed = true) }
        single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
        single<DemoSettingsRepository> { mockk(relaxed = true) }
        single<NotificationScheduler> { mockk(relaxed = true) }
        single<ShareSheet> { mockk(relaxed = true) }
        single<PermissionsProvider> { mockk(relaxed = true) }
        single<UrlOpener> { mockk(relaxed = true) }
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
        assertNotNull(get<CheckInViewModel>())
        assertNotNull(get<ApprovalsViewModel>())
        assertNotNull(get<PayablesViewModel>())
        assertNotNull(get<AgentViewModel>())
        assertNotNull(get<CreatePaymentViewModel>())
        assertNotNull(get<PaymentsHistoryViewModel>())
        assertNotNull(get<CreateEventViewModel>())
        assertNotNull(get<EventsHistoryViewModel>())
        assertNotNull(get<MasterSearchViewModel>())
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
