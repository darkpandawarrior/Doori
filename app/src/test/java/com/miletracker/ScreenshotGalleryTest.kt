package com.miletracker

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.miletracker.core.data.dao.HardwareEventDao
import com.miletracker.core.data.dao.LocationDao
import com.miletracker.core.data.dao.LogMilesDraftDao
import com.miletracker.core.data.dao.LogMilesFrequentRouteDao
import com.miletracker.core.data.dao.SavedTrackDao
import com.miletracker.core.data.dao.TripAttachmentDao
import com.miletracker.core.data.library.MediaLibraryDao
import com.miletracker.core.data.library.MediaLibraryEntry
import com.miletracker.core.data.model.db.SavedTrack
import com.miletracker.core.data.session.CurrentTrackDataStore
import com.miletracker.core.data.settings.DemoSettingsRepository
import com.miletracker.core.maps.MapSurface
import com.miletracker.core.platform.AnalyticsHelper
import com.miletracker.core.platform.AppReviewManagerFactory
import com.miletracker.core.platform.AppUpdateManagerFactory
import com.miletracker.core.platform.CrashReporter
import com.miletracker.core.platform.LoggingAnalyticsHelper
import com.miletracker.core.platform.NotificationScheduler
import com.miletracker.core.platform.ReferralData
import com.miletracker.core.platform.ReferralManager
import com.miletracker.core.ui.di.coreUiModule
import com.miletracker.core.ui.theme.MileTrackerTheme
import com.miletracker.feature.agent.di.agentModule
import com.miletracker.feature.agent.ui.screens.AgentChatScreen
import com.miletracker.feature.agent.ui.screens.AgentHistoryScreen
import com.miletracker.feature.approvals.di.approvalsModule
import com.miletracker.feature.approvals.ui.screens.ApprovalDetailsScreen
import com.miletracker.feature.approvals.ui.screens.ApprovalsScreen
import com.miletracker.feature.cards.di.cardsModule
import com.miletracker.feature.cards.ui.CardDetailScreen
import com.miletracker.feature.cards.ui.CardRequestScreen
import com.miletracker.feature.cards.ui.CardsHomeScreen
import com.miletracker.feature.events.di.eventsModule
import com.miletracker.feature.events.ui.screens.CreateEventScreen
import com.miletracker.feature.events.ui.screens.EventsHistoryScreen
import com.miletracker.feature.logging.di.loggingModule
import com.miletracker.feature.logging.ui.screens.ExpenseDetailScreen
import com.miletracker.feature.logging.ui.screens.ExpenseDetailsInputScreen
import com.miletracker.feature.logging.ui.screens.ExpenseEntryScreen
import com.miletracker.feature.logging.ui.screens.ExpenseHistoryScreen
import com.miletracker.feature.logging.ui.screens.LogMilesHistoryScreen
import com.miletracker.feature.logging.ui.screens.LogMilesScreen
import com.miletracker.feature.logging.ui.screens.LogMilesStep2Screen
import com.miletracker.feature.logging.ui.screens.SpendsHomeScreen
import com.miletracker.feature.logging.ui.screens.VoucherHistoryScreen
import com.miletracker.feature.media.di.mediaModule
import com.miletracker.feature.media.model.FlashMode
import com.miletracker.feature.media.ui.camera.CameraCaptureScreen
import com.miletracker.feature.media.ui.screens.AttachmentPreviewScreen
import com.miletracker.feature.media.ui.screens.AttachmentSelectionScreen
import com.miletracker.feature.media.ui.screens.CloudLibraryScreen
import com.miletracker.feature.media.viewmodel.MediaViewModel
import com.miletracker.feature.payables.di.payablesModule
import com.miletracker.feature.payables.ui.screens.CreateInvoiceScreen
import com.miletracker.feature.payables.ui.screens.CreatePurchaseRequestScreen
import com.miletracker.feature.payables.ui.screens.PayablesHistoryScreen
import com.miletracker.feature.payables.ui.screens.PayablesHomeScreen
import com.miletracker.feature.payables.ui.screens.PurchaseRequestDetailsScreen
import com.miletracker.feature.payments.di.paymentsModule
import com.miletracker.feature.payments.ui.screens.CreatePaymentScreen
import com.miletracker.feature.payments.ui.screens.PaymentsHistoryScreen
import com.miletracker.feature.profile.di.profileModule
import com.miletracker.feature.profile.ui.screens.AdvanceHistoryScreen
import com.miletracker.feature.profile.ui.screens.AnalyticsDetailScreen
import com.miletracker.feature.profile.ui.screens.AnalyticsHomeScreen
import com.miletracker.feature.profile.ui.screens.AskAdvanceFormScreen
import com.miletracker.feature.profile.ui.screens.DelegationScreen
import com.miletracker.feature.profile.ui.screens.DemoSettingsScreen
import com.miletracker.feature.profile.ui.screens.HelpScreen
import com.miletracker.feature.profile.ui.screens.NotificationCentreScreen
import com.miletracker.feature.profile.ui.screens.PreferencesScreen
import com.miletracker.feature.profile.ui.screens.ProfileDetailsScreen
import com.miletracker.feature.profile.ui.screens.ProfileScreen
import com.miletracker.feature.profile.ui.screens.QrHomeScreen
import com.miletracker.feature.profile.ui.screens.RootGuardScreen
import com.miletracker.feature.profile.ui.screens.SettingsScreen
import com.miletracker.feature.tracking.debug.DebugMenuScreen
import com.miletracker.feature.tracking.di.trackingModule
import com.miletracker.feature.tracking.ui.screens.CheckInHistoryItem
import com.miletracker.feature.tracking.ui.screens.CheckInHistoryScreen
import com.miletracker.feature.tracking.ui.screens.CreateVoucherScreen
import com.miletracker.feature.tracking.ui.screens.GeoCheckInScreen
import com.miletracker.feature.tracking.ui.screens.HardwareEventsLogScreen
import com.miletracker.feature.tracking.ui.screens.LocationMapScreen
import com.miletracker.feature.tracking.ui.screens.ManualCheckInScreen
import com.miletracker.feature.tracking.ui.screens.SavedTracksScreen
import com.miletracker.feature.tracking.ui.screens.SetupGuideScreen
import com.miletracker.feature.tracking.ui.screens.TrackCustomizationScreen
import com.miletracker.feature.tracking.ui.screens.TrackDataPreviewScreen
import com.miletracker.feature.tracking.ui.screens.TrackDetailScreen
import com.miletracker.feature.tracking.ui.screens.TrackInsightsScreen
import com.miletracker.feature.tracking.ui.screens.TrackLoadingScreen
import com.miletracker.feature.tracking.ui.screens.TrackMilesScreen
import com.miletracker.feature.tracking.ui.screens.TrackSettingsScreen
import com.miletracker.feature.tracking.ui.screens.TrackSubmissionScreen
import com.miletracker.feature.tracking.ui.screens.TrackingSuccessScreen
import com.miletracker.feature.travel.di.travelModule
import com.miletracker.feature.travel.ui.screens.BookingHistoryScreen
import com.miletracker.feature.travel.ui.screens.CreateMjpScreen
import com.miletracker.feature.travel.ui.screens.CreateTripScreen
import com.miletracker.feature.travel.ui.screens.TravelHomeScreen
import com.miletracker.feature.travel.ui.screens.TripHistoryScreen
import com.miletracker.stub.di.stubModule
import com.miletracker.ui.AssistantHomeSheet
import com.miletracker.ui.ShellPlaceholderScreen
import com.miletracker.ui.auth.LoginScreen
import com.miletracker.ui.auth.SplashScreen
import com.miletracker.ui.home.HomeScreenContent
import com.miletracker.ui.home.HomeUiState
import com.miletracker.ui.home.homeModule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// ---------------------------------------------------------------------------
// Full Roborazzi screen gallery for the docs/ screenshot catalogue.
//
// A single Koin graph carries every feature module (tracking / logging / travel
// / payables / payments / events / cards / agent / profile / media / approvals
// / home + appModule) plus a `fakeRoomLayer` that supplies the DAOs, the concrete
// data-layer singletons (CurrentTrackDataStore, DemoSettingsRepository) and the
// MapSurface those modules need. coreDataModule, mapsKoinModule() and
// platformServicesKoinModule() are intentionally NOT included — they would build
// Room / GMS / MapLibre against a mock Context and crash on the JVM. The fakes
// stand in for exactly the boundary they own.
//
// Record / update:
//   ./gradlew :app:testNoGmsDebugUnitTest \
//     --tests "com.miletracker.ScreenshotGalleryTest" -Proborazzi.test.record=true
//
// Output: docs/screenshots/<name>.png
// ---------------------------------------------------------------------------

// Use plain Application to skip MileTrackerApplication.onCreate → startKoin.
// qualifiers pins a realistic phone viewport (411×891 dp, mdpi) so chip/button rows
// (booking & expense status filters, check-in actions) lay out the way they do on a
// real device instead of overflowing in the narrow 320 dp Robolectric default.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotGalleryTest {

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        // Pre-seeded SavedTrackDao shared by SavedTracks / TrackDetail / TrackInsights /
        // MileageSubmission / CreateVoucher VMs. FakeSavedTrackDao is a public top-level
        // test class (TrackMilesViewModelTest.kt); reuse it rather than redefine.
        private val seededDao = FakeSavedTrackDao().also { dao ->
            val baseMs = 1_700_000_000_000L
            dao.preload(completedTrack("route-j1", "Pune → Hinjewadi", 12_400.0, baseMs - 86_400_000L))
            dao.preload(completedTrack("route-j2", "FC Road → Koregaon Park", 3_800.0, baseMs - 172_800_000L))
            dao.preload(completedTrack("route-j3", "Camp → Hadapsar", 7_100.0, baseMs - 259_200_000L))
            dao.preload(submittedTrack("route-s1", "Kothrud → Baner", 9_200.0, baseMs - 432_000_000L))
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

        // Seeded MediaLibraryDao so CloudLibraryScreen renders a populated grid rather
        // than the empty state. observeAll() emits a fixed list of demo entries.
        private val mediaLibraryDao = mockk<MediaLibraryDao>(relaxed = true).also { dao ->
            val baseMs = 1_700_000_000_000L
            val entries = listOf(
                MediaLibraryEntry("m1", "file:///demo/odometer.jpg", "image/jpeg", "Odometer — Pune", "CAMERA", baseMs - 3_600_000L),
                MediaLibraryEntry("m2", "file:///demo/fuel.jpg", "image/jpeg", "Fuel receipt — Hinjewadi", "GALLERY", baseMs - 7_200_000L),
                MediaLibraryEntry("m3", "file:///demo/toll.jpg", "image/jpeg", "Toll receipt — Mumbai Expressway", "CAMERA", baseMs - 10_800_000L),
                MediaLibraryEntry("m4", "file:///demo/parking.jpg", "image/jpeg", "Parking — Magarpatta", "GALLERY", baseMs - 14_400_000L),
                MediaLibraryEntry("m5", "file:///demo/invoice.jpg", "image/jpeg", "Cab invoice — Koregaon Park", "CAMERA", baseMs - 18_000_000L),
                MediaLibraryEntry("m6", "file:///demo/meal.jpg", "image/jpeg", "Meal receipt — FC Road", "GALLERY", baseMs - 21_600_000L),
            )
            every { dao.observeAll() } returns MutableStateFlow(entries)
        }

        private val fakeRoomLayer = module {
            single<SavedTrackDao> { seededDao }
            single<LocationDao> { mockk(relaxed = true) }
            single<HardwareEventDao> { mockk(relaxed = true) }
            single<LogMilesDraftDao> { mockk(relaxed = true) }
            single<LogMilesFrequentRouteDao> { mockk(relaxed = true) }
            single<TripAttachmentDao> { mockk(relaxed = true) }
            single<MediaLibraryDao> { mediaLibraryDao }
            single<CurrentTrackDataStore> { mockk(relaxed = true) }
            single<DemoSettingsRepository> { mockk(relaxed = true) }
            // Map screens (GeoCheckIn, LocationMap, LiveTrack, LogMiles thumbnail) inject
            // MapSurface; the real flavor surfaces need GMS / MapLibre native, so use a
            // no-op fake on the JVM. mapsKoinModule() is deliberately excluded.
            single<MapSurface> { FakeMapSurface() }
        }

        // Stand-ins for the platform-service graph (platformModule +
        // platformServicesKoinModule) that is deliberately excluded because its real
        // Android impls touch a live Context/Activity and crash on Robolectric:
        //  - NotificationScheduler: AndroidNotificationScheduler's ctor does
        //    getSystemService(...) as NotificationManager; the mock Context returns a
        //    bare Object → ClassCastException (pulled by MileageSubmissionViewModel).
        //  - ReferralManager: ProfileScreen's ReferralCardHost koinInjects it once the
        //    taller viewport scrolls it into composition; a tiny demo impl renders a
        //    populated referral card instead of a blank one.
        //  - the rest are safety-net binds so any update/review/analytics surface that
        //    scrolls into view resolves instead of throwing NoDefinitionFound.
        // Listed LAST in modules(...) so Koin's last-definition-wins override picks them.
        private val fakeOverrides = module {
            single<NotificationScheduler> { mockk(relaxed = true) }
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
        }

        @BeforeClass @JvmStatic
        fun setup() {
            // These are record-only documentation screenshots written to a custom docs/
            // path (the README gallery), not Roborazzi's tracked output dir — so force
            // record here, matching the project convention. The strict verifyRoborazzi
            // gate covers the deterministic component previews in ScreenshotCatalogTest.
            System.setProperty("roborazzi.test.record", "true")
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

    // ── Tracking ───────────────────────────────────────────────────────────────

    @Test
    fun trackMilesIdleScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackMilesScreen(
                    onStop = { _, _, _, _, _ -> },
                    onOpenMap = {},
                    onOpenHwEvents = {},
                    onOpenCheckInHistory = {},
                    onOpenSettings = {},
                    onNavigateToGeoCheckIn = {},
                    onNavigateToManualCheckIn = {},
                )
            }
        }
        capture("track_miles_idle_screen")
    }

    @Test
    fun savedTracksJourneysTab() {
        composeRule.setContent {
            MileTrackerTheme {
                SavedTracksScreen(onTrackClick = {}, onStartNew = {})
            }
        }
        capture("saved_tracks_journeys_tab")
    }

    @Test
    fun trackDetailScreen() {
        composeRule.setContent {
            MileTrackerTheme {
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
        capture("track_detail_screen")
    }

    @Test
    fun trackInsightsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackInsightsScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("track_insights_screen")
    }

    @Test
    fun hardwareEventsLogScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                HardwareEventsLogScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("hardware_events_log_screen")
    }

    @Test
    fun trackDataPreviewOverviewTab() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackDataPreviewScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("track_data_preview_overview_tab")
    }

    // LiveTrackScreen is intentionally omitted: it needs an active in-progress track
    // session (CurrentTrackDataStore must emit a track with locations); with the offline
    // fakes it renders a "Failed to load" state. track_miles_idle + tracking_success
    // already document the live-tracking flow.

    @Test
    fun locationMapScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                LocationMapScreen(onNavigateBack = {})
            }
        }
        capture("location_map_screen")
    }

    @Test
    fun geoCheckInScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                GeoCheckInScreen(onBack = {})
            }
        }
        capture("geo_check_in_screen")
    }

    @Test
    fun manualCheckInScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ManualCheckInScreen(onBack = {})
            }
        }
        capture("manual_check_in_screen")
    }

    @Test
    fun checkInHistoryScreen() {
        val baseMs = 1_700_000_000_000L
        val events = listOf(
            CheckInHistoryItem("c1", "Hinjewadi IT Park", "Geo check-in confirmed", baseMs - 3_600_000L, 18.5904, 73.7394, "GEO", false),
            CheckInHistoryItem("c2", "FC Road Cafe", "Manual check-in", baseMs - 10_800_000L, 18.5285, 73.8434, "MANUAL", true),
            CheckInHistoryItem("c3", "Magarpatta Office", "Geo check-in confirmed", baseMs - 25_200_000L, 18.5152, 73.9262, "GEO", false),
        )
        composeRule.setContent {
            MileTrackerTheme {
                CheckInHistoryScreen(events = events, onBack = {})
            }
        }
        capture("check_in_history_screen")
    }

    @Test
    fun trackSubmissionScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackSubmissionScreen(
                    routeId = "route-j1",
                    distanceKm = 12.4,
                    vehicleKey = "fourWheelerPetrol",
                    startTime = 1_700_000_000_000L,
                    endTime = 1_700_003_600_000L,
                    onSuccess = {},
                    onBack = {},
                )
            }
        }
        capture("track_submission_screen")
    }

    @Test
    fun trackingSuccessScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackingSuccessScreen(
                    distanceKm = 12.4,
                    reimbursableAmount = 185.60,
                    vehicleName = "Honda City",
                    startTime = 1_700_000_000_000L,
                    endTime = 1_700_003_600_000L,
                    transactionId = "TXN-20241115-0042",
                    submissionStatus = "APPROVED",
                    violationCount = 0,
                    violationMessage = null,
                    voucherNumber = "V-2024-0112",
                    voucherAmount = 185.60,
                    onTrackNewJourney = {},
                    onViewExpense = {},
                    onCreateVoucher = {},
                )
            }
        }
        capture("tracking_success_screen")
    }

    @Test
    fun setupGuideScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                SetupGuideScreen(onBack = {}, onOpenTrackSettings = {})
            }
        }
        capture("tracking_setup_guide_screen")
    }

    @Test
    fun trackSettingsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackSettingsScreen(onBack = {})
            }
        }
        capture("track_settings_screen")
    }

    @Test
    fun trackCustomizationScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackCustomizationScreen(onBack = {})
            }
        }
        capture("track_customization_screen")
    }

    @Test
    fun trackLoadingScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TrackLoadingScreen(message = "Submitting your journey...")
            }
        }
        capture("tracking_loading_screen")
    }

    // ── Logging & Expenses ───────────────────────────────────────────────────────

    @Test
    fun createVoucherSelectExpenses() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateVoucherScreen(onBack = {})
            }
        }
        capture("create_voucher_select_expenses")
    }

    @Test
    fun logMilesStep1Screen() {
        composeRule.setContent {
            MileTrackerTheme {
                LogMilesScreen(onNext = {}, onOpenHistory = {})
            }
        }
        capture("log_miles_step1_screen")
    }

    @Test
    fun logMilesStep2Screen() {
        composeRule.setContent {
            MileTrackerTheme {
                LogMilesStep2Screen(onBack = {}, onSubmitted = {})
            }
        }
        capture("log_miles_step2_screen")
    }

    // LogMilesSuccessScreen / ExpenseSuccessScreen / PurchaseRequestSuccessScreen are
    // intentionally omitted: each is a confirmation screen whose entire content is the
    // reference id + amount of a just-completed submission, read from VM state. Rendered
    // cold (no submission) they show a blank "Expense ID:" / "PO Number:" and ₹0.00,
    // which misrepresents the app. The success-confirmation design is documented by
    // tracking_success_screen, which is rendered with full realistic data.

    @Test
    fun logMilesHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                LogMilesHistoryScreen(onBack = {}, onOpenDraft = {})
            }
        }
        capture("log_miles_history_screen")
    }

    @Test
    fun spendsHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                SpendsHomeScreen(
                    onTrackMileage = {},
                    onAddExpense = {},
                    onMileageHistory = {},
                    onExpenseHistory = {},
                )
            }
        }
        capture("spends_home_screen")
    }

    @Test
    fun expenseEntryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ExpenseEntryScreen(onBack = {}, onCategorySelected = {})
            }
        }
        capture("expense_entry_screen")
    }

    @Test
    fun expenseDetailsInputScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ExpenseDetailsInputScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("expense_details_input_screen")
    }

    @Test
    fun expenseHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ExpenseHistoryScreen(onBack = {}, onOpenDetail = {})
            }
        }
        capture("expense_history_screen")
    }

    @Test
    fun expenseDetailScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ExpenseDetailScreen(expenseId = "EXP-002", onBack = {})
            }
        }
        capture("expense_detail_screen")
    }

    @Test
    fun voucherHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                VoucherHistoryScreen(onBack = {})
            }
        }
        capture("voucher_history_screen")
    }

    // ── Travel ───────────────────────────────────────────────────────────────────

    @Test
    fun travelHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TravelHomeScreen()
            }
        }
        capture("travel_home_screen")
    }

    @Test
    fun createTripScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateTripScreen(onBack = {}, onSubmitted = { _ -> })
            }
        }
        capture("create_trip_screen")
    }

    @Test
    fun createMjpScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateMjpScreen(onBack = {}, onSubmitted = { _ -> })
            }
        }
        capture("create_mjp_screen")
    }

    @Test
    fun tripHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                TripHistoryScreen(onBack = {})
            }
        }
        capture("trip_history_screen")
    }

    @Test
    fun bookingHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                BookingHistoryScreen(onBack = {})
            }
        }
        capture("booking_history_screen")
    }

    // ── Approvals & Payables ───────────────────────────────────────────────────────

    @Test
    fun advanceHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                AdvanceHistoryScreen(onBack = {}, onRequestAdvance = {})
            }
        }
        capture("advance_history_screen")
    }

    @Test
    fun askAdvanceFormStep1Screen() {
        composeRule.setContent {
            MileTrackerTheme {
                AskAdvanceFormScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("ask_advance_form_step1_screen")
    }

    @Test
    fun payablesHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PayablesHomeScreen(
                    onNewRequest = {},
                    onOpenPo = { _ -> },
                )
            }
        }
        capture("payables_home_screen")
    }

    @Test
    fun createPurchaseRequestScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreatePurchaseRequestScreen(
                    onBack = {},
                    onSubmitted = {},
                )
            }
        }
        capture("create_purchase_request_screen")
    }

    @Test
    fun purchaseRequestDetailsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PurchaseRequestDetailsScreen(
                    poId = "PO-2024-001",
                    onBack = {},
                )
            }
        }
        capture("purchase_request_details_screen")
    }

    @Test
    fun createInvoiceScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateInvoiceScreen(
                    onBack = {},
                    onSubmitted = { _ -> },
                )
            }
        }
        capture("create_invoice_screen")
    }

    @Test
    fun payablesHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PayablesHistoryScreen(
                    onBack = {},
                )
            }
        }
        capture("payables_history_screen")
    }

    @Test
    fun approvalsScreenPendingTab() {
        composeRule.setContent {
            MileTrackerTheme {
                ApprovalsScreen(onOpenDetail = {})
            }
        }
        capture("approvals_screen_pending_tab")
    }

    @Test
    fun approvalDetailsScreenViolation() {
        composeRule.setContent {
            MileTrackerTheme {
                ApprovalDetailsScreen(approvalId = "A003", onBack = {})
            }
        }
        capture("approval_details_screen_violation")
    }

    // ── Payments & Events ──────────────────────────────────────────────────────────

    @Test
    fun createPaymentScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreatePaymentScreen(
                    onBack = {},
                    onSubmitted = { _ -> },
                )
            }
        }
        capture("create_payment_screen")
    }

    @Test
    fun paymentsHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PaymentsHistoryScreen(
                    onBack = {},
                )
            }
        }
        capture("payments_history_screen")
    }

    @Test
    fun createEventScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateEventScreen(
                    onBack = {},
                    onSubmitted = { _ -> },
                )
            }
        }
        capture("create_event_screen")
    }

    @Test
    fun eventsHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                EventsHistoryScreen(
                    onBack = {},
                )
            }
        }
        capture("events_history_screen")
    }

    @Test
    fun qrHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                QrHomeScreen(onBack = {})
            }
        }
        capture("qr_home_screen")
    }

    @Test
    fun cardsHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CardsHomeScreen(
                    onOpenCard = {},
                    onRequestCard = {},
                )
            }
        }
        capture("cards_home_screen")
    }

    @Test
    fun cardDetailScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CardDetailScreen(
                    cardId = 1L,
                    onBack = {},
                )
            }
        }
        capture("card_detail_screen")
    }

    @Test
    fun cardRequestScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CardRequestScreen(
                    onDone = {},
                )
            }
        }
        capture("card_request_screen")
    }

    // ── Profile & Account ──────────────────────────────────────────────────────────

    @Test
    fun profileAccountHub() {
        composeRule.setContent {
            MileTrackerTheme {
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
        capture("profile_account_hub")
    }

    @Test
    fun profileDetailsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ProfileDetailsScreen(onBack = {})
            }
        }
        capture("profile_details_screen")
    }

    @Test
    fun preferencesScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PreferencesScreen(onBack = {})
            }
        }
        capture("preferences_screen")
    }

    @Test
    fun settingsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                SettingsScreen(onBack = {}, onOpenDebugMenu = {})
            }
        }
        capture("settings_screen")
    }

    @Test
    fun analyticsHomeScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                AnalyticsHomeScreen(onBack = {}, onOpenDetail = { _ -> })
            }
        }
        capture("analytics_home_screen")
    }

    @Test
    fun analyticsDetailMileageScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                AnalyticsDetailScreen(category = "Mileage", onBack = {})
            }
        }
        capture("analytics_detail_mileage_screen")
    }

    @Test
    fun delegationScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                DelegationScreen(onBack = {})
            }
        }
        capture("delegation_screen")
    }

    @Test
    fun helpSupportScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                HelpScreen(onBack = {})
            }
        }
        capture("help_support_screen")
    }

    @Test
    fun notificationCentreScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                NotificationCentreScreen(onBack = {})
            }
        }
        capture("notification_centre_screen")
    }

    // ── Media ──────────────────────────────────────────────────────────────────────

    @Test
    fun mediaAttachmentSelectionScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                val mediaVm = koinViewModel<MediaViewModel>()
                AttachmentSelectionScreen(
                    viewModel = mediaVm,
                    onNavigateToCamera = { _ -> },
                    onNavigateToPreview = {},
                    onNavigateBack = {},
                    onNavigateToLibrary = {},
                )
            }
        }
        capture("media_attachment_selection_screen")
    }

    @Test
    fun mediaAttachmentPreviewScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                val mediaVm = koinViewModel<MediaViewModel>()
                AttachmentPreviewScreen(
                    viewModel = mediaVm,
                    onRetake = {},
                    onUsePhoto = {},
                    onAddMore = {},
                )
            }
        }
        capture("media_attachment_preview_screen")
    }

    @Test
    fun mediaCloudLibraryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CloudLibraryScreen(
                    onNavigateBack = {},
                    onSelectUri = { _ -> },
                )
            }
        }
        capture("media_cloud_library_screen")
    }

    @Test
    fun mediaCameraPermissionRequired() {
        composeRule.setContent {
            MileTrackerTheme {
                // Mock Context → CAMERA permission not-granted → renders the
                // permission-required fallback (headless-safe).
                CameraCaptureScreen(
                    onCaptured = { _ -> },
                    isOdometerMode = false,
                    flashMode = FlashMode.AUTO,
                    onCycleFlash = {},
                )
            }
        }
        capture("media_camera_permission_required")
    }

    // ── Assistant ──────────────────────────────────────────────────────────────────

    @Test
    fun agentChatScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                AgentChatScreen(
                    onBack = {},
                    onOpenHistory = {},
                )
            }
        }
        capture("agent_chat_screen")
    }

    @Test
    fun agentHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                AgentHistoryScreen(
                    onBack = {},
                    onConversationSelected = { _ -> },
                )
            }
        }
        capture("agent_history_screen")
    }

    @Test
    fun assistantHomeSheet() {
        composeRule.setContent {
            MileTrackerTheme {
                AssistantHomeSheet(onDismiss = {})
            }
        }
        capture("assistant_home_sheet")
    }

    // ── Security ───────────────────────────────────────────────────────────────────

    @Test
    fun debugMenuScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                DebugMenuScreen(onBack = {})
            }
        }
        capture("debug_menu_screen")
    }

    @Test
    fun demoSettingsScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                DemoSettingsScreen(onBack = {}, onOpenRootGuard = {}, onOpenRootGuardDetected = {})
            }
        }
        capture("demo_settings_screen")
    }

    // ── Home ───────────────────────────────────────────────────────────────────────

    @Test
    fun homeScreenLoaded() {
        composeRule.setContent {
            MileTrackerTheme {
                HomeScreenContent(
                    state = HomeUiState(greetingName = "Siddharth", notificationCount = 3),
                    onStartTracking = {},
                    onAddExpense = {},
                    onOpenAccount = {},
                )
            }
        }
        capture("home_screen_loaded")
    }

    @Test
    fun loginScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                LoginScreen(onSignedIn = {})
            }
        }
        capture("login_screen")
    }

    @Test
    fun splashScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                SplashScreen(onFinished = {})
            }
        }
        capture("splash_screen")
    }

    @Test
    fun shellPlaceholderScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                ShellPlaceholderScreen(
                    title = "Travel",
                    icon = Icons.Filled.TravelExplore,
                )
            }
        }
        capture("shell_placeholder_screen")
    }

    // ── Security ──────────────────────────────────────────────────────────────

    @Test
    fun rootGuardScreen_signalsDetected() {
        composeRule.setContent {
            MileTrackerTheme {
                RootGuardScreen(
                    onContinue = {},
                    signals = listOf("su binary found at /system/xbin/su", "test-keys build"),
                )
            }
        }
        capture("root_guard_screen")
    }

    @Test
    fun rootGuardScreen_clean() {
        composeRule.setContent {
            MileTrackerTheme {
                RootGuardScreen(onContinue = {}, signals = emptyList())
            }
        }
        capture("root_guard_screen_clean")
    }

    private fun capture(name: String) =
        composeRule.onRoot().captureRoboImage(File(screenshotsDir, "$name.png").absolutePath)
}
