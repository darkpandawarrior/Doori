package com.mileway

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.mileway.core.data.dao.AgentDao
import com.mileway.core.data.dao.ConnectedAccountDao
import com.mileway.core.data.dao.DelegationDao
import com.mileway.core.data.dao.DraftExpenseDao
import com.mileway.core.data.dao.HardwareEventDao
import com.mileway.core.data.settings.AgentSessionStore
import com.mileway.feature.agent.analytics.AgentAnalyticsStore
import com.mileway.feature.agent.engine.AssistantEngine
import com.mileway.feature.agent.voice.SpeechToText
import com.mileway.feature.agent.voice.TextToSpeech
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
import com.mileway.core.data.settings.DemoSettingsRepository
import com.mileway.core.maps.MapSurface
import com.siddharth.kmp.common.CrashReporter
import com.mileway.core.platform.ReferralData
import com.mileway.core.platform.ReferralManager
import com.mileway.core.platform.ShareSheet
import com.mileway.core.platform.UrlOpener
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.feature.agent.di.agentModule
import com.siddharth.kmp.appshell.AnalyticsHelper
import com.siddharth.kmp.appshell.AppReviewManagerFactory
import com.siddharth.kmp.appshell.AppUpdateManagerFactory
import com.siddharth.kmp.appshell.LoggingAnalyticsHelper
import com.siddharth.kmp.appshell.NotificationScheduler
import com.siddharth.kmp.appshell.PermissionsProvider
import com.mileway.feature.agent.ui.components.AssistantFab
import com.mileway.feature.agent.ui.components.ChatAgentIndicator
import com.mileway.feature.agent.ui.components.ChatIndicatorMode
import com.mileway.feature.agent.ui.components.VoiceWaveformOverlay
import com.mileway.feature.agent.ui.components.WaveformState
import com.mileway.feature.agent.ui.screens.AgentChatScreen
import com.mileway.feature.agent.ui.screens.AgentHistoryScreen
import com.mileway.feature.approvals.di.approvalsModule
import com.mileway.feature.approvals.ui.screens.ApprovalDetailsScreen
import com.mileway.feature.approvals.ui.screens.ApprovalsScreen
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
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.logging.ui.screens.VoucherHistoryScreen
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
import com.mileway.feature.profile.ui.screens.AdvanceHistoryScreen
import com.mileway.feature.profile.ui.screens.AnalyticsDetailScreen
import com.mileway.feature.profile.ui.screens.AnalyticsHomeScreen
import com.mileway.feature.profile.ui.screens.AskAdvanceFormScreen
import com.mileway.feature.profile.ui.screens.AccountDeletionScreen
import com.mileway.feature.profile.ui.screens.ActiveSessionsScreen
import com.mileway.feature.profile.ui.screens.ClubBenefitsScreen
import com.mileway.feature.profile.ui.screens.ConnectedAccountsScreen
import com.mileway.feature.profile.ui.screens.CouponsScreen
import com.mileway.feature.profile.ui.screens.DelegationScreen
import com.mileway.feature.profile.ui.screens.DocumentDetailScreen
import com.mileway.feature.profile.ui.screens.EmergencyContactsScreen
import com.mileway.feature.profile.ui.screens.IncentiveProgramsScreen
import com.mileway.feature.profile.ui.screens.ManagerReporteesScreen
import com.mileway.feature.profile.ui.screens.MarketingHubScreen
import com.mileway.feature.profile.ui.screens.MySubscriptionScreen
import com.mileway.feature.profile.ui.screens.OrgChartScreen
import com.mileway.feature.profile.ui.screens.PlansScreen
import com.mileway.feature.profile.ui.screens.PluginManagerScreen
import com.mileway.feature.profile.ui.screens.ReferralHubScreen
import com.mileway.feature.profile.ui.screens.RewardsScreen
import com.mileway.feature.profile.ui.screens.SavedPlacesScreen
import com.mileway.feature.profile.ui.screens.VerificationCentreScreen
import com.mileway.feature.profile.ui.screens.DemoSettingsScreen
import com.mileway.feature.profile.ui.screens.HelpScreen
import com.mileway.feature.profile.ui.screens.MyTicketsScreen
import com.mileway.feature.profile.ui.screens.NotificationCentreScreen
import com.mileway.feature.profile.ui.screens.PreferencesScreen
import com.mileway.feature.profile.ui.screens.ProfileDetailsScreen
import com.mileway.feature.profile.ui.screens.ProfileScreen
import com.mileway.feature.profile.ui.screens.QrHomeScreen
import com.mileway.feature.profile.ui.screens.RootGuardScreen
import com.mileway.feature.profile.ui.screens.SettingsScreen
import com.mileway.feature.tracking.debug.DebugMenuScreen
import com.mileway.feature.tracking.di.trackingModule
import com.mileway.feature.tracking.ui.screens.CheckInHistoryItem
import com.mileway.feature.tracking.ui.screens.CheckInHistoryScreen
import com.mileway.feature.tracking.ui.screens.CreateVoucherScreen
import com.mileway.feature.tracking.ui.screens.GeoCheckInScreen
import com.mileway.feature.tracking.ui.screens.HardwareEventsLogScreen
import com.mileway.feature.tracking.ui.screens.LocationMapScreen
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
import com.mileway.feature.tracking.ui.screens.TrackSubmissionScreen
import com.mileway.feature.tracking.ui.screens.TrackingSuccessScreen
import com.mileway.feature.travel.di.travelModule
import com.mileway.feature.travel.ui.screens.BookingHistoryScreen
import com.mileway.feature.travel.ui.screens.CreateMjpScreen
import com.mileway.feature.travel.ui.screens.CreateTripScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.feature.travel.ui.screens.TripHistoryScreen
import com.mileway.stub.di.stubModule
import com.mileway.ui.AssistantHomeSheet
import com.mileway.ui.ShellPlaceholderScreen
import com.mileway.ui.auth.LoginScreen
import com.mileway.ui.auth.OnboardingFormConfig
import com.mileway.ui.auth.SignupOnboardingScreen
import com.mileway.ui.auth.SplashScreen
import com.mileway.ui.auth.authModule
import com.mileway.feature.whatsnew.data.WhatsNewCatalog
import com.mileway.feature.whatsnew.di.whatsNewFeatureModule
import com.mileway.ui.home.WhatsNewSheet
import com.mileway.ui.home.HomeScreenContent
import com.mileway.ui.home.HomeUiState
import com.mileway.ui.home.homeModule
import dev.tmapps.konnection.Konnection
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

    // LiveTrackScreen is intentionally omitted: it needs an active in-progress track
    // session (CurrentTrackDataStore must emit a track with locations); with the offline
    // fakes it renders a "Failed to load" state. track_miles_idle + tracking_success
    // already document the live-tracking flow.

    @Test
    fun locationMapScreen() {
        composeRule.setContent {
            MilewayTheme {
                LocationMapScreen(onNavigateBack = {})
            }
        }
        capture("location_map_screen")
    }

    @Test
    fun geoCheckInScreen() {
        composeRule.setContent {
            MilewayTheme {
                GeoCheckInScreen(onBack = {})
            }
        }
        capture("geo_check_in_screen")
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
    fun trackSubmissionScreen() {
        composeRule.setContent {
            MilewayTheme {
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
            MilewayTheme {
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
            MilewayTheme {
                ApprovalsScreen(onOpenDetail = {})
            }
        }
        capture("approvals_screen_pending_tab")
    }

    @Test
    fun approvalDetailsScreenViolation() {
        composeRule.setContent {
            MilewayTheme {
                ApprovalDetailsScreen(approvalId = "A003", onBack = {})
            }
        }
        capture("approval_details_screen_violation")
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
            MilewayTheme {
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
            MilewayTheme {
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

    private fun capture(name: String) =
        composeRule.onRoot().captureRoboImage(File(screenshotsDir, "$name.png").absolutePath)
}
