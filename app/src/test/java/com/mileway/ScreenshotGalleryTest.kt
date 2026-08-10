package com.mileway

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mileway.core.platform.SystemSettingsOpener
import com.mileway.core.data.model.db.VoucherCategory
import com.mileway.core.data.model.db.VoucherEntity
import com.mileway.core.data.model.display.TrackingSystemFlags
import com.mileway.core.data.model.network.PolicyViolation
import com.mileway.feature.tracking.viewmodel.TrackMilesPhase
import com.mileway.feature.tracking.ui.live.LiveDriveActions
import com.mileway.feature.tracking.ui.live.LiveDriveScreen
import com.mileway.feature.tracking.ui.live.LiveDriveState
import com.mileway.feature.tracking.ui.evidence.TrackEvidenceScreen
import com.mileway.feature.tracking.viewmodel.TrackSignal
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
import com.mileway.core.network.model.BusinessEntity
import com.mileway.core.network.model.Office
import com.mileway.core.platform.ReferralData
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.UrlOpener
import com.mileway.core.platform.defaultPermissionTiers
import com.mileway.core.ui.components.CriticalErrorDialog
import com.mileway.core.ui.components.LanguageSelectionSheet
import com.mileway.core.ui.components.dialog.ColorWheelDialog
import com.mileway.core.ui.components.sheet.ActionConfirmationBottomSheet
import com.mileway.core.ui.components.sheet.ActionConfirmationToneType
import com.mileway.core.ui.components.sheet.DetailInfoBottomSheet
import com.mileway.core.ui.components.sheet.DetailInfoCard
import com.mileway.core.ui.components.sheet.DetailInfoRow
import com.mileway.core.ui.components.sheet.FilterBottomSheet
import com.mileway.core.ui.components.sheet.FilterOption
import com.mileway.core.ui.components.sheet.FilterSection
import com.mileway.core.ui.components.sheet.FilterSelectionMode
import com.mileway.core.ui.components.sheet.OdometerDiscrepancySheet
import com.mileway.core.ui.components.sheet.OdometerRejectionSheet
import com.mileway.core.ui.components.pickers.WheelDatePickerDialog
import com.mileway.core.ui.components.pickers.WheelTimePickerDialog
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.platform.LocalNowMs
import com.mileway.core.ui.support.BugReportSheet
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.di.agentModule
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.ui.components.AssistantFab
import com.mileway.feature.agent.ui.components.ChatAgentIndicator
import com.mileway.feature.agent.ui.components.ChatIndicatorMode
import com.mileway.feature.agent.ui.components.VoiceWaveformOverlay
import com.mileway.feature.agent.ui.components.WaveformState
import com.mileway.feature.agent.ui.screens.AgentChatScreen
import com.mileway.feature.agent.ui.screens.AgentHistoryScreen
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.voice.TextToSpeech
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.approvals.model.ApprovalItem
import com.mileway.feature.approvals.model.ApprovalStatus
import com.mileway.feature.approvals.model.ApprovalType
import com.mileway.feature.approvals.ui.screens.ApprovalDetailsScreen
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
import com.mileway.feature.approvals.ui.sheets.ClaimantHistorySheet
import com.mileway.feature.cards.di.cardsModule
import com.mileway.feature.cards.ui.CardDetailScreen
import com.mileway.feature.cards.ui.CardRequestScreen
import com.mileway.feature.cards.ui.CardsHomeScreen
import com.mileway.feature.events.di.eventsModule
import com.mileway.feature.events.ui.screens.CreateEventScreen
import com.mileway.feature.events.ui.screens.EventsHistoryScreen
import com.mileway.feature.logging.di.loggingModule
import com.mileway.feature.logging.ui.screens.ExpenseDetailScreen
import com.mileway.feature.logging.ui.screens.ExpenseHistoryScreen
import com.mileway.feature.logging.ui.screens.ExpenseScreen
import com.mileway.feature.logging.ui.screens.LogMilesHistoryScreen
import com.mileway.feature.logging.ui.screens.LogMilesScreen
import com.mileway.feature.logging.ui.screens.LogMilesStep2Screen
import com.mileway.feature.logging.ui.screens.CardsTxnHistoryScreen
import com.mileway.feature.logging.ui.screens.SettlementHistoryScreen
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.logging.ui.screens.VoucherDetailsScreen
import com.mileway.feature.logging.ui.screens.VoucherHistoryScreen
import com.mileway.feature.logging.viewmodel.VoucherDetailsViewModel
import com.mileway.feature.media.di.mediaModule
import com.mileway.feature.media.model.FlashMode
import com.mileway.feature.media.ui.camera.CameraCaptureScreen
import com.mileway.feature.media.ui.screens.AttachmentPreviewScreen
import com.mileway.feature.media.ui.screens.AttachmentSelectionScreen
import com.mileway.feature.media.ui.screens.CloudLibraryScreen
import com.mileway.feature.media.viewmodel.MediaViewModel
import com.mileway.feature.payables.di.payablesModule
import com.mileway.feature.payables.ui.screens.CreateInvoiceScreen
import com.mileway.feature.payables.ui.screens.CreatePurchaseRequestScreen
import com.mileway.feature.payables.ui.screens.PayablesHistoryScreen
import com.mileway.feature.payables.ui.screens.PayablesHomeScreen
import com.mileway.feature.payables.ui.screens.PurchaseRequestDetailsScreen
import com.mileway.feature.payments.di.paymentsModule
import com.mileway.feature.payments.ui.screens.CreatePaymentScreen
import com.mileway.feature.payments.ui.screens.PaymentsHistoryScreen
import com.mileway.feature.profile.di.profileModule
import com.mileway.feature.profile.ui.screens.AccountDeletionScreen
import com.mileway.feature.profile.ui.screens.ActiveSessionsScreen
import com.mileway.feature.profile.ui.screens.AdvanceHistoryScreen
import com.mileway.feature.profile.ui.screens.AdvanceRequestDetailsScreen
import com.mileway.feature.profile.ui.screens.AnalyticsDetailScreen
import com.mileway.feature.profile.ui.screens.AnalyticsHomeScreen
import com.mileway.feature.profile.ui.screens.AskAdvanceFormScreen
import com.mileway.feature.profile.ui.screens.ClubBenefitsScreen
import com.mileway.feature.profile.ui.screens.ConnectedAccountsScreen
import com.mileway.feature.profile.ui.screens.CouponsScreen
import com.mileway.feature.profile.ui.screens.DelegationScreen
import com.mileway.feature.profile.ui.screens.DemoSettingsScreen
import com.mileway.feature.profile.ui.screens.DocumentDetailScreen
import com.mileway.feature.profile.ui.screens.EcoDashboardScreen
import com.mileway.feature.profile.ui.screens.EmergencyContactsScreen
import com.mileway.feature.profile.ui.screens.FavouriteRoutesScreen
import com.mileway.feature.profile.ui.screens.HelpScreen
import com.mileway.feature.profile.ui.screens.IncentiveProgramsScreen
import com.mileway.feature.profile.ui.screens.ManagerReporteesScreen
import com.mileway.feature.profile.ui.screens.MarketingHubScreen
import com.mileway.feature.profile.ui.screens.MySubscriptionScreen
import com.mileway.feature.profile.ui.screens.MyTicketsScreen
import com.mileway.feature.profile.ui.screens.NotificationCentreScreen
import com.mileway.feature.profile.ui.screens.OffersHubScreen
import com.mileway.feature.profile.ui.screens.OrgChartScreen
import com.mileway.feature.profile.ui.screens.PlansScreen
import com.mileway.feature.profile.ui.screens.PluginManagerScreen
import com.mileway.feature.profile.ui.screens.PreferencesScreen
import com.mileway.feature.profile.ui.screens.ProfileDetailsScreen
import com.mileway.feature.profile.ui.screens.ProfileScreen
import com.mileway.feature.profile.ui.screens.QrHomeScreen
import com.mileway.feature.profile.ui.screens.ReferralHubScreen
import com.mileway.feature.profile.ui.screens.RewardsScreen
import com.mileway.feature.profile.ui.screens.RootGuardScreen
import com.mileway.feature.profile.ui.screens.SavedPlacesScreen
import com.mileway.feature.profile.ui.screens.SelfAuditScreen
import com.mileway.feature.profile.ui.screens.SettingsScreen
import com.mileway.feature.profile.ui.screens.StorageManagementScreen
import com.mileway.feature.profile.ui.screens.SupportChatScreen
import com.mileway.feature.profile.ui.screens.SupportHubScreen
import com.mileway.feature.profile.ui.screens.TrainingTourScreen
import com.mileway.feature.profile.ui.screens.VehicleGarageScreen
import com.mileway.feature.profile.ui.screens.VerificationCentreScreen
import com.mileway.feature.tracking.debug.DebugMenuScreen
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.ui.components.DiscardJourneyDialog
import com.mileway.feature.tracking.ui.components.ExportOptionsDialog
import com.mileway.feature.tracking.ui.onboarding.PermissionPrimerController
import com.mileway.feature.tracking.ui.onboarding.PermissionPrimerSheet
import com.mileway.feature.tracking.ui.review.DriveReviewSheet
import com.mileway.feature.tracking.ui.screens.CheckInHistoryItem
import com.mileway.feature.tracking.ui.screens.CheckInHistoryScreen
import com.mileway.feature.tracking.ui.screens.CreateVoucherScreen
import com.mileway.feature.tracking.ui.screens.GeoCheckInScreen
import com.mileway.feature.tracking.ui.screens.HardwareEventsLogScreen
import com.mileway.feature.tracking.ui.screens.RouteReplayScreen
import com.mileway.feature.tracking.ui.screens.ManualCheckInScreen
import com.mileway.feature.tracking.ui.screens.SavedTracksScreen
import com.mileway.feature.tracking.ui.screens.SetupGuideScreen
import com.mileway.feature.tracking.ui.screens.TrackCustomizationScreen
import com.mileway.feature.tracking.ui.screens.TrackDataPreviewScreen
import com.mileway.feature.tracking.ui.screens.TrackDetailScreen
import com.mileway.feature.tracking.ui.screens.TrackInsightsScreen
import com.mileway.feature.tracking.ui.screens.TrackLoadingScreen
import com.mileway.feature.tracking.ui.screens.TrackMilesScreen
import com.mileway.feature.tracking.ui.screens.TrackSettingsScreen
import com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen
import com.mileway.feature.tracking.ui.sheets.CenterOption
import com.mileway.feature.tracking.ui.sheets.EntityPickerSheet
import com.mileway.feature.tracking.ui.sheets.JourneyGuideSheet
import com.mileway.feature.tracking.ui.sheets.JourneyGuideState
import com.mileway.feature.tracking.ui.sheets.JourneyGuideStep
import com.mileway.feature.tracking.ui.sheets.OfficePickerSheet
import com.mileway.feature.tracking.ui.sheets.PauseReasonSheet
import com.mileway.feature.tracking.ui.sheets.PermissionOnboardingSheet
import com.mileway.feature.tracking.ui.sheets.PolicyViolationSheet
import com.mileway.feature.tracking.ui.sheets.ResumeTrackingSheet
import com.mileway.feature.tracking.ui.sheets.RestorableSession
import com.mileway.feature.tracking.ui.sheets.SessionRestoreSheet
import com.mileway.feature.tracking.ui.sheets.SmartDistanceSheet
import com.mileway.feature.tracking.ui.sheets.SosBottomSheet
import com.mileway.feature.tracking.ui.sheets.StrangerSessionSheet
import com.mileway.feature.tracking.ui.sheets.SubmitConfirmSheet
import com.mileway.feature.tracking.ui.sheets.VehicleOption
import com.mileway.feature.tracking.ui.sheets.VehiclePickerSheet
import com.mileway.feature.tracking.ui.sheets.VendorPickerSheet
import com.mileway.feature.tracking.viewmodel.StrangerSessionConfig
import com.mileway.feature.travel.di.travelModule
import com.mileway.feature.travel.ui.screens.BookingHistoryScreen
import com.mileway.feature.travel.ui.screens.CreateMjpScreen
import com.mileway.feature.travel.ui.screens.CreateTripScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.feature.travel.ui.screens.TripHistoryScreen
import com.mileway.feature.whatsnew.data.WhatsNewCatalog
import com.mileway.feature.whatsnew.di.whatsNewFeatureModule
import com.mileway.stub.di.stubModule
import com.mileway.ui.AssistantHomeSheet
import com.mileway.ui.ShellPlaceholderScreen
import com.mileway.ui.auth.LoginScreen
import com.mileway.ui.auth.OnboardingFormConfig
import com.mileway.ui.auth.SignupOnboardingScreen
import com.mileway.ui.auth.SplashScreen
import com.mileway.ui.auth.authModule
import com.mileway.ui.home.HomeScreenContent
import com.mileway.ui.home.HomeUiState
import com.mileway.ui.home.WhatsNewSheet
import com.mileway.ui.home.homeModule
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import com.siddharth.kmp.appshell.LoggingAnalyticsHelper
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.PermissionsProvider
import com.siddharth.kmp.common.CrashReporter
import dev.tmapps.konnection.Konnection
import io.mockk.coEvery
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// ---------------------------------------------------------------------------
// Full Roborazzi screen gallery for the docs/ screenshot catalogue.
//
// A single Koin graph carries every feature module (tracking / logging / travel
// / payables / payments / events / cards / agent / profile / media / approvals
// / home + appModule) plus a `fakeRoomLayer` that supplies the DAOs, the concrete
// data-layer singletons (CurrentTrackDataStore, DemoSettingsRepository) and the
// MapSurface those modules need. coreDataModule, mapsKoinModule() and
// platformServicesKoinModule() are intentionally NOT included, they would build
// Room / GMS / MapLibre against a mock Context and crash on the JVM. The fakes
// stand in for exactly the boundary they own.
//
// Record / update:
//   ./gradlew :app:testNoGmsDebugUnitTest \
//     --tests "com.mileway.ScreenshotGalleryTest" -Proborazzi.test.record=true
//
// Output: docs/screenshots/<name>.png
// ---------------------------------------------------------------------------

// Use plain Application to skip MilewayApplication.onCreate → startKoin.
// qualifiers pins a realistic phone viewport (411×891 dp, mdpi) so chip/button rows
// (booking & expense status filters, check-in actions) lay out the way they do on a
// real device instead of overflowing in the narrow 320 dp Robolectric default.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotGalleryTest {
    /**
     * Fixed "now" for screens that render relative timestamps. 2026-01-01T12:00:00Z — midday so a
     * "hours ago" value can't cross a day boundary, and pinned so the rendered text (and therefore
     * the PNG bytes) can't drift with the wall clock.
     */
    private val screenshotNowMs = 1_767_268_800_000L


    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        // PLAN_V33 A6 LANDMINE (.ralph/PROGRESS.md): TrackingModule wires VehiclePricingRepository's
        // isOnline to NetworkMonitor::isConnectedNow, which reads Konnection.instance. MilewayApplication
        // .onCreate() calls Konnection.createInstance(this) to set it up; this test's bare Application
        // skips that (same gap as the ComposeResourcesTestFixture install below), so any screen that
        // resolves VehiclePricingRepository throws "KonnectionConfig is not initialized". Guarded so the
        // real createInstance() (which registers a ConnectivityManager callback) runs exactly once.
        private var konnectionInitialized = false

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
                MediaLibraryEntry("m1", "file:///demo/odometer.jpg", "image/jpeg", "Odometer: Pune", "CAMERA", baseMs - 3_600_000L),
                MediaLibraryEntry("m2", "file:///demo/fuel.jpg", "image/jpeg", "Fuel receipt: Hinjewadi", "GALLERY", baseMs - 7_200_000L),
                MediaLibraryEntry("m3", "file:///demo/toll.jpg", "image/jpeg", "Toll receipt: Mumbai Expressway", "CAMERA", baseMs - 10_800_000L),
                MediaLibraryEntry("m4", "file:///demo/parking.jpg", "image/jpeg", "Parking: Magarpatta", "GALLERY", baseMs - 14_400_000L),
                MediaLibraryEntry("m5", "file:///demo/invoice.jpg", "image/jpeg", "Cab invoice: Koregaon Park", "CAMERA", baseMs - 18_000_000L),
                MediaLibraryEntry("m6", "file:///demo/meal.jpg", "image/jpeg", "Meal receipt: FC Road", "GALLERY", baseMs - 21_600_000L),
            )
            every { dao.observeAll() } returns MutableStateFlow(entries)
        }

        // P3.1: a deterministic in-memory fake (not a mockk) so VoucherHistoryScreen and
        // CreateVoucherScreen's screenshots keep rendering the same rows they always did — a bare
        // `mockk(relaxed = true)` would return a null-backed Flow and crash
        // VoucherHistoryViewModel's collector (memory: screenshot Koin needs deterministic fakes).
        private val voucherDao = FakeVoucherDao()

        private val fakeRoomLayer = module {
            single<SavedTrackDao> { seededDao }
            single<LocationDao> { mockk(relaxed = true) }
            single<HardwareEventDao> { mockk(relaxed = true) }
            // P5.1: LogMilesViewModel.init now collectLatest's getAllDrafts(); a relaxed mockk
            // returns a null-backed Flow that crashes that collector (memory: screenshot Koin
            // needs deterministic fakes, same reason FakeVoucherDao exists below).
            single<LogMilesDraftDao> { FakeLogMilesDraftDao() }
            // Wave 3: LogMilesViewModel.init now also collectLatest's observeAllRoutes(); same
            // relaxed-mockk-null-Flow trap as LogMilesDraftDao above.
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
            // PLAN_V33 A5/A6 LANDMINE: SyncStatusViewModel.init() eagerly calls MilesSubmitSyncer.drain(),
            // which does `outbox.drafts(FORM_KEY).first()`. TripDraftOutbox (SubmitOutbox<TripDraft>) is
            // normally bound in the excluded CoreDataModule; without a binding here it falls through to
            // whatever relaxed mockk<SubmitOutbox<*>> is in scope, whose Flow-returning drafts() never
            // emits — .first() then throws NoSuchElementException. Deterministic fake keeps that resolving.
            single<com.mileway.core.data.outbox.TripDraftOutbox> { FakeTripDraftOutbox() }
            // P6.2: PersonalDetailsViewModel collects both of these in init(); a relaxed mockk
            // would return a null-backed Flow and crash the collector (memory: screenshot Koin
            // needs deterministic fakes, same reason FakeVoucherDao exists above).
            single<VehicleDetailsDao> { FakeVehicleDetailsDao() }
            single<PassportDetailsDao> { FakePassportDetailsDao() }
            // P12.7: SignatureViewModel combines SignatureDao.observe() with the plugin registry;
            // a relaxed mockk would return a null-backed Flow and crash the collector (memory:
            // screenshot Koin needs deterministic fakes, same null-collector trap as above).
            single<SignatureDao> { FakeSignatureDao() }
            // P6.3: DelegationViewModel collects this in init(); same null-collector trap as above.
            single<DelegationDao> { FakeDelegationDao() }
            // P6.4: ActiveSessionsViewModel collects this in init(); same null-collector trap as above.
            single<SessionDao> { FakeSessionDao() }
            // P6.5: NotificationViewModel collects this in init(); same null-collector trap as above.
            single<NotificationDao> { FakeNotificationDao() }
            // P6.6: ConnectedAccountsViewModel collects this in init(); same null-collector trap as above.
            single<ConnectedAccountDao> { FakeConnectedAccountDao() }
            // PLAN_V24 P8.1: ConnectedAccountsViewModel now also seeds/collects the wallet DAO + uses
            // the OTP engine (wallet link flow). Section hidden by default (walletLinkingEnabled off).
            single<com.mileway.core.data.dao.PaymentWalletDao> { FakePaymentWalletDao() }
            single { com.mileway.core.data.otp.LocalOtpEngine() }
            // PLAN_V24 super-profile screens (Plugins / Verification / Referral / Coupons / Rewards /
            // Marketing / Membership / Subscription / Incentives / Account-deletion / Saved-places /
            // Emergency-contacts / Manager-reportees). The real CoreDataModule isn't loaded here, so
            // supply each Room DAO fake + the core:data repositories the VMs collect in init(). These
            // mirror KoinGraphTest's fake layer 1:1.
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
            // PLAN_V28 P28.2: ApprovalDetailsScreen's ApprovalsViewModel.openDetail eagerly
            // combine/collects ClarificationRepository Flows during Compose render — approvalsModule
            // itself binds the real ClarificationRepository(get<ClarificationDao>()), so only the
            // DAO fake is needed here (a relaxed mockk would hand back a null-backed Flow and crash).
            single<com.mileway.core.data.dao.ClarificationDao> { FakeClarificationDao() }
            // PLAN_V28 P28.7: same eager-collect trap as ClarificationDao above, for
            // ApprovalCommentRepository's observeComments(...).
            single<com.mileway.core.data.dao.ApprovalCommentDao> { FakeApprovalCommentDao() }
            single<com.mileway.core.data.dao.SubscriptionDao> { FakeSubscriptionDao() }
            single { com.mileway.core.data.subscription.SubscriptionRepository(get()) }
            single<com.mileway.core.data.dao.DeletionRequestDao> { FakeDeletionRequestDao() }
            single { com.mileway.core.data.lifecycle.DeletionRequestRepository(get(), get()) }
            // PinViewModel (Set/Check-PIN screens) now takes a PinLockoutSource + Clock, and
            // SearchLocationViewModel (LogMiles step-1 location sheet) a SavedLocationsSource — both
            // normally bound in the excluded CoreDataModule. In-memory/no-op fakes (a relaxed mockk's
            // null Flow would crash the collectors) mirroring KoinGraphTest's bindings.
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
            // P6.8: SupportTicketViewModel collects this in init() (HelpScreen + MyTicketsScreen);
            // same null-collector trap as above.
            single<SupportTicketDao> { FakeSupportTicketDao() }
            single<AgentSessionStore> { FakeAgentSessionStore() }
            single<AssistantEngine> { FakeAssistantEngine() }
            single<SpeechToText> { FakeSpeechToText() }
            single<TextToSpeech> { FakeTextToSpeech() }
            single<CurrentTrackDataStore> { mockk(relaxed = true) }
            single<CurrentTrackDataSource> { get<CurrentTrackDataStore>() }
            // P2.1: ProfileViewModel reads this in init; a real in-memory fake avoids the
            // Flow<String?>-from-relaxed-mockk null-collector trap (memory: screenshot Koin
            // needs deterministic fakes, same reason FakeVoucherDao exists above).
            single<ActiveAccountSource> { FakeActiveAccountSource() }
            // PLAN_V24 P7.3: DelegationScreen/ProfileScreen resolve the session-delegation overlay +
            // (via DelegateSessionViewModel) the PluginRegistry. FakeActiveAccountSource emits a null
            // active account, so PluginRegistry never touches the relaxed-mockk override DAO.
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
            // P2.3: SwitchAccountViewModel.verify() reads this; a real in-memory fake avoids the
            // suspend-fun-on-a-relaxed-mockk trap (memory: screenshot Koin needs deterministic fakes).
            single<PinHashSource> { FakePinHashSource() }
            // P6.5: ProfileViewModel now collects `settings` eagerly in init() (Notification
            // Center channel toggles); a relaxed mockk's auto-generated Flow<DemoSettings> is not
            // guaranteed to behave like a real Flow under `.onEach{}.launchIn()` (memory:
            // screenshot Koin needs deterministic fakes), so a real MutableStateFlow-backed stub
            // is used instead.
            single<DemoSettingsRepository> {
                mockk {
                    every { settings } returns MutableStateFlow(com.mileway.core.data.settings.DemoSettings())
                }
            }
            // Wave-2 AbnormalDetectionConfig: trackingModule's TrackingConfigManager resolves this
            // DataStore-backed source; bind a DEFAULT-only fake like the other data-layer stubs here.
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
            // P3.4: ProfileViewModel now depends on MockAccountSessionCoordinator (pause/restore hook).
            single { MockAccountSessionCoordinator(get(), get(), get()) }
            // Map screens (GeoCheckIn, LocationMap, LiveTrack, LogMiles thumbnail) inject
            // MapSurface; the real flavor surfaces need GMS / MapLibre native, so use a
            // no-op fake on the JVM. mapsKoinModule() is deliberately excluded.
            single<MapSurface> { FakeMapSurface() }
            // TrackMilesScreen koinInject()s this for the permission primer. platformServices
            // KoinModule() is deliberately excluded here (it builds GMS/MapLibre against a mock
            // Context), so bind a no-op the same way MapSurface is faked.
            single<SystemSettingsOpener> { object : SystemSettingsOpener { override fun openAppSettings() = Unit } }
            // BugReportSheet (core:ui) resolves BugReportViewModel(BugReportRepository) — the
            // repository is normally bound in the excluded CoreDataModule; same null-collector-free
            // relaxed-mockk pattern as the other DAOs above (submit() is a fire-and-forget suspend
            // call, never awaited by the sheet, so a relaxed no-op DAO is enough).
            single<com.mileway.core.data.dao.BugReportDao> { mockk(relaxed = true) }
            single { com.mileway.core.data.support.BugReportRepository(get()) }
            // FavouriteRoutesScreen: FavouriteRoutesRepository combines both flows in its VM's
            // init{} — a relaxed mockk's null-backed Flow would crash the collector (memory:
            // screenshot Koin needs deterministic fakes), so both are seeded MutableStateFlows.
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
            // VehicleGarageScreen / SelfAuditScreen: GarageRepository.observeAll() is combine()'d
            // in both VMs' init{} — same null-collector trap as above, seeded with two demo vehicles.
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
                                com.mileway.core.data.model.db.VehicleEntity(
                                    id = "veh_seed_2", brand = "Maruti Suzuki", model = "Swift",
                                    registrationNumber = "MH12CD5678", year = 2021, color = "White",
                                    seats = 5, vehicleTypeKey = "fourWheelerPetrol", isActive = false,
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
            // SavedTrackDao already seeded above (seededDao) — EcometerRepository derives its
            // totals from real completed tracks, no extra binding needed.
            single { com.mileway.core.data.vehicle.EcometerRepository(get()) }
            // TrainingTourScreen: TourRepository.observe() is stateIn()'d in the VM — same
            // null-collector trap as above.
            single<com.mileway.core.data.dao.TourProgressDao> {
                mockk(relaxed = true) {
                    every { observe(any()) } returns MutableStateFlow(null)
                }
            }
            single { com.mileway.core.data.engagement.TourRepository(get(), get()) }
            // StorageManagementScreen: StorageRepository(Context) reads real getDatabasePath()/
            // cacheDir sizes. Not bound against Robolectric's real ApplicationProvider context or the
            // graph's relaxed mockk<Context> — StorageRepositoryTest's own doc records that real
            // cacheDir I/O against a Robolectric-managed Context corrupted its temp-dir bookkeeping on
            // Linux CI (Z.5b). Same fix that test uses: a mockk<Context> answering into real
            // java.io.File temp dirs this JVM owns outright, so I/O is real (a populated, non-empty
            // list renders) without touching Robolectric's managed sandbox. Deliberately NOT bound as
            // `single<Context>` — that type is already the graph-wide relaxed mockk from
            // androidContext() above; declaring a second one here would override it for every other
            // test in this class, not just this screen.
            single {
                val cache = kotlin.io.path.createTempDirectory("mileway-screenshot-cache").toFile()
                val databases = kotlin.io.path.createTempDirectory("mileway-screenshot-db").toFile()
                val files = kotlin.io.path.createTempDirectory("mileway-screenshot-files").toFile()
                val storageContext =
                    mockk<Context> {
                        every { cacheDir } returns cache
                        every { filesDir } returns files
                        every { getDatabasePath(any()) } answers { File(databases, firstArg()) }
                    }
                com.mileway.core.data.settings.StorageRepository(storageContext)
            }
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
            // PLAN_V33 A6 LANDMINE: trackingModule's real binding is VehiclePricingCacheStore(androidContext()),
            // a DataStore-Preferences store — it needs a working Context.filesDir, but this harness's Koin
            // androidContext() is a fully relaxed mockk (no real files dir), so DataStore throws a
            // NullPointerException the first time TrackMilesViewModel.loadVehicles() reads the cache.
            // InMemoryVehiclePricingCache is VehiclePricingRepository's own gallery/test-safe default; override
            // TrackingModule's real store with it here rather than touching production DI.
            single<com.mileway.feature.tracking.repository.VehiclePricingCache> {
                com.mileway.feature.tracking.repository.InMemoryVehiclePricingCache()
            }
            single<NotificationScheduler> { mockk(relaxed = true) }
            // CheckPinScreen koinInjects this (biometric-unlock affordance); the real impl is bound by
            // the excluded platformModule. Relaxed mockk → isAvailable()=false, so the screen renders
            // its PIN-entry state.
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
            // These are record-only documentation screenshots written to a custom docs/
            // path (the README gallery), not Roborazzi's tracked output dir, so force
            // record here, matching the project convention. The strict verifyRoborazzi
            // gate covers the deterministic component previews in ScreenshotCatalogTest.
            // D2 FIX (2026-08-09): this line used to be
            //   System.setProperty("roborazzi.test.record", "true")
            // which forced RECORD mode unconditionally, so every run overwrote the baseline and a
            // visual regression was literally unrepresentable — the test rewrote the evidence and
            // passed. That is why captures went stale and wrong for months with nothing alerting.
            // Verify is now the default; record only when explicitly asked:
            //   ./gradlew screenshotTest -Proborazzi.test.record=true
            //
            // D3 FIX (2026-08-09): the CLI flag above is dead for this class specifically. app's
            // `screenshotTestNoGmsDebug` (build.gradle.kts) is a plain custom `Test` task, not one of
            // the tasks Roborazzi's Gradle plugin auto-wires the `roborazzi.test.*` JVM system
            // properties onto (that wiring targets only its own generated
            // record/verifyRoborazziNoGmsDebug tasks, which this project's screenshotTestFilter
            // explicitly excludes this class from — the whole reason the fork exists). Confirmed by
            // decompiling roborazzi-core-jvm 1.70.0: every roborazzi.* property, including
            // `roborazzi.output.dir`, is read via a bare `System.getProperty(...)` with no
            // roborazzi.properties fallback, and with none of them ever landing as a real property in
            // this forked JVM, `RoborazziTaskType.of(false,false,false) == None` — every
            // `captureRoboImage()` call silently no-ops (no compare, no write, no failure). That is why
            // captures stayed stale no matter what CLI flag was passed, including record-flagged runs.
            // Setting `System.setProperty("roborazzi.test.record"/".verify", ...)` here from inside the
            // JVM that actually reads it fixes the record/verify flag (verified). It does NOT fix
            // `roborazzi.output.dir` the same way — setting that property here made no observable
            // difference (still landed in the `app/` module root, not docs/screenshots), which the
            // capture() helper below works around directly instead of relying on further guessing at
            // roborazzi-core's file-resolution path.
            //
            // Record mode is opt-in via an env var rather than `-P` (which can't reach a forked test
            // JVM without build-script changes, out of this file's ownership) — same "record only when
            // explicitly asked" intent as the D2 FIX above, via a channel that actually works:
            //   ROBORAZZI_RECORD=true ./gradlew :app:screenshotTestNoGmsDebug
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
                    // PLAN_V36 P8: SettingsScreen koinInjects WhatsNewVersionProvider for its badge
                    // (spec §5, wired since P2) — never added here when that landed, so
                    // `settingsScreen` threw NoDefinitionFoundException the first time this gallery
                    // ran against the whatsnew work.
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

    // Compose Multiplatform resources (1.12) resolve `Res.string.*` through an Android Context that
    // is normally wired by an auto-init ContentProvider. Under Robolectric with a plain Application
    // that provider doesn't run, so string-backed screens throw MissingResourceException ("Android
    // context is not initialized"). Feed the real Robolectric app context (its merged assets carry
    // every module's composeResources) via the library's documented escape hatch. This must run in a
    // per-test @Before (not @BeforeClass) — ApplicationProvider needs Robolectric's per-test
    // environment, which isn't set up yet during static class init. The static it sets persists for
    // the whole JVM fork, so re-setting it each test is cheap and idempotent.
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

    // ── Tracking ───────────────────────────────────────────────────────────────

    @Test
    fun trackMilesIdleScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
                SavedTracksScreen(onTrackClick = {}, onStartNew = {})
            }
        }
        capture("saved_tracks_journeys_tab")
        // FILLED variant (UI-realism audit): same real render as above — seededDao's 4 journeys
        // (mixed submitted/unsubmitted status, real km/₹ amounts) — just under the paired
        // <screen>_filled.png / <screen>_empty.png naming so the pair sits together in docs/.
        capture("saved_tracks_screen_filled")
    }

    // FILLED/EMPTY pair (UI-realism audit, tracking-content): the day-one state a brand-new
    // account sees before recording a single journey — driven by a ViewModel wired to its own
    // empty FakeSavedTrackDao rather than the class-level seededDao every other test shares
    // (seededDao is reused across this whole class; mutating it would break those tests).
    @Test
    fun savedTracksScreenEmpty() {
        composeRule.setContent {
            MilewayTheme {
                SavedTracksScreen(
                    onTrackClick = {},
                    onStartNew = {},
                    viewModel =
                        com.mileway.feature.tracking.viewmodel.SavedTracksViewModel(
                            repository = com.mileway.feature.tracking.repository.SavedTrackRepository(FakeSavedTrackDao()),
                            activeAccountSource = FakeActiveAccountSource(),
                        ),
                )
            }
        }
        capture("saved_tracks_screen_empty")
    }

    @Test
    fun trackDetailScreen() {
        composeRule.setContent {
            MilewayTheme {
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
        // FILLED variant (UI-realism audit, tracking-content): route-j1's real distance/duration/
        // amount data, paired with the not-found EMPTY variant below under the standardized name.
        capture("track_detail_screen_filled")
    }

    // ROUND 2: "Journey not found" — an unseeded routeId (deleted record, stale deep link) drives
    // TrackDetailViewModel.state.rawTrack to null after load finishes, which TrackDetailScreen now
    // renders as an explicit EmptyState instead of a blank body under a live top bar.
    @Test
    fun trackDetailScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                TrackDetailScreen(
                    routeId = "route-does-not-exist",
                    onBack = {},
                    onOpenInsights = {},
                    onOpenMap = {},
                    onOpenHwEvents = {},
                    onOpenDataPreview = {},
                )
            }
        }
        capture("track_detail_screen_not_found")
        // EMPTY variant (UI-realism audit, tracking-content): a deleted/never-recorded routeId is
        // the day-one "no data" shape for this single-record screen — no journeys have ever been
        // saved yet, so this is the honest EMPTY pair for track_detail_screen_filled above.
        capture("track_detail_screen_empty")
    }

    @Test
    fun trackInsightsScreen() {
        composeRule.setContent {
            MilewayTheme {
                TrackInsightsScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("track_insights_screen")
    }

    // ROUND 2: error-with-retry. TrackInsightsViewModel.loadInsights sets error="Track not found"
    // for an unseeded routeId; the screen renders that message with a Retry button that re-issues
    // the same Load action.
    @Test
    fun trackInsightsScreenErrorRetry() {
        composeRule.setContent {
            MilewayTheme {
                TrackInsightsScreen(routeId = "route-does-not-exist", onBack = {})
            }
        }
        capture("track_insights_screen_error_retry")
    }

    @Test
    fun hardwareEventsLogScreen() {
        composeRule.setContent {
            MilewayTheme {
                HardwareEventsLogScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("hardware_events_log_screen")
    }

    @Test
    fun trackDataPreviewOverviewTab() {
        composeRule.setContent {
            MilewayTheme {
                TrackDataPreviewScreen(routeId = "route-j1", onBack = {})
            }
        }
        capture("track_data_preview_overview_tab")
    }

    // ROUND 2: "Journey not found" — same unseeded-routeId condition as trackDetailScreenNotFound,
    // TrackDataPreviewScreen shares TrackDetailViewModel and renders the identical EmptyState fix.
    @Test
    fun trackDataPreviewScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                TrackDataPreviewScreen(routeId = "route-does-not-exist", onBack = {})
            }
        }
        capture("track_data_preview_screen_not_found")
    }

    // LiveTrackScreen is intentionally omitted: it needs an active in-progress track
    // session (CurrentTrackDataStore must emit a track with locations); with the offline
    // fakes it renders a "Failed to load" state. track_miles_idle + tracking_success
    // already document the live-tracking flow.

    @Test
    fun locationMapScreen() {
        composeRule.setContent {
            // RouteReplayScreen is a bare Box with no background of its own — see ThemedBackground's
            // doc for why this needs the explicit wrapper instead of bare MilewayTheme.
            ThemedBackground {
                RouteReplayScreen(onNavigateBack = {})
            }
        }
        capture("location_map_screen")
    }

    // ROUND 2: ERROR state. CurrentTrackDataStore is a relaxed mockk with no active session
    // seeded (fakeRoomLayer), so LiveTrackViewModel's combined state naturally lands in
    // LiveTrackingUiState.Error — RouteReplayScreen now renders that with DefaultErrorState +
    // retry instead of collapsing it into the same infinite spinner as Loading (tracking-tail fix).
    // Named explicitly since locationMapScreen above documents the same underlying render today
    // but under a filename that predates the fix.
    @Test
    fun routeReplayErrorState() {
        composeRule.setContent {
            ThemedBackground {
                RouteReplayScreen(onNavigateBack = {})
            }
        }
        capture("route_replay_error_state")
    }

    // ROUND 2: "Trip not found". TrackEvidenceScreen itself is stateless (takes a non-null
    // SavedTrack) — the not-found branch lives inline in TrackingNavigation's EVIDENCE route
    // (androidMain, built on a real NavController), not as a reusable composable. Reproduced here
    // by driving the same TrackDetailViewModel + DefaultEmptyState the nav route uses, rather than
    // standing up a full NavHost just for one screenshot.
    @Test
    fun trackEvidenceTripNotFoundScreen() {
        composeRule.setContent {
            MilewayTheme {
                val viewModel: com.mileway.feature.tracking.viewmodel.TrackDetailViewModel = koinViewModel()
                LaunchedEffect(Unit) {
                    viewModel.onAction(com.mileway.feature.tracking.viewmodel.TrackDetailAction.Load("route-does-not-exist"))
                }
                val state by viewModel.state.collectAsState()
                // The real nav route sits inside a Scaffold that paints the themed background;
                // reproduced explicitly here since this composable stands alone with no Scaffold
                // of its own — without it the EmptyState's muted text renders on the test
                // environment's raw (light) canvas instead of the app's real dark surface.
                androidx.compose.foundation.layout.Box(
                    modifier =
                        androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    when {
                        state.rawTrack != null -> TrackEvidenceScreen(track = state.rawTrack!!)
                        state.isLoading -> Unit
                        else ->
                            com.mileway.core.ui.mvi.DefaultEmptyState(
                                title = "Trip not found",
                                subtitle = "This record may have been deleted.",
                                ctaLabel = "Go back",
                                onCta = {},
                            )
                    }
                }
            }
        }
        capture("track_evidence_trip_not_found")
    }

    @Test
    fun geoCheckInScreen() {
        composeRule.setContent {
            MilewayTheme {
                GeoCheckInScreen(onBack = {})
            }
        }
        capture("geo_check_in_screen")
        // EMPTY variant (UI-realism audit, tracking-content): the day-one blank form — check-in
        // type still shows its placeholder, no dynamic fields yet (fixes the placeholder-as-value
        // bug where "Select type" used to look like a real answer).
        capture("geo_check_in_screen_empty")
    }

    // FILLED variant (UI-realism audit, tracking-content) paired with the EMPTY state above: a
    // type picked and both of its dynamic fields filled in, stopping short of submit so the form
    // itself — not the submission-error state already covered by geoCheckInSubmissionError — is
    // what's captured.
    @Test
    fun geoCheckInScreenFilled() {
        composeRule.setContent {
            MilewayTheme {
                GeoCheckInScreen(onBack = {})
            }
        }
        // The type field's "Select type" copy is now the OutlinedTextField's `placeholder` (the
        // label/placeholder fix this pairs with) — Material3 only renders a placeholder once the
        // field has focus when a label is also present, so it isn't in the semantics tree yet to
        // click on. "Check-In Type" appears twice (the section header, then the field's own
        // label) — the field's own label is the one spatially inside the clickable field.
        composeRule.onAllNodesWithText("Check-In Type")[1].performClick()
        composeRule.onNodeWithText("Office Check-In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desk number").performTextInput("D-114")
        composeRule.onNodeWithText("Floor").performTextInput("3rd Floor, West Wing")
        composeRule.waitForIdle()
        capture("geo_check_in_screen_filled")
    }

    // ROUND 1 (never captured): submission-error. GeoCheckInScreen owns this submit flow with
    // local Compose state (no ViewModel), so a broken HardwareEventRepository is passed directly
    // via the screen's default-koinInject() param — same seam ManualCheckInScreen uses below.
    // Drives the real form: pick a check-in type, fill its two dynamic fields, submit.
    @Test
    fun geoCheckInSubmissionError() {
        val brokenDao = mockk<HardwareEventDao>(relaxed = true)
        coEvery { brokenDao.insert(any()) } throws RuntimeException("Couldn't reach local storage")
        composeRule.setContent {
            MilewayTheme {
                GeoCheckInScreen(
                    onBack = {},
                    hardwareEventRepository =
                        com.mileway.feature.tracking.repository.HardwareEventRepository(brokenDao),
                )
            }
        }
        composeRule.onNodeWithText("Select type").performClick()
        composeRule.onNodeWithText("Office Check-In").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desk number").performTextInput("D-1")
        composeRule.onNodeWithText("Floor").performTextInput("3")
        // "Check In" is both the top-bar title and the submit button's label; hasClickAction()
        // is the only thing that tells them apart.
        composeRule.onNode(hasText("Check In") and hasClickAction()).performClick()
        // doCheckIn() has a real delay(1_200) before the write — waitForIdle() only drains pending
        // recomposition/animation work, it does not fast-forward a suspended coroutine's delay().
        // Advance Robolectric's main-looper clock so that delayed continuation actually resumes.
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        capture("geo_check_in_submission_error")
    }

    @Test
    fun manualCheckInScreen() {
        composeRule.setContent {
            MilewayTheme {
                ManualCheckInScreen(onBack = {})
            }
        }
        capture("manual_check_in_screen")
    }

    // ROUND 1 (never captured): submission-error. Same seam as geoCheckInSubmissionError above —
    // ManualCheckInScreen has no ViewModel, so a broken HardwareEventRepository is injected via
    // the screen's default parameter.
    @Test
    fun manualCheckInSubmissionError() {
        val brokenDao = mockk<HardwareEventDao>(relaxed = true)
        coEvery { brokenDao.insert(any()) } throws RuntimeException("Couldn't reach local storage")
        composeRule.setContent {
            MilewayTheme {
                ManualCheckInScreen(
                    onBack = {},
                    hardwareEventRepository =
                        com.mileway.feature.tracking.repository.HardwareEventRepository(brokenDao),
                )
            }
        }
        composeRule.onNodeWithText("Reason / Notes").performTextInput("Client meeting ran long")
        composeRule.onNodeWithText("Submit Check-In").performClick()
        // submitCheckIn() has a real delay(800) before the write — see the identical comment on
        // geoCheckInSubmissionError above.
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
        capture("manual_check_in_submission_error")
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
            MilewayTheme {
                CheckInHistoryScreen(events = events, onBack = {})
            }
        }
        capture("check_in_history_screen")
    }


    @Test
    fun trackingSuccessScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
                SetupGuideScreen(onBack = {}, onOpenTrackSettings = {})
            }
        }
        capture("tracking_setup_guide_screen")
    }

    @Test
    fun trackSettingsScreen() {
        composeRule.setContent {
            MilewayTheme {
                TrackSettingsScreen(onBack = {})
            }
        }
        capture("track_settings_screen")
    }

    @Test
    fun trackCustomizationScreen() {
        composeRule.setContent {
            MilewayTheme {
                TrackCustomizationScreen(onBack = {})
            }
        }
        capture("track_customization_screen")
    }

    @Test
    fun trackLoadingScreen() {
        composeRule.setContent {
            MilewayTheme {
                TrackLoadingScreen(message = "Submitting your journey...")
            }
        }
        capture("tracking_loading_screen")
    }

    // ── Logging & Expenses ───────────────────────────────────────────────────────

    @Test
    fun createVoucherSelectExpenses() {
        composeRule.setContent {
            MilewayTheme {
                CreateVoucherScreen(onBack = {})
            }
        }
        capture("create_voucher_select_expenses")
    }

    // ROUND 2: submitError. A broken VoucherDao (insert throws) passed into a throwaway
    // CreateVoucherViewModel — same "one-off dependency, bypass the shared Koin single" pattern
    // voucherDetailsScreen uses further down, so this doesn't corrupt the shared voucherDao other
    // tests read from. Drives the real wizard via onAction (declaration + submit aren't reachable
    // through plain text clicks without knowing every localized label), then captures step 2 with
    // the inline error text and the re-enabled Create button.
    @Test
    fun createVoucherSubmitError() {
        val brokenDao = mockk<VoucherDao>(relaxed = true)
        coEvery { brokenDao.insert(any()) } throws RuntimeException("Disk full")
        val viewModel =
            com.mileway.feature.tracking.viewmodel.CreateVoucherViewModel(
                savedTrackRepository = com.mileway.feature.tracking.repository.SavedTrackRepository(seededDao),
                voucherRepository = com.mileway.feature.tracking.repository.VoucherRepository(brokenDao),
            )
        composeRule.setContent {
            MilewayTheme {
                CreateVoucherScreen(onBack = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.ToggleSelection("route-s1"))
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.GoToStep(1))
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.SetTitle("Voucher: Test"))
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.GoToStep(2))
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.ToggleDeclaration(true))
        composeRule.waitForIdle()
        viewModel.onAction(com.mileway.feature.tracking.viewmodel.CreateVoucherAction.Submit)
        composeRule.waitForIdle()
        capture("create_voucher_submit_error")
    }

    @Test
    fun logMilesStep1Screen() {
        composeRule.setContent {
            MilewayTheme {
                LogMilesScreen(onNext = {}, onOpenHistory = {})
            }
        }
        capture("log_miles_step1_screen")
    }

    @Test
    fun logMilesStep2Screen() {
        composeRule.setContent {
            MilewayTheme {
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
            // LogMilesHistoryScreen's root Column has no background (only its header self-paints a
            // gradient) — see ThemedBackground's doc.
            ThemedBackground {
                LogMilesHistoryScreen(onBack = {}, onOpenDraft = {})
            }
        }
        capture("log_miles_history_screen")
    }

    @Test
    fun spendsHomeScreen() {
        composeRule.setContent {
            MilewayTheme {
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

    // V27 P27.E.1: the old 2-route entry(category)/details(amount+submit) pair is now one
    // in-place 2-step ExpenseScreen wizard (ExpenseFormState.step drives which step renders).
    // Golden filenames kept as-is (expense_entry_screen / expense_details_input_screen) — same
    // wizard, same instance, step 2 reached here by driving the same UI a real user would.
    @Test
    fun expenseEntryScreen() {
        composeRule.setContent {
            MilewayTheme {
                ExpenseScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("expense_entry_screen")
    }

    @Test
    fun expenseDetailsInputScreen() {
        composeRule.setContent {
            MilewayTheme {
                ExpenseScreen(onBack = {}, onSubmitted = {})
            }
        }
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithText("Next").performClick()
        capture("expense_details_input_screen")
    }

    @Test
    fun expenseHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                ExpenseHistoryScreen(onBack = {}, onOpenDetail = {})
            }
        }
        capture("expense_history_screen")
    }

    @Test
    fun expenseDetailScreen() {
        composeRule.setContent {
            MilewayTheme {
                ExpenseDetailScreen(expenseId = "EXP-002", onBack = {})
            }
        }
        capture("expense_detail_screen")
    }

    // ROUND 2: not-found. logging-tail's real fix — previously an unmatched expenseId rendered
    // only a blank body with no Scaffold/top bar/back button at all; now a proper EmptyState with
    // the back arrow preserved.
    @Test
    fun expenseDetailScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                ExpenseDetailScreen(expenseId = "EXP-DOES-NOT-EXIST", onBack = {})
            }
        }
        capture("expense_detail_screen_not_found")
    }

    @Test
    fun voucherHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                VoucherHistoryScreen(onBack = {})
            }
        }
        capture("voucher_history_screen")
    }

    // ── Travel ───────────────────────────────────────────────────────────────────

    @Test
    fun travelHomeScreen() {
        composeRule.setContent {
            MilewayTheme {
                TravelHomeScreen()
            }
        }
        capture("travel_home_screen")
    }

    @Test
    fun createTripScreen() {
        composeRule.setContent {
            MilewayTheme {
                CreateTripScreen(onBack = {}, onSubmitted = { _ -> })
            }
        }
        capture("create_trip_screen")
    }

    @Test
    fun createMjpScreen() {
        composeRule.setContent {
            MilewayTheme {
                CreateMjpScreen(onBack = {}, onSubmitted = { _ -> })
            }
        }
        capture("create_mjp_screen")
    }

    @Test
    fun tripHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                TripHistoryScreen(onBack = {})
            }
        }
        capture("trip_history_screen")
    }

    @Test
    fun bookingHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                BookingHistoryScreen(onBack = {})
            }
        }
        capture("booking_history_screen")
    }

    // ── Approvals & Payables ───────────────────────────────────────────────────────

    @Test
    fun advanceHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                AdvanceHistoryScreen(onBack = {}, onRequestAdvance = {})
            }
        }
        capture("advance_history_screen")
    }

    @Test
    fun askAdvanceFormStep1Screen() {
        composeRule.setContent {
            MilewayTheme {
                AskAdvanceFormScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("ask_advance_form_step1_screen")
    }

    @Test
    fun payablesHomeScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
                PurchaseRequestDetailsScreen(
                    poId = "PO-2024-001",
                    onBack = {},
                )
            }
        }
        capture("purchase_request_details_screen")
    }

    // ROUND 2: not-found. other-features's fix — the not-found message previously had no scaffold
    // chrome (only the system back gesture worked); now wrapped in TransactionDetailScaffold so
    // back is always available, matching the loaded state.
    @Test
    fun purchaseRequestDetailsScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                PurchaseRequestDetailsScreen(poId = "PO-2024-999", onBack = {})
            }
        }
        capture("purchase_request_details_screen_not_found")
    }

    @Test
    fun createInvoiceScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            // Pin "now". This screen renders relative timestamps ("5 hours ago") against fixed demo
            // data, so on the real clock the text — and therefore the PNG's byte size — drifted with
            // wall-clock time. That is why this baseline re-recorded on nearly every CI run and kept
            // the screenshot bot opening fresh refresh PRs. See LocalNowMs's doc.
            CompositionLocalProvider(LocalNowMs provides { screenshotNowMs }) {
                MilewayTheme {
                    ApprovalsScreen(onOpenDetail = {})
                }
            }
        }
        capture("approvals_screen_pending_tab")
        // FILLED variant (UI-realism audit, approvals-content): same real render — the pending
        // queue's 4 items (4 distinct claimants, mixed types, ₹150–8,400, ages 20m–7d) — under the
        // standardized <screen>_filled.png name so it pairs with approvals_screen_empty below.
        capture("approvals_screen_filled")
    }

    // EMPTY variant (UI-realism audit, approvals-content) paired with approvals_screen_filled
    // above: the pending queue's own "SAVED" filter chip, toggled with nothing saved yet — the
    // screen's real, already-wired EMPTY state (distinct copy from the "all caught up" case, see
    // ApprovalsScreen.kt's emptyTitle `when`), not a fabricated one.
    @Test
    fun approvalsScreenEmpty() {
        composeRule.setContent {
            CompositionLocalProvider(LocalNowMs provides { screenshotNowMs }) {
                MilewayTheme {
                    ApprovalsScreen(onOpenDetail = {})
                }
            }
        }
        composeRule.onNodeWithText("Saved").performClick()
        composeRule.waitForIdle()
        capture("approvals_screen_empty")
    }

    @Test
    fun approvalDetailsScreenViolation() {
        composeRule.setContent {
            MilewayTheme {
                ApprovalDetailsScreen(approvalId = "A003", onBack = {})
            }
        }
        capture("approval_details_screen_violation")
        // FILLED variant (UI-realism audit, approvals-content): same real render — A003's full
        // requester/amount/violation detail — under the standardized name, paired with
        // approval_details_screen_empty below.
        capture("approval_details_screen_filled")
    }

    // ROUND 2: not-found/error. other-features's fix — a stale/deleted approvalId previously
    // blanked the entire screen (no top bar); ApprovalsViewModel.openDetail now sets
    // ScreenState.Error and the screen renders it inside the same scaffold, back button intact.
    @Test
    fun approvalDetailsScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                ApprovalDetailsScreen(approvalId = "A-DOES-NOT-EXIST", onBack = {})
            }
        }
        capture("approval_details_screen_not_found")
        // EMPTY variant (UI-realism audit, approvals-content): a stale/deleted approvalId is this
        // screen's real "nothing here" shape — paired with approval_details_screen_filled above.
        capture("approval_details_screen_empty")
    }

    // FILLED/EMPTY pair (UI-realism audit, approvals-content): ClaimantHistorySheet is new — a
    // manager's "who is this person, should I trust this claim" lookup opened from the requester
    // card. It's a stateless composable (items flow in as a param), so both states are driven
    // directly rather than through ApprovalsRepository.all, which today has no requester with more
    // than one entry — real wiring's "history" is honest but currently always empty; this
    // illustrates what the sheet looks like once a claimant has a real track record.
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun claimantHistorySheetFilled() {
        composeRule.setContent {
            MilewayTheme {
                ClaimantHistorySheet(
                    requesterName = "Priya Sharma",
                    items =
                        listOf(
                            ApprovalItem("A001", ApprovalType.MILEAGE, "Priya Sharma", "Client visit – 48 km trip", 576.0, ApprovalStatus.PENDING, 1_781_654_400_000L - 3_600_000L),
                            ApprovalItem("A101", ApprovalType.EXPENSE, "Priya Sharma", "Business dinner – ₹3,200", 3200.0, ApprovalStatus.APPROVED, 1_781_654_400_000L - 86_400_000L),
                            ApprovalItem("A102", ApprovalType.TRAVEL, "Priya Sharma", "Pune–Mumbai cab", 1450.0, ApprovalStatus.REJECTED, 1_781_654_400_000L - 5 * 86_400_000L),
                            ApprovalItem(
                                "A103", ApprovalType.EXPENSE, "Priya Sharma", "Client gift – ₹1,800", 1800.0, ApprovalStatus.APPROVED,
                                1_781_654_400_000L - 9 * 86_400_000L, policyViolation = true,
                            ),
                        ),
                    onDismiss = {},
                )
            }
        }
        capture("claimant_history_sheet_filled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun claimantHistorySheetEmpty() {
        composeRule.setContent {
            MilewayTheme {
                ClaimantHistorySheet(requesterName = "Karan Chopra", items = emptyList(), onDismiss = {})
            }
        }
        capture("claimant_history_sheet_empty")
    }

    // ── Payments & Events ──────────────────────────────────────────────────────────

    @Test
    fun createPaymentScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
                QrHomeScreen(onBack = {})
            }
        }
        capture("qr_home_screen")
    }

    @Test
    fun cardsHomeScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
                ProfileDetailsScreen(onBack = {})
            }
        }
        capture("profile_details_screen")
    }

    @Test
    fun preferencesScreen() {
        composeRule.setContent {
            MilewayTheme {
                PreferencesScreen(onBack = {})
            }
        }
        capture("preferences_screen")
    }

    @Test
    fun settingsScreen() {
        composeRule.setContent {
            MilewayTheme {
                SettingsScreen(onBack = {}, onOpenDebugMenu = {})
            }
        }
        capture("settings_screen")
    }

    @Test
    fun analyticsHomeScreen() {
        composeRule.setContent {
            MilewayTheme {
                AnalyticsHomeScreen(onBack = {}, onOpenDetail = { _ -> })
            }
        }
        capture("analytics_home_screen")
    }

    @Test
    fun analyticsDetailMileageScreen() {
        composeRule.setContent {
            MilewayTheme {
                AnalyticsDetailScreen(category = "Mileage", onBack = {})
            }
        }
        capture("analytics_detail_mileage_screen")
    }

    @Test
    fun delegationScreen() {
        composeRule.setContent {
            MilewayTheme {
                DelegationScreen(onBack = {})
            }
        }
        capture("delegation_screen")
    }

    @Test
    fun activeSessionsScreen() {
        composeRule.setContent {
            MilewayTheme {
                ActiveSessionsScreen(onBack = {})
            }
        }
        capture("active_sessions_screen")
    }

    @Test
    fun helpSupportScreen() {
        composeRule.setContent {
            MilewayTheme {
                HelpScreen(onBack = {}, onOpenMyTickets = {})
            }
        }
        capture("help_support_screen")
    }

    @Test
    fun myTicketsScreen() {
        composeRule.setContent {
            MilewayTheme {
                MyTicketsScreen(onBack = {})
            }
        }
        capture("my_tickets_screen")
    }

    @Test
    fun notificationCentreScreen() {
        composeRule.setContent {
            MilewayTheme {
                NotificationCentreScreen(onBack = {})
            }
        }
        capture("notification_centre_screen")
    }

    @Test
    fun connectedAccountsScreen() {
        composeRule.setContent {
            MilewayTheme {
                ConnectedAccountsScreen(onBack = {})
            }
        }
        capture("connected_accounts_screen")
    }

    // ── Media ──────────────────────────────────────────────────────────────────────

    @Test
    fun mediaAttachmentSelectionScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
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
            // The permission-required fallback has no background of its own — see
            // ThemedBackground's doc.
            ThemedBackground {
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
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
                AssistantHomeSheet(onDismiss = {})
            }
        }
        capture("assistant_home_sheet")
    }

    @Test
    fun agentChatScreenAnalyticsTab() {
        composeRule.setContent {
            MilewayTheme {
                AgentChatScreen(
                    onBack = {},
                    onOpenHistory = {},
                )
            }
        }
        composeRule.onNodeWithText("[ POPULAR ]").performClick()
        capture("agent_chat_analytics_popular")
    }

    @Test
    fun agentChatScreenUnansweredTab() {
        composeRule.setContent {
            MilewayTheme {
                AgentChatScreen(
                    onBack = {},
                    onOpenHistory = {},
                )
            }
        }
        composeRule.onNodeWithText("[ UNANSWERED ]").performClick()
        capture("agent_chat_analytics_unanswered")
    }

    @Test
    fun voiceWaveformIdle() {
        composeRule.setContent {
            MilewayTheme {
                VoiceWaveformOverlay(
                    state = WaveformState.Idle,
                    rms = 0f,
                    transcript = "",
                )
            }
        }
        capture("voice_waveform_idle")
    }

    @Test
    fun voiceWaveformListening() {
        composeRule.setContent {
            MilewayTheme {
                VoiceWaveformOverlay(
                    state = WaveformState.Listening,
                    rms = 0.6f,
                    transcript = "how much did I travel this week",
                )
            }
        }
        capture("voice_waveform_listening")
    }

    @Test
    fun voiceWaveformSpeaking() {
        composeRule.setContent {
            MilewayTheme {
                VoiceWaveformOverlay(
                    state = WaveformState.Speaking,
                    rms = 0.4f,
                    transcript = "",
                )
            }
        }
        capture("voice_waveform_speaking")
    }

    @Test
    fun chatAgentIndicatorFull() {
        composeRule.setContent {
            MilewayTheme {
                ChatAgentIndicator(
                    mode = ChatIndicatorMode.FULL,
                    onClick = {},
                )
            }
        }
        capture("chat_agent_indicator_full")
    }

    @Test
    fun chatAgentIndicatorCompact() {
        composeRule.setContent {
            MilewayTheme {
                ChatAgentIndicator(
                    mode = ChatIndicatorMode.COMPACT,
                    onClick = {},
                )
            }
        }
        capture("chat_agent_indicator_compact")
    }

    @Test
    fun assistantFab() {
        composeRule.setContent {
            MilewayTheme {
                AssistantFab(onOpen = {}, onDismissToTopbar = {})
            }
        }
        capture("assistant_fab")
    }

    // ── Security ───────────────────────────────────────────────────────────────────

    @Test
    fun debugMenuScreen() {
        composeRule.setContent {
            MilewayTheme {
                DebugMenuScreen(onBack = {}, heapUsedMb = 128L, heapTotalMb = 512L)
            }
        }
        capture("debug_menu_screen")
    }

    @Test
    fun demoSettingsScreen() {
        composeRule.setContent {
            MilewayTheme {
                DemoSettingsScreen(onBack = {}, onOpenRootGuard = {}, onOpenRootGuardDetected = {})
            }
        }
        capture("demo_settings_screen")
    }

    // ── Home ───────────────────────────────────────────────────────────────────────

    @Test
    fun homeScreenLoaded() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
                LoginScreen(onSignInWithCredentials = {}, onContinueAsGuest = {})
            }
        }
        capture("login_screen")
    }

    @Test
    fun splashScreen() {
        composeRule.setContent {
            MilewayTheme {
                SplashScreen(onFinished = {})
            }
        }
        capture("splash_screen")
    }

    @Test
    fun setPinScreen() {
        composeRule.setContent {
            MilewayTheme {
                com.mileway.ui.auth.SetPinScreen(onCompleted = {}, onSkip = {})
            }
        }
        capture("set_pin_screen")
    }

    @Test
    fun checkPinScreen() {
        composeRule.setContent {
            MilewayTheme {
                com.mileway.ui.auth.CheckPinScreen(onUnlocked = {})
            }
        }
        capture("check_pin_screen")
    }

    @Test
    fun shellPlaceholderScreen() {
        composeRule.setContent {
            // ShellPlaceholderScreen's root Column has no background of its own — see
            // ThemedBackground's doc. (Also currently unreferenced by any real navigation graph:
            // grep -rn "ShellPlaceholderScreen(" turns up only this test — dead code, not a
            // runtime-reachable screen today.)
            ThemedBackground {
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
            MilewayTheme {
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
            MilewayTheme {
                RootGuardScreen(onContinue = {}, signals = emptyList())
            }
        }
        capture("root_guard_screen_clean")
    }

    // ── V24 Super-Profile: plugins, membership, growth, verification, account ────────

    @Test
    fun pluginManagerScreen() {
        composeRule.setContent {
            MilewayTheme {
                PluginManagerScreen(onBack = {})
            }
        }
        capture("plugin_manager_screen")
    }

    @Test
    fun clubBenefitsScreen() {
        composeRule.setContent {
            MilewayTheme {
                ClubBenefitsScreen(onBack = {})
            }
        }
        capture("club_benefits_screen")
    }

    @Test
    fun subscriptionPlansScreen() {
        composeRule.setContent {
            MilewayTheme {
                PlansScreen(onBack = {}, onOpenManage = {})
            }
        }
        capture("subscription_plans_screen")
    }

    @Test
    fun mySubscriptionScreen() {
        composeRule.setContent {
            MilewayTheme {
                MySubscriptionScreen(onBack = {}, onOpenPlans = {})
            }
        }
        capture("my_subscription_screen")
    }

    @Test
    fun incentiveProgramsScreen() {
        composeRule.setContent {
            MilewayTheme {
                IncentiveProgramsScreen(onBack = {})
            }
        }
        capture("incentive_programs_screen")
    }

    @Test
    fun verificationCentreScreen() {
        composeRule.setContent {
            MilewayTheme {
                VerificationCentreScreen(onBack = {}, onOpenDocument = {})
            }
        }
        capture("verification_centre_screen")
    }

    @Test
    fun verificationDocumentScreen() {
        composeRule.setContent {
            MilewayTheme {
                DocumentDetailScreen(docType = "driving_license", onBack = {})
            }
        }
        capture("verification_document_screen")
    }

    // ROUND 2: EMPTY/not-found. profile-tail's fix — an unmatched docType (stale deep link, or the
    // seedIfEmpty()/observeAll() async gap landing exactly here) previously rendered only the top
    // bar with a blank title; now a DefaultEmptyState names what happened.
    @Test
    fun verificationDocumentScreenNotFound() {
        composeRule.setContent {
            MilewayTheme {
                DocumentDetailScreen(docType = "does_not_exist", onBack = {})
            }
        }
        capture("verification_document_screen_not_found")
    }

    @Test
    fun referralHubScreen() {
        composeRule.setContent {
            MilewayTheme {
                ReferralHubScreen(onBack = {})
            }
        }
        capture("referral_hub_screen")
    }

    @Test
    fun couponsScreen() {
        composeRule.setContent {
            MilewayTheme {
                CouponsScreen(onBack = {})
            }
        }
        capture("coupons_screen")
    }

    @Test
    fun rewardsScreen() {
        composeRule.setContent {
            MilewayTheme {
                RewardsScreen(onBack = {})
            }
        }
        capture("rewards_screen")
    }

    @Test
    fun marketingHubScreen() {
        composeRule.setContent {
            MilewayTheme {
                MarketingHubScreen(onBack = {})
            }
        }
        capture("marketing_hub_screen")
    }

    @Test
    fun accountDeletionScreen() {
        composeRule.setContent {
            MilewayTheme {
                AccountDeletionScreen(onBack = {}, onAccountDeleted = {})
            }
        }
        capture("account_deletion_screen")
    }

    @Test
    fun managerReporteesScreen() {
        composeRule.setContent {
            MilewayTheme {
                ManagerReporteesScreen(onBack = {}, onOpenReportee = {})
            }
        }
        capture("manager_reportees_screen")
    }

    @Test
    fun savedPlacesScreen() {
        composeRule.setContent {
            MilewayTheme {
                SavedPlacesScreen(onBack = {})
            }
        }
        capture("saved_places_screen")
    }

    @Test
    fun emergencyContactsScreen() {
        composeRule.setContent {
            MilewayTheme {
                EmergencyContactsScreen(onBack = {})
            }
        }
        capture("emergency_contacts_screen")
    }

    @Test
    fun orgChartScreen() {
        composeRule.setContent {
            MilewayTheme {
                OrgChartScreen(onBack = {})
            }
        }
        capture("org_chart_screen")
    }

    @Test
    fun signupOnboardingScreen() {
        composeRule.setContent {
            MilewayTheme {
                SignupOnboardingScreen(config = OnboardingFormConfig(), onComplete = {})
            }
        }
        capture("signup_onboarding_screen")
    }

    @Test
    fun whatsNewSheet() {
        composeRule.setContent {
            MilewayTheme {
                WhatsNewSheet(entries = WhatsNewCatalog.entries, onDismiss = {})
            }
        }
        capture("whats_new_sheet")
    }

    @Test
    fun liveDriveScreen() {
        composeRule.setContent {
            MilewayTheme {
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
        capture("live_drive_screen")
    }

    @Test
    fun trackEvidenceScreen() {
        composeRule.setContent {
            MilewayTheme {
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
        capture("track_evidence_screen")
    }

    // ── Tracking sheets & dialogs (bottom sheets are where the decisions live — see class doc) ──

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun journeyGuideSheet() {
        composeRule.setContent {
            MilewayTheme {
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
        capture("journey_guide_sheet")
    }

    @Test
    fun pauseReasonSheet() {
        composeRule.setContent {
            // PauseReasonSheet is a deliberately bare, stateless body (see PauseResumeSheets.kt's
            // file-header doc) with no background of its own — see ThemedBackground's doc for why
            // this needs the explicit wrapper. NOTE: at runtime TrackMilesScreen calls this
            // composable directly too (feature/tracking/.../screens/TrackMilesScreen.kt, the
            // TrackSheet.PAUSE branch) without ever wrapping it in a ModalBottomSheet, contradicting
            // that same file-header doc's contract ("the integrator wraps each in its own
            // ModalBottomSheet and owns the state"). That is a real product bug distinct from this
            // capture-harness gap — reported, not fixed here (out of this file's ownership).
            ThemedBackground {
                PauseReasonSheet(
                    timestamp = "2:45 PM",
                    selectedReason = null,
                    customReason = "",
                    onSelectReason = {},
                    onCustomReason = {},
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        capture("pause_reason_sheet")
    }

    // FILLED/EMPTY pair (UI-realism audit, tracking-content): the "Custom reason" field the fix
    // added a real label to. showCustomInput is forced true (rather than left to its
    // customReason.isNotEmpty() default) so the EMPTY variant can show the field's placeholder +
    // label with nothing typed yet, not just the collapsed "Add custom reason" button.
    @Test
    fun pauseReasonSheetFilled() {
        composeRule.setContent {
            ThemedBackground {
                PauseReasonSheet(
                    timestamp = "2:45 PM",
                    selectedReason = null,
                    customReason = "Waiting for the toll booth queue to clear near the flyover",
                    showCustomInput = true,
                    onSelectReason = {},
                    onCustomReason = {},
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        capture("pause_reason_sheet_filled")
    }

    @Test
    fun pauseReasonSheetEmpty() {
        composeRule.setContent {
            ThemedBackground {
                PauseReasonSheet(
                    timestamp = "2:45 PM",
                    selectedReason = null,
                    customReason = "",
                    showCustomInput = true,
                    onSelectReason = {},
                    onCustomReason = {},
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        capture("pause_reason_sheet_empty")
    }

    // FILLED/EMPTY pair (UI-realism audit, tracking-content): the "Resume notes" field the fix
    // added a real label to. ResumeTrackingSheet had no capture in this gallery at all before now.
    @Test
    fun resumeTrackingSheetFilled() {
        composeRule.setContent {
            ThemedBackground {
                ResumeTrackingSheet(
                    pauseReason = "traffic",
                    resumeNotes = "Traffic cleared near Hinjewadi Phase 1, resuming the route now.",
                    onNotesChange = {},
                    onResume = {},
                    onCancel = {},
                )
            }
        }
        capture("resume_tracking_sheet_filled")
    }

    @Test
    fun resumeTrackingSheetEmpty() {
        composeRule.setContent {
            ThemedBackground {
                ResumeTrackingSheet(
                    pauseReason = "traffic",
                    resumeNotes = "",
                    onNotesChange = {},
                    onResume = {},
                    onCancel = {},
                )
            }
        }
        capture("resume_tracking_sheet_empty")
    }

    @Test
    fun sessionRestoreSheet() {
        composeRule.setContent {
            // SessionRestoreSheet is a bare, stateless body with no background of its own — see
            // ThemedBackground's doc. NOTE: unlike PauseReasonSheet, this composable is currently
            // dead code — grep -rn "SessionRestoreSheet(" turns up only this test. Real production
            // session-restore uses the differently-named SessionRestoreBottomSheet
            // (feature/tracking/.../sheets/SessionRestoreBottomSheet.kt), which correctly hosts its
            // own ModalBottomSheet. So this capture documents a composable nothing actually renders.
            ThemedBackground {
                SessionRestoreSheet(
                    sessions =
                        listOf(
                            RestorableSession(token = "tok-8f21", label = "Server Session", timestamp = "2 hours ago"),
                            RestorableSession(token = "tok-a904", label = "Draft", timestamp = "yesterday"),
                        ),
                    onRestore = {},
                    onDiscard = {},
                    onIgnore = {},
                )
            }
        }
        capture("session_restore_sheet")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun vehiclePickerSheet() {
        composeRule.setContent {
            MilewayTheme {
                VehiclePickerSheet(
                    vehicles =
                        listOf(
                            VehicleOption(key = "twoWheeler", name = "Honda Activa", ratePerKm = 3.5),
                            VehicleOption(key = "fourWheelerPetrol", name = "Honda City", ratePerKm = 10.0),
                        ),
                    query = "",
                    onQueryChange = {},
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        capture("vehicle_picker_sheet")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun vendorPickerSheet() {
        composeRule.setContent {
            MilewayTheme {
                VendorPickerSheet(
                    centers =
                        listOf(
                            CenterOption(id = "c1", name = "Hinjewadi IT Park", address = "Hinjewadi Phase 1, Pune"),
                            CenterOption(id = "c2", name = "FC Road Branch", address = null),
                        ),
                    query = "",
                    onQueryChange = {},
                    onSelect = {},
                    onOpenMaps = {},
                    onDismiss = {},
                )
            }
        }
        capture("vendor_picker_sheet")
    }

    @Test
    fun strangerSessionSheet() {
        composeRule.setContent {
            MilewayTheme {
                StrangerSessionSheet(
                    config = StrangerSessionConfig(routeId = "route-j1", ownerLabel = "Rahul Sharma"),
                    onResume = {},
                    onDismiss = {},
                )
            }
        }
        capture("stranger_session_sheet")
    }

    @Test
    fun sosBottomSheet() {
        composeRule.setContent {
            MilewayTheme {
                SosBottomSheet(onDismiss = {})
            }
        }
        capture("sos_bottom_sheet")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smartDistanceSheet() {
        composeRule.setContent {
            MilewayTheme {
                SmartDistanceSheet(
                    trackedKm = 12.0,
                    odometerKm = 19.0,
                    verified = false,
                    explanation = "",
                    onVerifiedChange = {},
                    onExplanationChange = {},
                    onStop = {},
                    onContinue = {},
                    onDismiss = {},
                )
            }
        }
        capture("smart_distance_sheet")
    }

    // FILLED/EMPTY pair (UI-realism audit, tracking-content): the "Explanation" field the fix
    // added a real label to. That field only renders once `verified = true`, so both variants
    // force that (the plain smart_distance_sheet test above documents the pre-verification state,
    // where the field isn't shown at all).
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smartDistanceSheetFilled() {
        composeRule.setContent {
            MilewayTheme {
                SmartDistanceSheet(
                    trackedKm = 12.0,
                    odometerKm = 19.0,
                    verified = true,
                    explanation = "GPS lost signal near the flyover, drove around back roads before it locked back on.",
                    onVerifiedChange = {},
                    onExplanationChange = {},
                    onStop = {},
                    onContinue = {},
                    onDismiss = {},
                )
            }
        }
        capture("smart_distance_sheet_filled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun smartDistanceSheetEmpty() {
        composeRule.setContent {
            MilewayTheme {
                SmartDistanceSheet(
                    trackedKm = 12.0,
                    odometerKm = 19.0,
                    verified = true,
                    explanation = "",
                    onVerifiedChange = {},
                    onExplanationChange = {},
                    onStop = {},
                    onContinue = {},
                    onDismiss = {},
                )
            }
        }
        capture("smart_distance_sheet_empty")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun submitConfirmSheet() {
        composeRule.setContent {
            MilewayTheme {
                SubmitConfirmSheet(onConfirm = {}, onCancel = {}, onDismiss = {})
            }
        }
        capture("submit_confirm_sheet")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun policyViolationSheet() {
        composeRule.setContent {
            MilewayTheme {
                PolicyViolationSheet(
                    violations =
                        listOf(
                            PolicyViolation(
                                id = "v1",
                                title = "Distance exceeds policy limit",
                                message = "This trip exceeds the 50 km daily limit by 12 km.",
                            ),
                        ),
                    askAuthoritiesSelected = false,
                    note = "",
                    onToggleAskAuthorities = {},
                    onNoteChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }
        capture("policy_violation_sheet")
    }

    // FILLED/EMPTY pair (UI-realism audit, tracking-content): the "Note for reviewer" field the
    // fix added a real label + required-hint to. That field only renders once
    // `askAuthoritiesSelected = true`, so both variants force that (the plain policy_violation_sheet
    // test above documents the pre-selection state, where the field isn't shown at all).
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun policyViolationSheetFilled() {
        composeRule.setContent {
            MilewayTheme {
                PolicyViolationSheet(
                    violations =
                        listOf(
                            PolicyViolation(
                                id = "v1",
                                title = "Distance exceeds policy limit",
                                message = "This trip exceeds the 50 km daily limit by 12 km.",
                            ),
                        ),
                    askAuthoritiesSelected = true,
                    note = "Submitted a written explanation to HR; awaiting sign-off from the finance desk.",
                    onToggleAskAuthorities = {},
                    onNoteChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }
        capture("policy_violation_sheet_filled")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun policyViolationSheetEmpty() {
        composeRule.setContent {
            MilewayTheme {
                PolicyViolationSheet(
                    violations =
                        listOf(
                            PolicyViolation(
                                id = "v1",
                                title = "Distance exceeds policy limit",
                                message = "This trip exceeds the 50 km daily limit by 12 km.",
                            ),
                        ),
                    askAuthoritiesSelected = true,
                    note = "",
                    onToggleAskAuthorities = {},
                    onNoteChange = {},
                    onSubmit = {},
                    onDismiss = {},
                )
            }
        }
        capture("policy_violation_sheet_empty")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun officePickerSheet() {
        composeRule.setContent {
            MilewayTheme {
                OfficePickerSheet(
                    offices =
                        listOf(
                            Office(code = "PUN01", name = "Pune Hinjewadi Office", address = "Hinjewadi Phase 1, Pune", gstin = "27ABCDE1234F1Z5"),
                            Office(code = "MUM01", name = "Mumbai BKC Office", address = "Bandra Kurla Complex, Mumbai", gstin = "27ABCDE1234F1Z6"),
                        ),
                    query = "",
                    onQueryChange = {},
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        capture("office_picker_sheet")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun entityPickerSheet() {
        composeRule.setContent {
            MilewayTheme {
                EntityPickerSheet(
                    entities =
                        listOf(
                            BusinessEntity(id = 1L, name = "Mileway Technologies Pvt Ltd", country = "India", currencySymbol = "₹"),
                            BusinessEntity(id = 2L, name = "Mileway Inc", country = "United States", currencySymbol = "$"),
                        ),
                    query = "",
                    onQueryChange = {},
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        capture("entity_picker_sheet")
    }

    @Test
    fun discardJourneyDialog() {
        composeRule.setContent {
            MilewayTheme {
                DiscardJourneyDialog(onConfirm = {}, onDismiss = {}, isTracking = true)
            }
        }
        capture("discard_journey_dialog")
    }

    @Test
    fun exportOptionsDialog() {
        composeRule.setContent {
            MilewayTheme {
                ExportOptionsDialog(
                    onDismiss = {},
                    onExport = { _, _ -> },
                    trackName = "Pune → Hinjewadi",
                )
            }
        }
        capture("export_options_dialog")
    }

    @Test
    fun permissionOnboardingSheet() {
        composeRule.setContent {
            MilewayTheme {
                PermissionOnboardingSheet(
                    tier = defaultPermissionTiers.first(),
                    oemHint = null,
                    onGrant = {},
                    onSkip = {},
                )
            }
        }
        capture("permission_onboarding_sheet")
    }

    @Test
    fun permissionPrimerSheet() {
        composeRule.setContent {
            MilewayTheme {
                val controller = remember { PermissionPrimerController(provider = mockk(relaxed = true)) }
                PermissionPrimerSheet(
                    controller = controller,
                    oemHint = null,
                    onOpenAppSettings = {},
                    onFinished = {},
                )
            }
        }
        capture("permission_primer_sheet")
    }

    // OdometerReadingConfirmSheet (feature:tracking androidMain) is SKIPPED: it eagerly runs real
    // ML Kit TextRecognition (rememberOdometerOcrService -> TextRecognition.getClient(...)) in a
    // LaunchedEffect, which needs a real on-device ML Kit runtime and hangs/throws under
    // Robolectric. There is no separately-exported stateless inner composable to render instead
    // (unlike the ViewModel-needing sheets), so per the "skip rather than fake" rule this one has
    // no capture. OdometerDiscrepancySheet/OdometerRejectionSheet below cover its two downstream
    // outcome sheets, which ARE stateless.

    @Test
    fun driveReviewSheet() {
        composeRule.setContent {
            MilewayTheme {
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
        capture("drive_review_sheet")
    }

    // ── core:ui shared sheets & dialogs ──────────────────────────────────────────────

    @Test
    fun actionConfirmationBottomSheet() {
        composeRule.setContent {
            MilewayTheme {
                ActionConfirmationBottomSheet(
                    title = "Approve this expense?",
                    description = "This will notify the requester and finalize the payment.",
                    onConfirm = {},
                    onDismiss = {},
                    tone = ActionConfirmationToneType.Success,
                    showRemarksField = true,
                )
            }
        }
        capture("action_confirmation_bottom_sheet")
    }

    @Test
    fun filterBottomSheet() {
        composeRule.setContent {
            MilewayTheme {
                FilterBottomSheet(
                    sections =
                        listOf(
                            FilterSection(
                                key = "status",
                                title = "Status",
                                icon = Icons.Filled.FilterList,
                                mode = FilterSelectionMode.MULTI,
                                options = listOf(FilterOption("pending", "Pending"), FilterOption("approved", "Approved")),
                            ),
                        ),
                    initialSelected = emptyMap(),
                    onApply = {},
                    onDismiss = {},
                )
            }
        }
        capture("filter_bottom_sheet")
    }

    @Test
    fun bugReportSheet() {
        composeRule.setContent {
            MilewayTheme {
                BugReportSheet(onDismiss = {}, screen = "TrackMilesScreen")
            }
        }
        capture("bug_report_sheet")
    }

    @Test
    fun odometerDiscrepancySheet() {
        composeRule.setContent {
            MilewayTheme {
                OdometerDiscrepancySheet(
                    userReading = 45210,
                    deviceReading = 45300,
                    aiReading = 45280,
                    onAccept = {},
                    onRetake = {},
                )
            }
        }
        capture("odometer_discrepancy_sheet")
    }

    @Test
    fun odometerRejectionSheet() {
        composeRule.setContent {
            MilewayTheme {
                OdometerRejectionSheet(
                    reason = "We couldn't get a reliable reading from any source.",
                    userReading = 45210,
                    onAccept = {},
                    onRetake = {},
                )
            }
        }
        capture("odometer_rejection_sheet")
    }

    @Test
    fun wheelDatePickerDialog() {
        composeRule.setContent {
            MilewayTheme {
                WheelDatePickerDialog(initialDateMillis = screenshotNowMs, onConfirm = {}, onDismiss = {})
            }
        }
        capture("wheel_date_picker_dialog")
    }

    @Test
    fun wheelTimePickerDialog() {
        composeRule.setContent {
            MilewayTheme {
                WheelTimePickerDialog(initialMinutes = 600, onConfirm = { _, _ -> }, onDismiss = {})
            }
        }
        capture("wheel_time_picker_dialog")
    }

    @Test
    fun criticalErrorDialog() {
        composeRule.setContent {
            MilewayTheme {
                CriticalErrorDialog(
                    title = "Something went wrong",
                    message = "Your local data appears to be corrupted. Retry or exit the app.",
                    onRetry = {},
                    onExit = {},
                )
            }
        }
        capture("critical_error_dialog")
    }

    @Test
    fun languageSelectionSheet() {
        composeRule.setContent {
            MilewayTheme {
                LanguageSelectionSheet(onDismiss = {})
            }
        }
        capture("language_selection_sheet")
    }

    @Test
    fun colorWheelDialog() {
        composeRule.setContent {
            MilewayTheme {
                ColorWheelDialog(
                    selectedColor = Color(0xFF00E676),
                    onColorSelected = { _, _ -> },
                    onDismiss = {},
                )
            }
        }
        capture("color_wheel_dialog")
    }

    @Test
    fun detailInfoBottomSheet() {
        composeRule.setContent {
            MilewayTheme {
                DetailInfoBottomSheet(
                    title = "Cafe Coffee Day",
                    subtitle = "Transaction details",
                    headerGradient = listOf(Color(0xFF6200EE), Color(0xFF9C27B0)),
                    headerIcon = Icons.Filled.Info,
                    onDismiss = {},
                ) {
                    DetailInfoCard(title = "Transaction") {
                        DetailInfoRow("Merchant", "Cafe Coffee Day")
                        DetailInfoRow("Amount", "₹450.00")
                    }
                }
            }
        }
        capture("detail_info_bottom_sheet")
    }

    // ── Profile: additional uncovered screens ────────────────────────────────────────

    @Test
    fun advanceRequestDetailsScreen() {
        composeRule.setContent {
            MilewayTheme {
                AdvanceRequestDetailsScreen(advanceId = "ADV-001", onBack = {})
            }
        }
        capture("advance_request_details_screen")
    }

    @Test
    fun ecoDashboardScreen() {
        composeRule.setContent {
            MilewayTheme {
                EcoDashboardScreen(onBack = {})
            }
        }
        capture("eco_dashboard_screen")
    }

    @Test
    fun favouriteRoutesScreen() {
        composeRule.setContent {
            MilewayTheme {
                FavouriteRoutesScreen(onBack = {})
            }
        }
        capture("favourite_routes_screen")
    }

    @Test
    fun offersHubScreen() {
        composeRule.setContent {
            MilewayTheme {
                OffersHubScreen(onBack = {})
            }
        }
        capture("offers_hub_screen")
    }

    @Test
    fun selfAuditScreen() {
        composeRule.setContent {
            MilewayTheme {
                SelfAuditScreen(vehicleId = "veh_seed_1", onBack = {})
            }
        }
        capture("self_audit_screen")
    }

    @Test
    fun storageManagementScreen() {
        composeRule.setContent {
            MilewayTheme {
                StorageManagementScreen(onBack = {})
            }
        }
        // Unlike every other screen here, StorageManagementViewModel.refresh() hops to a real
        // Dispatchers.IO thread (StorageRepository scans the actual cacheDir/db file) rather than
        // collecting an already-emitted fake Flow — composeRule's implicit idle-wait doesn't cover
        // that hop, so the capture raced ahead of it and recorded the pre-load empty list. Explicit
        // waitForIdle() drains the main-looper post-back once the (near-instant) scan completes.
        composeRule.waitForIdle()
        capture("storage_management_screen")
    }

    @Test
    fun supportChatScreen() {
        composeRule.setContent {
            MilewayTheme {
                SupportChatScreen(onBack = {})
            }
        }
        capture("support_chat_screen")
    }

    @Test
    fun supportHubScreen() {
        composeRule.setContent {
            MilewayTheme {
                SupportHubScreen(onBack = {}, onOpenFaq = {}, onOpenTickets = {}, onOpenChat = {}, onOpenTour = {})
            }
        }
        capture("support_hub_screen")
    }

    @Test
    fun trainingTourScreen() {
        composeRule.setContent {
            MilewayTheme {
                TrainingTourScreen(onBack = {})
            }
        }
        capture("training_tour_screen")
    }

    @Test
    fun vehicleGarageScreen() {
        composeRule.setContent {
            MilewayTheme {
                VehicleGarageScreen(onBack = {})
            }
        }
        capture("vehicle_garage_screen")
    }

    // ── Logging: additional uncovered screens ────────────────────────────────────────

    @Test
    fun cardsTxnHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                CardsTxnHistoryScreen(onBack = {})
            }
        }
        capture("cards_txn_history_screen")
    }

    @Test
    fun settlementHistoryScreen() {
        composeRule.setContent {
            MilewayTheme {
                SettlementHistoryScreen(onBack = {})
            }
        }
        capture("settlement_history_screen")
    }

    @Test
    fun voucherDetailsScreen() {
        // A throwaway FakeVoucherDao seeded with one row, passed directly as the VM's dependency
        // (bypassing Koin's shared `voucherDao` single) so this doesn't mutate the state the
        // voucherHistoryScreen/createVoucherSelectExpenses tests above also read from.
        val demoDao =
            FakeVoucherDao().apply {
                kotlinx.coroutines.runBlocking {
                    insert(
                        VoucherEntity(
                            voucherNumber = "V-DEMO-1",
                            title = "Mileage claim — Pune → Hinjewadi",
                            category = VoucherCategory.MILEAGE,
                            totalAmount = 185.60,
                            notes = "Client site visit",
                            expenseRouteIdsJson = VoucherEntity.encodeExpenseRouteIds(listOf("route-j1")),
                            status = "PENDING",
                            createdAtMs = 1_700_000_000_000L,
                        ),
                    )
                }
            }
        composeRule.setContent {
            MilewayTheme {
                VoucherDetailsScreen(
                    voucherNumber = "V-DEMO-1",
                    onBack = {},
                    viewModel = VoucherDetailsViewModel(demoDao),
                )
            }
        }
        capture("voucher_details_screen")
    }

    // D2 FIX (2026-08-09): this passed File(screenshotsDir, "$name.png").absolutePath — an ABSOLUTE
    // path computed by walking up to the repo root. Roborazzi's compare/verify pipeline keys off its
    // own configured roborazzi.output.dir ("../docs/screenshots", set in roborazzi.properties), so an
    // absolute path handed straight to captureRoboImage bypassed compare entirely and always wrote.
    // That is why this surface still passed with a deliberately corrupted baseline even after verify
    // was switched on. A relative name lets roborazzi own the location, and therefore the comparison.
    //
    // D3 FIX (2026-08-09): that reasoning assumed roborazzi.output.dir actually resolves to
    // docs/screenshots for this class's forked task — per setup()'s D3 FIX doc, it does not (confirmed
    // by testing: a relative name landed PNGs in the `app/` module root instead). Verify mode's
    // relative-name path is left as-is above (harmless: it's already a no-op today, see setup()'s D3
    // FIX, and stays correct-by-construction if the underlying Gradle wiring is ever fixed). Only the
    // explicit, deliberately-requested record path below needs to reach the real directory today.
    // Some screen/sheet bodies (PauseReasonSheet, SessionRestoreSheet, RouteReplayScreen,
    // LogMilesHistoryScreen, ShellPlaceholderScreen, CameraCaptureScreen's permission state, …)
    // are deliberately self-contained with no Scaffold/Surface of their own — MilewayTheme itself
    // never paints a background either (it only provides MaterialTheme, see its KDoc). In the real
    // app every one of these always renders nested inside MilewayAppRoot's or MilewayApp's outer
    // Scaffold, whose default containerColor (MaterialTheme.colorScheme.background) paints the
    // dark surface behind them for free. This gallery mounts each capture bare under MilewayTheme
    // with no such ancestor, so a composable that relies on it falls through to Robolectric's
    // undecorated (white) root canvas and MaterialTheme-colored text renders washed out/illegible
    // — a capture-harness gap, not a runtime bug (confirmed by reading the real nesting in
    // MilewayAppRoot.kt / MilewayApp.kt). Reproduces that one ambient Scaffold layer explicitly;
    // same fix already applied ad hoc for trackEvidenceTripNotFoundScreen above.
    @Composable
    private fun ThemedBackground(content: @Composable () -> Unit) {
        MilewayTheme {
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
}
