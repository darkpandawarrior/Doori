package com.mileway

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.dao.DraftExpenseDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.dao.LocationDao
import com.mileway.core.data.dao.LogMilesDraftDao
import com.mileway.core.data.dao.LogMilesFrequentRouteDao
import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.dao.NotificationDao
import com.mileway.core.data.dao.PassportDetailsDao
import com.mileway.core.data.dao.SavedTrackDao
import com.mileway.core.data.dao.SessionDao
import com.mileway.core.data.dao.SignatureDao
import com.mileway.core.data.dao.SupportTicketDao
import com.mileway.core.data.dao.TripAttachmentDao
import com.mileway.core.data.dao.VehicleDetailsDao
import com.mileway.core.data.dao.VoucherDao
import com.mileway.core.data.library.MediaLibraryDao
import com.mileway.core.data.library.MediaLibraryEntry
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.data.session.CurrentTrackDataSource
import com.mileway.core.data.session.CurrentTrackDataStore
import com.mileway.core.data.session.MockAccountSessionCoordinator
import com.mileway.core.data.session.PinHashSource
import com.mileway.core.data.session.SessionRepository
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.maps.MapSurface
import com.mileway.core.platform.ReferralData
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.SystemSettingsOpener
import com.mileway.core.platform.UrlOpener
import com.mileway.core.ui.components.DistanceLedger
import com.mileway.core.ui.components.DistanceLedgerBar
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.platform.LocalNowMs
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.core.ui.theme.MilewayThemeVariant
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.di.agentModule
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.voice.TextToSpeech
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
import com.mileway.feature.cards.di.cardsModule
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.logging.ui.screens.ExpenseScreen
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.payables.di.payablesModule
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.profile.di.profileModule
import com.mileway.feature.profile.ui.screens.ProfileScreen
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.ui.evidence.TrackEvidenceScreen
import com.mileway.feature.tracking.ui.live.LiveDriveActions
import com.mileway.feature.tracking.ui.live.LiveDriveScreen
import com.mileway.feature.tracking.ui.live.LiveDriveState
import com.mileway.feature.tracking.ui.review.DriveReviewSheet
import com.mileway.feature.tracking.ui.screens.SavedTracksScreen
import com.mileway.feature.tracking.ui.screens.TrackDetailScreen
import com.mileway.feature.tracking.ui.sheets.JourneyGuideSheet
import com.mileway.feature.tracking.ui.sheets.JourneyGuideState
import com.mileway.feature.tracking.ui.sheets.JourneyGuideStep
import com.mileway.feature.tracking.viewmodel.TrackMilesPhase
import com.mileway.feature.tracking.viewmodel.TrackSignal
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.feature.travel.di.travelModule
import com.mileway.feature.whatsnew.di.whatsNewFeatureModule
import com.mileway.stub.di.stubModule
import com.mileway.ui.auth.authModule
import com.mileway.ui.home.homeModule
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import com.siddharth.kmp.appshell.LoggingAnalyticsHelper
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.PermissionsProvider
import com.siddharth.kmp.common.CrashReporter
import dev.tmapps.konnection.Konnection
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// ---------------------------------------------------------------------------
// Cross-direction comparison gallery: the SAME ten key screens, rendered once per new
// MilewayThemeVariant (Ledger / Signal / Paper / Instrument / Refined Ember), so the owner can
// flip between variant_<direction>_<screen>.png files for one screen and see the design
// directions side by side. This is a NEW file — ScreenshotGalleryTest.kt and
// ScreenshotCatalogTest.kt are never edited here.
//
// The Koin graph, seeded DAOs, FakeMapSurface and the 411dp viewport config below are copied
// from ScreenshotGalleryTest.kt's companion object (same proven bindings, same reasons — see the
// inline comments carried over) rather than re-derived, since every screen captured here is
// already exercised against this exact graph over there.
//
// Record / update:
//   ROBORAZZI_RECORD=true ./gradlew :app:screenshotTestNoGmsDebug
//
// Output: docs/screenshots/variant_<direction>_<screen>.png
// ---------------------------------------------------------------------------

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotVariantsTest {

    private val screenshotNowMs = 1_767_268_800_000L

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        // The five new directions built for this exercise — DEFAULT/EMBER and the other curated
        // variants are deliberately excluded, this file's whole job is comparing these five.
        private val DIRECTIONS =
            listOf(
                MilewayThemeVariant.LEDGER,
                MilewayThemeVariant.SIGNAL,
                MilewayThemeVariant.PAPER,
                MilewayThemeVariant.INSTRUMENT,
                MilewayThemeVariant.REFINED_EMBER,
            )

        private fun MilewayThemeVariant.slug(): String = id.lowercase()

        // Same PLAN_V33 A6 landmine guard as ScreenshotGalleryTest: VehiclePricingRepository reads
        // Konnection.instance, which only MilewayApplication.onCreate() (skipped by this bare
        // Application) normally sets up.
        private var konnectionInitialized = false

        // Same seeded SavedTrackDao as ScreenshotGalleryTest (FakeSavedTrackDao is a public
        // top-level test class in TrackMilesViewModelTest.kt, same package, reused not redefined).
        private val seededDao =
            FakeSavedTrackDao().also { dao ->
                val baseMs = 1_700_000_000_000L
                dao.preload(completedTrack("route-j1", "Pune → Hinjewadi", 12_400.0, baseMs - 86_400_000L))
                dao.preload(completedTrack("route-j2", "FC Road → Koregaon Park", 3_800.0, baseMs - 172_800_000L))
            }

        private fun completedTrack(routeId: String, name: String, distanceMeters: Double, startMs: Long) =
            SavedTrack(
                routeId = routeId, name = name, isCompleted = true,
                startLatitude = 18.5204, startLongitude = 73.8567,
                endLatitude = 18.5500, endLongitude = 73.8800,
                pausedLatitude = 0.0, pausedLongitude = 0.0,
                startTime = startMs, endTime = startMs + 3_600_000L,
                distance = distanceMeters, duration = 3_600_000L,
                selectedVehicleType = "fourWheelerPetrol", vehiclePricing = 10.0,
                createdAt = startMs, startedAtTimestamp = startMs,
                startedByEmployeeCode = "EMP001",
            )

        private val mediaLibraryDao = mockk<MediaLibraryDao>(relaxed = true).also { dao ->
            every { dao.observeAll() } returns MutableStateFlow(emptyList<MediaLibraryEntry>())
        }

        private val voucherDao = FakeVoucherDao()

        // Same fake data layer as ScreenshotGalleryTest.kt's fakeRoomLayer — trimmed to the
        // bindings this file's six ViewModel-backed screens (TrackDetail, SavedTracks,
        // DriveReviewSheet's MileageSubmissionViewModel, Approvals, Expense, Profile) actually
        // resolve, following that file's documented deterministic-fake pattern throughout (a
        // relaxed mockk's null-backed Flow crashes an eager collectLatest/combine in a VM's init).
        private val fakeRoomLayer =
            module {
                single<SavedTrackDao> { seededDao }
                single<LocationDao> { mockk(relaxed = true) }
                single<HardwareEventDao> { mockk(relaxed = true) }
                single<LogMilesDraftDao> { FakeLogMilesDraftDao() }
                single<LogMilesFrequentRouteDao> { FakeLogMilesFrequentRouteDao() }
                single<com.siddharth.kmp.offlineoutbox.SubmitOutbox<com.mileway.core.data.model.network.LogMilesSubmitRequestV2>> {
                    mockk(relaxed = true)
                }
                single<TripAttachmentDao> { mockk(relaxed = true) }
                single<DraftExpenseDao> { mockk(relaxed = true) }
                single<VoucherDao> { voucherDao }
                single<MediaLibraryDao> { mediaLibraryDao }
                single<AgentDao> { FakeAgentDao() }
                single<MockAccountDao> { FakeMockAccountDao() }
                single<com.mileway.core.data.outbox.TripDraftOutbox> { FakeTripDraftOutbox() }
                single<VehicleDetailsDao> { FakeVehicleDetailsDao() }
                single<PassportDetailsDao> { FakePassportDetailsDao() }
                single<SignatureDao> { FakeSignatureDao() }
                single<DelegationDao> { FakeDelegationDao() }
                single<SessionDao> { FakeSessionDao() }
                single<NotificationDao> { FakeNotificationDao() }
                single<ConnectedAccountDao> { FakeConnectedAccountDao() }
                single<com.mileway.core.data.dao.PaymentWalletDao> { FakePaymentWalletDao() }
                single { com.mileway.core.data.otp.LocalOtpEngine() }
                single { com.mileway.core.data.review.SimulatedReviewEngine() }
                single<com.mileway.core.data.dao.SavedPlaceDao> { FakeSavedPlaceDao() }
                single<com.mileway.core.data.dao.EmergencyContactDao> { FakeEmergencyContactDao() }
                single { com.mileway.core.data.emergency.EmergencyContactsRepository(get()) }
                single<com.mileway.core.data.dao.DocumentDao> { FakeDocumentDao() }
                single<com.mileway.core.data.dao.ReferralTxnDao> { FakeReferralTxnDao() }
                single<com.mileway.core.data.dao.CouponDao> { FakeCouponDao() }
                single<com.mileway.core.data.dao.RewardCardDao> { FakeRewardCardDao() }
                single<com.mileway.core.data.dao.CampaignDao> { FakeCampaignDao() }
                single { com.mileway.core.data.campaign.CampaignRepository(get()) }
                single<com.mileway.core.data.dao.ClarificationDao> { FakeClarificationDao() }
                single<com.mileway.core.data.dao.ApprovalCommentDao> { FakeApprovalCommentDao() }
                single<com.mileway.core.data.dao.SubscriptionDao> { FakeSubscriptionDao() }
                single { com.mileway.core.data.subscription.SubscriptionRepository(get()) }
                single<com.mileway.core.data.dao.DeletionRequestDao> { FakeDeletionRequestDao() }
                single { com.mileway.core.data.lifecycle.DeletionRequestRepository(get(), get()) }
                single<kotlin.time.Clock> { kotlin.time.Clock.System }
                single<com.mileway.core.data.session.PinLockoutSource> {
                    object : com.mileway.core.data.session.PinLockoutSource {
                        override suspend fun getState(accountId: String) =
                            com.mileway.core.data.session.PinLockoutState()

                        override suspend fun setState(
                            accountId: String,
                            state: com.mileway.core.data.session.PinLockoutState,
                        ) = Unit

                        override suspend fun clear(accountId: String) = Unit
                    }
                }
                single<com.mileway.core.data.location.SavedLocationsSource> {
                    object : com.mileway.core.data.location.SavedLocationsSource {
                        override val data = MutableStateFlow(com.mileway.core.data.location.SavedLocationsData())

                        override suspend fun addRecent(place: com.mileway.core.data.location.SavedPlace) = Unit

                        override suspend fun removeRecent(name: String) = Unit

                        override suspend fun clearRecent() = Unit

                        override suspend fun toggleFavorite(place: com.mileway.core.data.location.SavedPlace) = Unit

                        override suspend fun saveAs(
                            place: com.mileway.core.data.location.SavedPlace,
                            label: String,
                        ) = Unit

                        override suspend fun removeSaved(label: String) = Unit
                    }
                }
                single<SupportTicketDao> { FakeSupportTicketDao() }
                single<AgentSessionStore> { FakeAgentSessionStore() }
                single<AssistantEngine> { FakeAssistantEngine() }
                single<SpeechToText> { FakeSpeechToText() }
                single<TextToSpeech> { FakeTextToSpeech() }
                single<CurrentTrackDataStore> { mockk(relaxed = true) }
                single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
                single<ActiveAccountSource> { FakeActiveAccountSource() }
                single<com.mileway.core.data.session.DelegationSessionSource> {
                    com.mileway.core.data.session.InMemoryDelegationSessionSource()
                }
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
                single<PinHashSource> { FakePinHashSource() }
                single<DemoSettingsRepository> {
                    mockk {
                        every { settings } returns MutableStateFlow(com.mileway.core.data.settings.DemoSettings())
                    }
                }
                single<com.mileway.core.data.settings.AbnormalDetectionSettingsSource> {
                    mockk {
                        every { overrides } returns
                            MutableStateFlow(com.mileway.core.data.settings.AbnormalDetectionOverrides())
                    }
                }
                single<SessionRepository> {
                    mockk(relaxed = true) {
                        every { sessionState } returns MutableStateFlow(com.mileway.core.data.session.SessionState())
                    }
                }
                single { MockAccountSessionCoordinator(get(), get(), get()) }
                single<MapSurface> { FakeMapSurface() }
                single<SystemSettingsOpener> { object : SystemSettingsOpener { override fun openAppSettings() = Unit } }
                single<com.mileway.core.data.dao.BugReportDao> { mockk(relaxed = true) }
                single { com.mileway.core.data.support.BugReportRepository(get()) }
                single<com.mileway.core.data.dao.FavouriteRouteDao> { mockk(relaxed = true) { every { observeAll() } returns MutableStateFlow(emptyList()) } }
                single { com.mileway.core.data.favourite.FavouriteRoutesRepository(get(), get()) }
                single<com.mileway.core.data.dao.VehicleDao> {
                    mockk(relaxed = true) {
                        every { observeAll() } returns MutableStateFlow(emptyList())
                        every { observeActive() } returns MutableStateFlow(null)
                    }
                }
                single { com.mileway.core.data.vehicle.GarageRepository(get()) }
                single<com.mileway.core.data.dao.VehicleAuditDao> {
                    mockk(relaxed = true) { every { observeForVehicle(any()) } returns MutableStateFlow(emptyList()) }
                }
                single { com.mileway.core.data.vehicle.SelfAuditRepository(get(), get()) }
                single { com.mileway.core.data.vehicle.EcometerRepository(get()) }
                single<com.mileway.core.data.dao.TourProgressDao> {
                    mockk(relaxed = true) { every { observe(any()) } returns MutableStateFlow(null) }
                }
                single { com.mileway.core.data.engagement.TourRepository(get(), get()) }
            }

        private val fakeOverrides =
            module {
                // Same VehiclePricingCache override reasoning as ScreenshotGalleryTest's fakeOverrides
                // (PLAN_V33 A6 landmine): TrackingModule's real VehiclePricingCacheStore needs a
                // working Context.filesDir the mock androidContext() here doesn't provide.
                single<com.mileway.feature.tracking.repository.VehiclePricingCache> {
                    com.mileway.feature.tracking.repository.InMemoryVehiclePricingCache()
                }
                single<NotificationScheduler> { mockk(relaxed = true) }
                single<com.mileway.core.platform.BiometricAuthenticator> { mockk(relaxed = true) }
                single<ReferralManager> {
                    object : ReferralManager {
                        override suspend fun myReferralCode(): String = "MILEWAY-SID-9F2K"

                        override fun pendingReferral(): kotlinx.coroutines.flow.Flow<ReferralData?> =
                            kotlinx.coroutines.flow.emptyFlow()

                        override suspend fun redeem(code: String): Boolean = true
                    }
                }
                single<AnalyticsHelper> { LoggingAnalyticsHelper() }
                single<CrashReporter> { mockk(relaxed = true) }
                single<AppUpdateManagerFactory> { mockk(relaxed = true) }
                single<AppReviewManagerFactory> { mockk(relaxed = true) }
                single<ShareSheet> { mockk(relaxed = true) }
                single<PermissionsProvider> { mockk(relaxed = true) }
                single<UrlOpener> { mockk(relaxed = true) }
                single<AgentAnalyticsStore> { FakeAgentAnalyticsStore() }
            }

        @BeforeClass
        @JvmStatic
        fun setup() {
            if (System.getenv("ROBORAZZI_RECORD") == "true") {
                System.setProperty("roborazzi.test.record", "true")
            } else {
                System.setProperty("roborazzi.test.verify", "true")
            }
            try {
                stopKoin()
            } catch (_: Exception) {
            }
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
                    travelModule,
                    cardsModule,
                    agentModule,
                    paymentsModule,
                    eventsModule,
                    homeModule,
                    whatsNewFeatureModule,
                    authModule,
                    com.mileway.ui.auth.pinModule,
                    appModule,
                    fakeOverrides,
                )
            }
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            try {
                stopKoin()
            } catch (_: Exception) {
            }
        }
    }

    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @org.junit.Before
    fun initComposeResources() {
        ComposeResourcesTestFixture.install()
        org.jetbrains.compose.resources.setResourceReaderAndroidContext(
            ApplicationProvider.getApplicationContext(),
        )
        if (!konnectionInitialized) {
            Konnection.createInstance(ApplicationProvider.getApplicationContext())
            konnectionInitialized = true
        }
    }

    // Bare composables (no Scaffold ancestor of their own) fall through to Robolectric's white
    // root canvas — see ScreenshotGalleryTest's ThemedBackground doc for the full explanation.
    // This variant is theme-aware so each direction's real background paints, not just Ember's.
    @Composable
    private fun ThemedBackground(
        variant: MilewayThemeVariant,
        content: @Composable () -> Unit,
    ) {
        MilewayTheme(milewayTheme = variant) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                content()
            }
        }
    }

    private fun capture(name: String) {
        if (System.getenv("ROBORAZZI_RECORD") == "true") {
            composeRule.onRoot().captureRoboImage(File(screenshotsDir, "$name.png").absolutePath)
        } else {
            composeRule.onRoot().captureRoboImage("$name.png")
        }
    }

    // ── Hero surface: live tracking HUD ──────────────────────────────────────────────

    @Composable
    private fun LiveDriveContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            LiveDriveScreen(
                state =
                    LiveDriveState(
                        phase = TrackMilesPhase.TRACKING,
                        distanceKm = 12.42,
                        elapsedMs = 1_421_000L,
                        speedKmh = 48.0,
                        avgSpeedKmh = 31.5,
                        maxSpeedKmh = 62.0,
                        pointsCount = 842L,
                        qualityScore = 94,
                        batteryPct = 68,
                        isCharging = false,
                        unsyncedPoints = 12L,
                        pauseReason = null,
                        currentLat = 18.5204,
                        currentLng = 73.8567,
                        bearingDegrees = 118f,
                        signal = TrackSignal.GOOD,
                        systemFlags = TrackingSystemFlags(),
                    ),
                actions = LiveDriveActions({}, {}, {}, {}),
            )
        }
    }

    // ── Decision sheet: the trip-end confirm/submit moment ───────────────────────────

    @Composable
    private fun DriveReviewContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            DriveReviewSheet(
                routeId = "route-j1",
                distanceKm = 12.4,
                vehicleKey = "fourWheelerPetrol",
                startTime = 1_700_000_000_000L,
                endTime = 1_700_003_600_000L,
                onTrackNewJourney = {},
                onViewExpense = {},
                onCreateVoucher = {},
            )
        }
    }

    // ── Detail surface ─────────────────────────────────────────────────────────────

    @Composable
    private fun TrackDetailContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            TrackDetailScreen(
                routeId = "route-j1",
                onBack = {},
                onOpenInsights = {},
                onOpenMap = {},
                onOpenHwEvents = {},
                onOpenDataPreview = {},
            )
        }
    }

    // ── Dense list ────────────────────────────────────────────────────────────────

    @Composable
    private fun SavedTracksContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            SavedTracksScreen(onTrackClick = {}, onStartNew = {})
        }
    }

    // ── Evidence / receipt surface ────────────────────────────────────────────────

    @Composable
    private fun TrackEvidenceContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            TrackEvidenceScreen(
                track =
                    SavedTrack(
                        routeId = "route-e1", name = "Kothrud to Hinjewadi", isCompleted = true,
                        startLatitude = 18.5074, startLongitude = 73.8077,
                        endLatitude = 18.5913, endLongitude = 73.7389,
                        pausedLatitude = 0.0, pausedLongitude = 0.0,
                        startTime = 1_767_268_800_000L, endTime = 1_767_272_400_000L,
                        distance = 14_900.0, duration = 3_600_000L,
                        selectedVehicleType = "fourWheelerPetrol", vehiclePricing = 10.0,
                        createdAt = 1_767_268_800_000L, startedAtTimestamp = 1_767_268_800_000L,
                        startedByEmployeeCode = "EMP001",
                    ),
            )
        }
    }

    // ── Dense list: approvals queue ──────────────────────────────────────────────

    @Composable
    private fun ApprovalsContent(variant: MilewayThemeVariant) {
        CompositionLocalProvider(LocalNowMs provides { screenshotNowMs }) {
            MilewayTheme(milewayTheme = variant) {
                ApprovalsScreen(onOpenDetail = {})
            }
        }
    }

    // ── Form ──────────────────────────────────────────────────────────────────────

    @Composable
    private fun ExpenseContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            ExpenseScreen(onBack = {}, onSubmitted = {})
        }
    }

    // ── Decision sheet: the guided start-trip wizard ─────────────────────────────

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Composable
    private fun JourneyGuideContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            JourneyGuideSheet(
                state =
                    JourneyGuideState(
                        step = JourneyGuideStep.VEHICLE,
                        vehicleName = "Honda City",
                        vehicleRatePerKm = 10.0,
                        startOdometer = null,
                        draftEnabled = false,
                        requiresOdometer = true,
                    ),
                onPickVehicle = {},
                onCaptureOdometer = {},
                onToggleDraft = {},
                onStartTracking = {},
                onDismiss = {},
            )
        }
    }

    // ── Profile hub ───────────────────────────────────────────────────────────────

    @Composable
    private fun ProfileContent(variant: MilewayThemeVariant) {
        MilewayTheme(milewayTheme = variant) {
            ProfileScreen(
                onOpenDetails = {},
                onOpenPreferences = {},
                onOpenNotifications = {},
                onOpenSettings = {},
                onOpenAboutSupport = {},
                onOpenAdvance = {},
                onOpenCards = {},
                onOpenInsights = {},
                onOpenDelegation = {},
                onOpenDemoSettings = {},
                onOpenQr = {},
            )
        }
    }

    // ── The money moment: the distance ledger receipt ────────────────────────────

    private val demoLedger =
        DistanceLedger(
            rawKm = 14.9,
            cleanedKm = 13.6,
            claimedKm = 13.6,
            abnormalKm = 0.4,
            mockKm = 0.0,
            spikeKm = 0.9,
            odometerKm = 13.5,
        )

    @Composable
    private fun DistanceLedgerContent(variant: MilewayThemeVariant) {
        ThemedBackground(variant) {
            DistanceLedgerBar(ledger = demoLedger, modifier = Modifier.padding(16.dp))
        }
    }

    // ── liveDrive across all five directions ──
    @Test
    fun variants_liveDrive_ledger() {
        composeRule.setContent { LiveDriveContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_live_drive_screen")
    }

    @Test
    fun variants_liveDrive_signal() {
        composeRule.setContent { LiveDriveContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_live_drive_screen")
    }

    @Test
    fun variants_liveDrive_paper() {
        composeRule.setContent { LiveDriveContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_live_drive_screen")
    }

    @Test
    fun variants_liveDrive_instrument() {
        composeRule.setContent { LiveDriveContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_live_drive_screen")
    }

    @Test
    fun variants_liveDrive_refined_ember() {
        composeRule.setContent { LiveDriveContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_live_drive_screen")
    }

    // ── driveReview across all five directions ──
    @Test
    fun variants_driveReview_ledger() {
        composeRule.setContent { DriveReviewContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_drive_review_sheet")
    }

    @Test
    fun variants_driveReview_signal() {
        composeRule.setContent { DriveReviewContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_drive_review_sheet")
    }

    @Test
    fun variants_driveReview_paper() {
        composeRule.setContent { DriveReviewContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_drive_review_sheet")
    }

    @Test
    fun variants_driveReview_instrument() {
        composeRule.setContent { DriveReviewContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_drive_review_sheet")
    }

    @Test
    fun variants_driveReview_refined_ember() {
        composeRule.setContent { DriveReviewContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_drive_review_sheet")
    }

    // ── trackDetail across all five directions ──
    @Test
    fun variants_trackDetail_ledger() {
        composeRule.setContent { TrackDetailContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_track_detail_screen")
    }

    @Test
    fun variants_trackDetail_signal() {
        composeRule.setContent { TrackDetailContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_track_detail_screen")
    }

    @Test
    fun variants_trackDetail_paper() {
        composeRule.setContent { TrackDetailContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_track_detail_screen")
    }

    @Test
    fun variants_trackDetail_instrument() {
        composeRule.setContent { TrackDetailContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_track_detail_screen")
    }

    @Test
    fun variants_trackDetail_refined_ember() {
        composeRule.setContent { TrackDetailContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_track_detail_screen")
    }

    // ── savedTracks across all five directions ──
    @Test
    fun variants_savedTracks_ledger() {
        composeRule.setContent { SavedTracksContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_saved_tracks_screen")
    }

    @Test
    fun variants_savedTracks_signal() {
        composeRule.setContent { SavedTracksContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_saved_tracks_screen")
    }

    @Test
    fun variants_savedTracks_paper() {
        composeRule.setContent { SavedTracksContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_saved_tracks_screen")
    }

    @Test
    fun variants_savedTracks_instrument() {
        composeRule.setContent { SavedTracksContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_saved_tracks_screen")
    }

    @Test
    fun variants_savedTracks_refined_ember() {
        composeRule.setContent { SavedTracksContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_saved_tracks_screen")
    }

    // ── trackEvidence across all five directions ──
    @Test
    fun variants_trackEvidence_ledger() {
        composeRule.setContent { TrackEvidenceContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_track_evidence_screen")
    }

    @Test
    fun variants_trackEvidence_signal() {
        composeRule.setContent { TrackEvidenceContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_track_evidence_screen")
    }

    @Test
    fun variants_trackEvidence_paper() {
        composeRule.setContent { TrackEvidenceContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_track_evidence_screen")
    }

    @Test
    fun variants_trackEvidence_instrument() {
        composeRule.setContent { TrackEvidenceContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_track_evidence_screen")
    }

    @Test
    fun variants_trackEvidence_refined_ember() {
        composeRule.setContent { TrackEvidenceContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_track_evidence_screen")
    }

    // ── approvals across all five directions ──
    @Test
    fun variants_approvals_ledger() {
        composeRule.setContent { ApprovalsContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_approvals_screen")
    }

    @Test
    fun variants_approvals_signal() {
        composeRule.setContent { ApprovalsContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_approvals_screen")
    }

    @Test
    fun variants_approvals_paper() {
        composeRule.setContent { ApprovalsContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_approvals_screen")
    }

    @Test
    fun variants_approvals_instrument() {
        composeRule.setContent { ApprovalsContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_approvals_screen")
    }

    @Test
    fun variants_approvals_refined_ember() {
        composeRule.setContent { ApprovalsContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_approvals_screen")
    }

    // ── expense across all five directions ──
    @Test
    fun variants_expense_ledger() {
        composeRule.setContent { ExpenseContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_expense_screen")
    }

    @Test
    fun variants_expense_signal() {
        composeRule.setContent { ExpenseContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_expense_screen")
    }

    @Test
    fun variants_expense_paper() {
        composeRule.setContent { ExpenseContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_expense_screen")
    }

    @Test
    fun variants_expense_instrument() {
        composeRule.setContent { ExpenseContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_expense_screen")
    }

    @Test
    fun variants_expense_refined_ember() {
        composeRule.setContent { ExpenseContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_expense_screen")
    }

    // ── journeyGuide across all five directions ──
    @Test
    fun variants_journeyGuide_ledger() {
        composeRule.setContent { JourneyGuideContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_journey_guide_sheet")
    }

    @Test
    fun variants_journeyGuide_signal() {
        composeRule.setContent { JourneyGuideContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_journey_guide_sheet")
    }

    @Test
    fun variants_journeyGuide_paper() {
        composeRule.setContent { JourneyGuideContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_journey_guide_sheet")
    }

    @Test
    fun variants_journeyGuide_instrument() {
        composeRule.setContent { JourneyGuideContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_journey_guide_sheet")
    }

    @Test
    fun variants_journeyGuide_refined_ember() {
        composeRule.setContent { JourneyGuideContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_journey_guide_sheet")
    }

    // ── profile across all five directions ──
    @Test
    fun variants_profile_ledger() {
        composeRule.setContent { ProfileContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_profile_screen")
    }

    @Test
    fun variants_profile_signal() {
        composeRule.setContent { ProfileContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_profile_screen")
    }

    @Test
    fun variants_profile_paper() {
        composeRule.setContent { ProfileContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_profile_screen")
    }

    @Test
    fun variants_profile_instrument() {
        composeRule.setContent { ProfileContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_profile_screen")
    }

    @Test
    fun variants_profile_refined_ember() {
        composeRule.setContent { ProfileContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_profile_screen")
    }

    // ── distanceLedger across all five directions ──
    @Test
    fun variants_distanceLedger_ledger() {
        composeRule.setContent { DistanceLedgerContent(MilewayThemeVariant.LEDGER) }
        capture("variant_ledger_distance_ledger")
    }

    @Test
    fun variants_distanceLedger_signal() {
        composeRule.setContent { DistanceLedgerContent(MilewayThemeVariant.SIGNAL) }
        capture("variant_signal_distance_ledger")
    }

    @Test
    fun variants_distanceLedger_paper() {
        composeRule.setContent { DistanceLedgerContent(MilewayThemeVariant.PAPER) }
        capture("variant_paper_distance_ledger")
    }

    @Test
    fun variants_distanceLedger_instrument() {
        composeRule.setContent { DistanceLedgerContent(MilewayThemeVariant.INSTRUMENT) }
        capture("variant_instrument_distance_ledger")
    }

    @Test
    fun variants_distanceLedger_refined_ember() {
        composeRule.setContent { DistanceLedgerContent(MilewayThemeVariant.REFINED_EMBER) }
        capture("variant_refined_ember_distance_ledger")
    }

}
