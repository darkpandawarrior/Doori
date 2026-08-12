package com.mileway

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
import com.mileway.core.data.model.display.TrackingSystemFlags
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
// Design-direction comparison gallery: the SAME 8 screens, rendered under all FIVE
// MilewayThemeVariant design directions (LEDGER / SIGNAL / PAPER / INSTRUMENT /
// REFINED_EMBER), so the owner can compare like-for-like and pick one.
//
// This is a SEPARATE producer from ScreenshotGalleryTest.kt / ScreenshotCatalogTest.kt —
// deliberately not touching either. The Koin graph, seeded DAOs and fakes below are copied
// verbatim from ScreenshotGalleryTest.kt's companion object (same reasoning, same landmines);
// see that file for the "why" comments on any individual binding.
//
// Screen set (8) and why each earns a slot:
//  - LiveDriveScreen    — hero surface: the live-tracking HUD, the app's most-seen screen.
//  - DriveReviewSheet   — the money moment: distance -> reimbursable amount.
//  - SavedTracksScreen  — dense list #1: personal journey history.
//  - ApprovalsScreen    — dense list #2: a different domain (financial approval queue) so the
//                         list treatment is compared across more than one context.
//  - TrackDetailScreen  — a details surface: map + stat rows + actions.
//  - ExpenseScreen      — a form: category chips, amount entry, validation states.
//  - JourneyGuideSheet  — a decision sheet: the pre-tracking vehicle/odometer gate.
//  - TrackEvidenceScreen — trip evidence composition (route summary + receipts).
//
// Record:
//   ROBORAZZI_RECORD=true ./gradlew :app:screenshotTestNoGmsDebug --tests "com.mileway.ScreenshotDirectionsTest"
//
// Output: docs/screenshots/dir_<variant>_<screen>.png
// ---------------------------------------------------------------------------

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotDirectionsTest {
    private val screenshotNowMs = 1_767_268_800_000L

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        private var konnectionInitialized = false

        private val seededDao = FakeSavedTrackDao().also { dao ->
            val baseMs = 1_700_000_000_000L
            dao.preload(completedTrack("route-j1", "Pune -> Hinjewadi", 12_400.0, baseMs - 86_400_000L))
            dao.preload(completedTrack("route-j2", "FC Road -> Koregaon Park", 3_800.0, baseMs - 172_800_000L))
            dao.preload(completedTrack("route-j3", "Camp -> Hadapsar", 7_100.0, baseMs - 259_200_000L))
            dao.preload(submittedTrack("route-s1", "Kothrud -> Baner", 9_200.0, baseMs - 432_000_000L))
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
                startedByEmployeeCode = "EMP001"
            )

        private fun submittedTrack(routeId: String, name: String, distanceMeters: Double, startMs: Long) =
            completedTrack(routeId, name, distanceMeters, startMs).copy(
                serverUploaded = true, submittedAmount = distanceMeters / 1000.0 * 10.0,
                submissionTime = startMs + 3_600_000L + 600_000L, pettyId = 9001L
            )

        private val mediaLibraryDao = mockk<MediaLibraryDao>(relaxed = true).also { dao ->
            val baseMs = 1_700_000_000_000L
            val entries = listOf(
                MediaLibraryEntry("m1", "file:///demo/odometer.jpg", "image/jpeg", "Odometer: Pune", "CAMERA", baseMs - 3_600_000L),
                MediaLibraryEntry("m2", "file:///demo/fuel.jpg", "image/jpeg", "Fuel receipt: Hinjewadi", "GALLERY", baseMs - 7_200_000L),
            )
            every { dao.observeAll() } returns MutableStateFlow(entries)
        }

        private val voucherDao = FakeVoucherDao()

        private val fakeRoomLayer = module {
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
            single<com.mileway.core.data.dao.FavouriteRouteDao> {
                mockk(relaxed = true) {
                    every { observeAll() } returns
                        MutableStateFlow(
                            listOf(
                                com.mileway.core.data.model.db.FavouriteRouteEntity(
                                    id = "fav-1", sourceTrackId = "route-j1", name = "Home to Office",
                                    purpose = "Business", distanceKm = 12.4, createdAtMs = 1_700_000_000_000L,
                                ),
                            ),
                        )
                }
            }
            single { com.mileway.core.data.favourite.FavouriteRoutesRepository(get(), get()) }
            single<com.mileway.core.data.dao.VehicleDao> {
                mockk(relaxed = true) {
                    every { observeAll() } returns
                        MutableStateFlow(
                            listOf(
                                com.mileway.core.data.model.db.VehicleEntity(
                                    id = "veh_seed_1", brand = "Honda", model = "Activa",
                                    registrationNumber = "MH12AB1234", year = 2022, color = "Grey",
                                    seats = 2, vehicleTypeKey = "twoWheeler", isActive = true,
                                ),
                            ),
                        )
                    every { observeActive() } returns MutableStateFlow(null)
                }
            }
            single { com.mileway.core.data.vehicle.GarageRepository(get()) }
            single<com.mileway.core.data.dao.VehicleAuditDao> {
                mockk(relaxed = true) {
                    every { observeForVehicle(any()) } returns MutableStateFlow(emptyList())
                }
            }
            single { com.mileway.core.data.vehicle.SelfAuditRepository(get(), get()) }
            single { com.mileway.core.data.vehicle.EcometerRepository(get()) }
            single<com.mileway.core.data.dao.TourProgressDao> {
                mockk(relaxed = true) {
                    every { observe(any()) } returns MutableStateFlow(null)
                }
            }
            single { com.mileway.core.data.engagement.TourRepository(get(), get()) }
            single {
                val cache = kotlin.io.path.createTempDirectory("mileway-directions-cache").toFile()
                val databases = kotlin.io.path.createTempDirectory("mileway-directions-db").toFile()
                val files = kotlin.io.path.createTempDirectory("mileway-directions-files").toFile()
                val storageContext =
                    mockk<Context> {
                        every { cacheDir } returns cache
                        every { filesDir } returns files
                        every { getDatabasePath(any()) } answers { File(databases, firstArg()) }
                    }
                com.mileway.core.data.settings.StorageRepository(storageContext)
            }
        }

        private val fakeOverrides = module {
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

        @BeforeClass @JvmStatic
        fun setup() {
            if (System.getenv("ROBORAZZI_RECORD") == "true") {
                System.setProperty("roborazzi.test.record", "true")
            } else {
                System.setProperty("roborazzi.test.verify", "true")
            }
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

        @AfterClass @JvmStatic
        fun teardown() {
            try { stopKoin() } catch (_: Exception) {}
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

    // ── demo composables — one per screen, no theme wrapper (that's applied per-test below so
    // each of the 5 directions renders the identical screen state) ──────────────────────────

    @Composable
    private fun demoLiveDrive() {
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

    @Composable
    private fun demoDriveReview() {
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

    @Composable
    private fun demoSavedTracks() {
        SavedTracksScreen(onTrackClick = {}, onStartNew = {})
    }

    @Composable
    private fun demoApprovals() {
        CompositionLocalProvider(LocalNowMs provides { screenshotNowMs }) {
            ApprovalsScreen(onOpenDetail = {})
        }
    }

    @Composable
    private fun demoTrackDetail() {
        TrackDetailScreen(
            routeId = "route-j1",
            onBack = {},
            onOpenInsights = {},
            onOpenMap = {},
            onOpenHwEvents = {},
            onOpenDataPreview = {},
        )
    }

    @Composable
    private fun demoExpense() {
        ExpenseScreen(onBack = {}, onSubmitted = {})
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun demoJourneyGuide() {
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

    @Composable
    private fun demoTrackEvidence() {
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

    /**
     * Reproduces ScreenshotGalleryTest's ThemedBackground pattern, parametrized by direction: an
     * explicit Box(background = MaterialTheme.colorScheme.background) ancestor so a composable
     * that relies on the app's real ambient Scaffold (which this harness doesn't mount) doesn't
     * fall through to Robolectric's undecorated white canvas. None of the 8 screens below
     * actually need it (each owns its own Scaffold/Surface, same as their ScreenshotGalleryTest
     * captures) but it costs nothing to apply uniformly and removes the failure mode entirely.
     */

    // ── PAPER, NIGHT ────────────────────────────────────────────────────────────────────
    // Paper's hand-built dark counterpart. Captured as a pair with the light face so the site
    // can wipe between them: same layout, same content, only luminance differs, which is
    // exactly the comparison a drag-divider is good at.

    @Test
    fun dirPaperNightApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoApprovals() } }
        capture("dir_paper_night_approvals")
    }

    @Test
    fun dirPaperNightTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoTrackDetail() } }
        capture("dir_paper_night_track_detail")
    }

    @Test
    fun dirPaperNightExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoExpense() } }
        capture("dir_paper_night_expense")
    }

    @Test
    fun dirPaperNightLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoLiveDrive() } }
        capture("dir_paper_night_live_drive")
    }

    @Test
    fun dirPaperNightDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoDriveReview() } }
        capture("dir_paper_night_drive_review")
    }

    @Test
    fun dirPaperNightSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoSavedTracks() } }
        capture("dir_paper_night_saved_tracks")
    }

    @Test
    fun dirPaperNightTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoTrackEvidence() } }
        capture("dir_paper_night_track_evidence")
    }

    @Test
    fun dirPaperNightJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER, dark = true) { demoJourneyGuide() } }
        capture("dir_paper_night_journey_guide")
    }

    @Composable
    private fun directed(
        variant: MilewayThemeVariant,
        dark: Boolean = !variant.isLight,
        content: @Composable () -> Unit,
    ) {
        // Defaulting to the variant's *native* luminance, not to `false`. Once a direction gains a
        // counterpart spec it starts answering to `darkTheme` (MilewayThemeVariant.followsSystem),
        // so a hardcoded `false` default would silently re-record every dark direction's flagship
        // capture as its day face — which is what happened on the first record pass here.
        // For a still-single-mode variant this stays a no-op: it forces its own luminance anyway.
        MilewayTheme(darkTheme = dark, milewayTheme = variant) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            ) {
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

    // ── LEDGER ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun dirLedgerLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoLiveDrive() } }
        capture("dir_ledger_live_drive")
    }

    @Test
    fun dirLedgerDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoDriveReview() } }
        capture("dir_ledger_drive_review")
    }

    @Test
    fun dirLedgerSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoSavedTracks() } }
        capture("dir_ledger_saved_tracks")
    }

    @Test
    fun dirLedgerApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoApprovals() } }
        capture("dir_ledger_approvals")
    }

    @Test
    fun dirLedgerTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoTrackDetail() } }
        capture("dir_ledger_track_detail")
    }

    @Test
    fun dirLedgerExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoExpense() } }
        capture("dir_ledger_expense")
    }

    @Test
    fun dirLedgerJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoJourneyGuide() } }
        capture("dir_ledger_journey_guide")
    }

    @Test
    fun dirLedgerTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER) { demoTrackEvidence() } }
        capture("dir_ledger_track_evidence")
    }

    // ── SIGNAL ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun dirSignalLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoLiveDrive() } }
        capture("dir_signal_live_drive")
    }

    @Test
    fun dirSignalDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoDriveReview() } }
        capture("dir_signal_drive_review")
    }

    @Test
    fun dirSignalSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoSavedTracks() } }
        capture("dir_signal_saved_tracks")
    }

    @Test
    fun dirSignalApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoApprovals() } }
        capture("dir_signal_approvals")
    }

    @Test
    fun dirSignalTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoTrackDetail() } }
        capture("dir_signal_track_detail")
    }

    @Test
    fun dirSignalExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoExpense() } }
        capture("dir_signal_expense")
    }

    @Test
    fun dirSignalJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoJourneyGuide() } }
        capture("dir_signal_journey_guide")
    }

    @Test
    fun dirSignalTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL) { demoTrackEvidence() } }
        capture("dir_signal_track_evidence")
    }

    // ── PAPER ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun dirPaperLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoLiveDrive() } }
        capture("dir_paper_live_drive")
    }

    @Test
    fun dirPaperDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoDriveReview() } }
        capture("dir_paper_drive_review")
    }

    @Test
    fun dirPaperSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoSavedTracks() } }
        capture("dir_paper_saved_tracks")
    }

    @Test
    fun dirPaperApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoApprovals() } }
        capture("dir_paper_approvals")
    }

    @Test
    fun dirPaperTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoTrackDetail() } }
        capture("dir_paper_track_detail")
    }

    @Test
    fun dirPaperExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoExpense() } }
        capture("dir_paper_expense")
    }

    @Test
    fun dirPaperJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoJourneyGuide() } }
        capture("dir_paper_journey_guide")
    }

    @Test
    fun dirPaperTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.PAPER) { demoTrackEvidence() } }
        capture("dir_paper_track_evidence")
    }

    // ── INSTRUMENT ──────────────────────────────────────────────────────────────────────────

    @Test
    fun dirInstrumentLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoLiveDrive() } }
        capture("dir_instrument_live_drive")
    }

    @Test
    fun dirInstrumentDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoDriveReview() } }
        capture("dir_instrument_drive_review")
    }

    @Test
    fun dirInstrumentSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoSavedTracks() } }
        capture("dir_instrument_saved_tracks")
    }

    @Test
    fun dirInstrumentApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoApprovals() } }
        capture("dir_instrument_approvals")
    }

    @Test
    fun dirInstrumentTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoTrackDetail() } }
        capture("dir_instrument_track_detail")
    }

    @Test
    fun dirInstrumentExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoExpense() } }
        capture("dir_instrument_expense")
    }

    @Test
    fun dirInstrumentJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoJourneyGuide() } }
        capture("dir_instrument_journey_guide")
    }

    @Test
    fun dirInstrumentTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT) { demoTrackEvidence() } }
        capture("dir_instrument_track_evidence")
    }

    // ── REFINED_EMBER ───────────────────────────────────────────────────────────────────────

    @Test
    fun dirRefinedEmberLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoLiveDrive() } }
        capture("dir_refined_ember_live_drive")
    }

    @Test
    fun dirRefinedEmberDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoDriveReview() } }
        capture("dir_refined_ember_drive_review")
    }

    @Test
    fun dirRefinedEmberSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoSavedTracks() } }
        capture("dir_refined_ember_saved_tracks")
    }

    @Test
    fun dirRefinedEmberApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoApprovals() } }
        capture("dir_refined_ember_approvals")
    }

    @Test
    fun dirRefinedEmberTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoTrackDetail() } }
        capture("dir_refined_ember_track_detail")
    }

    @Test
    fun dirRefinedEmberExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoExpense() } }
        capture("dir_refined_ember_expense")
    }

    @Test
    fun dirRefinedEmberJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoJourneyGuide() } }
        capture("dir_refined_ember_journey_guide")
    }

    @Test
    fun dirRefinedEmberTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER) { demoTrackEvidence() } }
        capture("dir_refined_ember_track_evidence")
    }

    // ── LEDGER, NIGHT ─────────────────────────────────────────────────────────────────────
    // Ledger's hand-built night counterpart (LedgerSpecNight), now registered as LEDGER.darkSpec.
    // Captured as a pair with the face above so the site can wipe between them: same layout,
    // same content, only luminance differs.

    @Test
    fun dirLedgerNightLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoLiveDrive() } }
        capture("dir_ledger_night_live_drive")
    }

    @Test
    fun dirLedgerNightDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoDriveReview() } }
        capture("dir_ledger_night_drive_review")
    }

    @Test
    fun dirLedgerNightSavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoSavedTracks() } }
        capture("dir_ledger_night_saved_tracks")
    }

    @Test
    fun dirLedgerNightApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoApprovals() } }
        capture("dir_ledger_night_approvals")
    }

    @Test
    fun dirLedgerNightTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoTrackDetail() } }
        capture("dir_ledger_night_track_detail")
    }

    @Test
    fun dirLedgerNightExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoExpense() } }
        capture("dir_ledger_night_expense")
    }

    @Test
    fun dirLedgerNightJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoJourneyGuide() } }
        capture("dir_ledger_night_journey_guide")
    }

    @Test
    fun dirLedgerNightTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.LEDGER, dark = true) { demoTrackEvidence() } }
        capture("dir_ledger_night_track_evidence")
    }

    // ── SIGNAL, DAY ───────────────────────────────────────────────────────────────────────
    // Signal's hand-built day counterpart (SignalSpecDay), now registered as SIGNAL.lightSpec.
    // Captured as a pair with the face above so the site can wipe between them: same layout,
    // same content, only luminance differs.

    @Test
    fun dirSignalDayLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoLiveDrive() } }
        capture("dir_signal_day_live_drive")
    }

    @Test
    fun dirSignalDayDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoDriveReview() } }
        capture("dir_signal_day_drive_review")
    }

    @Test
    fun dirSignalDaySavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoSavedTracks() } }
        capture("dir_signal_day_saved_tracks")
    }

    @Test
    fun dirSignalDayApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoApprovals() } }
        capture("dir_signal_day_approvals")
    }

    @Test
    fun dirSignalDayTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoTrackDetail() } }
        capture("dir_signal_day_track_detail")
    }

    @Test
    fun dirSignalDayExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoExpense() } }
        capture("dir_signal_day_expense")
    }

    @Test
    fun dirSignalDayJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoJourneyGuide() } }
        capture("dir_signal_day_journey_guide")
    }

    @Test
    fun dirSignalDayTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.SIGNAL, dark = false) { demoTrackEvidence() } }
        capture("dir_signal_day_track_evidence")
    }

    // ── INSTRUMENT, DAY ───────────────────────────────────────────────────────────────────
    // Instrument's hand-built day counterpart (InstrumentSpecDay), now registered as INSTRUMENT.lightSpec.
    // Captured as a pair with the face above so the site can wipe between them: same layout,
    // same content, only luminance differs.

    @Test
    fun dirInstrumentDayLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoLiveDrive() } }
        capture("dir_instrument_day_live_drive")
    }

    @Test
    fun dirInstrumentDayDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoDriveReview() } }
        capture("dir_instrument_day_drive_review")
    }

    @Test
    fun dirInstrumentDaySavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoSavedTracks() } }
        capture("dir_instrument_day_saved_tracks")
    }

    @Test
    fun dirInstrumentDayApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoApprovals() } }
        capture("dir_instrument_day_approvals")
    }

    @Test
    fun dirInstrumentDayTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoTrackDetail() } }
        capture("dir_instrument_day_track_detail")
    }

    @Test
    fun dirInstrumentDayExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoExpense() } }
        capture("dir_instrument_day_expense")
    }

    @Test
    fun dirInstrumentDayJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoJourneyGuide() } }
        capture("dir_instrument_day_journey_guide")
    }

    @Test
    fun dirInstrumentDayTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.INSTRUMENT, dark = false) { demoTrackEvidence() } }
        capture("dir_instrument_day_track_evidence")
    }

    // ── REFINED_EMBER, DAY ────────────────────────────────────────────────────────────────
    // Refined Ember's hand-built day counterpart (RefinedEmberSpecDay), now registered as REFINED_EMBER.lightSpec.
    // Captured as a pair with the face above so the site can wipe between them: same layout,
    // same content, only luminance differs.

    @Test
    fun dirRefinedEmberDayLiveDrive() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoLiveDrive() } }
        capture("dir_refined_ember_day_live_drive")
    }

    @Test
    fun dirRefinedEmberDayDriveReview() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoDriveReview() } }
        capture("dir_refined_ember_day_drive_review")
    }

    @Test
    fun dirRefinedEmberDaySavedTracks() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoSavedTracks() } }
        capture("dir_refined_ember_day_saved_tracks")
    }

    @Test
    fun dirRefinedEmberDayApprovals() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoApprovals() } }
        capture("dir_refined_ember_day_approvals")
    }

    @Test
    fun dirRefinedEmberDayTrackDetail() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoTrackDetail() } }
        capture("dir_refined_ember_day_track_detail")
    }

    @Test
    fun dirRefinedEmberDayExpense() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoExpense() } }
        capture("dir_refined_ember_day_expense")
    }

    @Test
    fun dirRefinedEmberDayJourneyGuide() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoJourneyGuide() } }
        capture("dir_refined_ember_day_journey_guide")
    }

    @Test
    fun dirRefinedEmberDayTrackEvidence() {
        composeRule.setContent { directed(MilewayThemeVariant.REFINED_EMBER, dark = false) { demoTrackEvidence() } }
        capture("dir_refined_ember_day_track_evidence")
    }
}
