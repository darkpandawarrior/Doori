package com.mileway

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.mileway.core.ui.previews.PreviewHeroTrackingCardActive
import com.mileway.core.ui.previews.PreviewHeroTrackingCardActiveIon
import com.mileway.core.ui.previews.PreviewHeroTrackingCardIdle
import com.mileway.core.ui.previews.PreviewThemePickerAmoled
import com.mileway.core.ui.previews.PreviewThemePickerDaybreak
import com.mileway.core.ui.previews.PreviewThemePickerIon
import com.mileway.core.ui.previews.PreviewThemePickerMatrix
import com.mileway.core.ui.previews.PreviewTrackingStatusPills
import com.mileway.core.ui.previews.PreviewTrackingTopBarActive
import com.mileway.core.ui.previews.PreviewTrackingTopBarActiveDaybreak
import com.mileway.core.ui.previews.PreviewTrackingTopBarIdle
import com.mileway.core.ui.previews.PreviewTrackingTopBarPaused
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemApproved
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemPending
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemRejected
import com.mileway.feature.approvals.ui.previews.PreviewApprovalItemWithViolation
import com.mileway.feature.payables.ui.previews.PreviewPoCardApproved
import com.mileway.feature.payables.ui.previews.PreviewPoCardPendingApproval
import com.mileway.feature.payables.ui.previews.PreviewPoLineItemsMatrix
import com.mileway.feature.payables.ui.previews.PreviewPoListMatrix
import com.mileway.feature.tracking.ui.previews.PreviewSetupGuideScreen
import com.mileway.feature.tracking.ui.previews.PreviewTrackLoadingCustomMessage
import com.mileway.feature.tracking.ui.previews.PreviewTrackLoadingDefault
import com.mileway.feature.tracking.ui.previews.PreviewTrackSettingsScreen
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessClean
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessWithViolation
import com.mileway.feature.tracking.ui.previews.PreviewTrackingSuccessWithVoucher
import com.mileway.feature.travel.ui.previews.PreviewBookingCardActiveFlight
import com.mileway.feature.travel.ui.previews.PreviewBookingCardCompletedFlight
import com.mileway.feature.travel.ui.previews.PreviewBookingCardUpcomingTrain
import com.mileway.feature.travel.ui.previews.PreviewBookingListMatrix
import com.mileway.feature.whatsnew.ui.previews.PreviewWhatsNewDetailCarousel
import com.mileway.feature.whatsnew.ui.previews.PreviewWhatsNewDetailSingleMedia
import com.mileway.feature.whatsnew.ui.previews.PreviewWhatsNewListEmpty
import com.mileway.feature.whatsnew.ui.previews.PreviewWhatsNewListPopulated
import com.mileway.ui.search.PreviewMasterSearchEmpty
import com.mileway.ui.search.PreviewMasterSearchResults
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// ---------------------------------------------------------------------------
// Roborazzi component catalog, Storytale replacement.
//
// Renders public @Preview composables from all feature modules as PNGs.
//
// Generate / update screenshots:
//   ./gradlew :app:recordRoborazziGmsDebug
//
// CI regression gate:
//   ./gradlew :app:verifyRoborazziGmsDebug
//
// Output: docs/screenshots/<name>.png
// ---------------------------------------------------------------------------

// Z.5c: qualifiers pins a fixed device config (density/viewport), matching
// ScreenshotGalleryTest/WelcomeDisclaimerSheetTest's convention. Without it, Robolectric's
// default device config is left to environment defaults, which is the root cause of the
// ~110-baseline nondeterministic re-record noise this class produced per run.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotCatalogTest {

    @get:Rule
    val composeRule = createComposeRule()

    // See the same @Before in ScreenshotGalleryTest: Compose Multiplatform 1.12 resources need an
    // Android Context that the auto-init ContentProvider doesn't supply under Robolectric. Preview
    // composables that read `Res.string.*` otherwise throw MissingResourceException. Must be a
    // per-test @Before (not @BeforeClass) so Robolectric's per-test environment is ready for
    // ApplicationProvider; the static it sets persists for the whole JVM fork.
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @Before
    fun initComposeResources() {
        ComposeResourcesTestFixture.install()
        org.jetbrains.compose.resources.setResourceReaderAndroidContext(
            ApplicationProvider.getApplicationContext(),
        )
    }

    // PLAN_V36 P8: feature:whatsnew's WhatsNewEntryCard/HeroCarousel are the first catalog entries
    // to render a real AsyncImage. Coil3's default ImageLoader fetches/decodes on Dispatchers.IO,
    // a real thread pool Robolectric's compose-idle check doesn't track — a subsequent
    // verifyRoborazziGmsDebug run flaked to a blank hero image (raced the decode against capture;
    // see app/build/outputs/roborazzi/whatsnew_list_screen_compare.png from that run). Unconfined
    // runs fetch+decode inline on the calling thread instead of hopping to a background one, so
    // it's always finished before this test's composeRule is considered idle.
    @Before
    fun installDeterministicImageLoader() {
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(ApplicationProvider.getApplicationContext())
                .coroutineContext(Dispatchers.Unconfined)
                .build(),
        )
    }

    private fun capture(name: String) =
        composeRule.onRoot().captureRoboImage(
            File(screenshotsDir, "$name.png").absolutePath,
        )

    // ── feature:tracking ────────────────────────────────────────────────────

    @Test
    fun tracking_loadingScreen_default() {
        composeRule.setContent { PreviewTrackLoadingDefault() }
        capture("tracking_loadingScreen_default")
    }

    @Test
    fun tracking_loadingScreen_subStatuses() {
        composeRule.setContent { PreviewTrackLoadingCustomMessage() }
        capture("tracking_loadingScreen_subStatuses")
    }

    @Test
    fun tracking_successScreen_clean() {
        composeRule.setContent { PreviewTrackingSuccessClean() }
        capture("tracking_successScreen_clean")
    }

    @Test
    fun tracking_successScreen_withViolation() {
        composeRule.setContent { PreviewTrackingSuccessWithViolation() }
        capture("tracking_successScreen_withViolation")
    }

    @Test
    fun tracking_successScreen_withVoucher() {
        composeRule.setContent { PreviewTrackingSuccessWithVoucher() }
        capture("tracking_successScreen_withVoucher")
    }

    @Test
    fun tracking_settingsScreen() {
        composeRule.setContent { PreviewTrackSettingsScreen() }
        capture("tracking_settingsScreen")
    }

    @Test
    fun tracking_setupGuide() {
        composeRule.setContent { PreviewSetupGuideScreen() }
        capture("tracking_setupGuide")
    }

    // ── feature:approvals ───────────────────────────────────────────────────

    @Test
    fun approvals_item_pending() {
        composeRule.setContent { PreviewApprovalItemPending() }
        capture("approvals_item_pending")
    }

    @Test
    fun approvals_item_withViolation() {
        composeRule.setContent { PreviewApprovalItemWithViolation() }
        capture("approvals_item_withViolation")
    }

    @Test
    fun approvals_item_approved() {
        composeRule.setContent { PreviewApprovalItemApproved() }
        capture("approvals_item_approved")
    }

    @Test
    fun approvals_item_rejected() {
        composeRule.setContent { PreviewApprovalItemRejected() }
        capture("approvals_item_rejected")
    }

    // ── feature:payables ────────────────────────────────────────────────────

    @Test
    fun payables_card_approved() {
        composeRule.setContent { PreviewPoCardApproved() }
        capture("payables_card_approved")
    }

    @Test
    fun payables_card_pendingApproval() {
        composeRule.setContent { PreviewPoCardPendingApproval() }
        capture("payables_card_pendingApproval")
    }

    @Test
    fun payables_list_matrix() {
        composeRule.setContent { PreviewPoListMatrix() }
        capture("payables_list_matrix")
    }

    @Test
    fun payables_lineItems_matrix() {
        composeRule.setContent { PreviewPoLineItemsMatrix() }
        capture("payables_lineItems_matrix")
    }

    // ── feature:travel ───────────────────────────────────────────────────────

    @Test
    fun travel_bookingCard_activeFlight() {
        composeRule.setContent { PreviewBookingCardActiveFlight() }
        capture("travel_bookingCard_activeFlight")
    }

    @Test
    fun travel_bookingCard_upcomingTrain() {
        composeRule.setContent { PreviewBookingCardUpcomingTrain() }
        capture("travel_bookingCard_upcomingTrain")
    }

    @Test
    fun travel_bookingCard_completedFlight() {
        composeRule.setContent { PreviewBookingCardCompletedFlight() }
        capture("travel_bookingCard_completedFlight")
    }

    @Test
    fun travel_bookingList_matrix() {
        composeRule.setContent { PreviewBookingListMatrix() }
        capture("travel_bookingList_matrix")
    }

    // ── feature:whatsnew ────────────────────────────────────────────────────

    @Test
    fun whatsnew_list_screen() {
        composeRule.setContent { PreviewWhatsNewListPopulated() }
        capture("whatsnew_list_screen")
    }

    @Test
    fun whatsnew_list_empty() {
        composeRule.setContent { PreviewWhatsNewListEmpty() }
        capture("whatsnew_list_empty")
    }

    @Test
    fun whatsnew_detail_screen() {
        composeRule.setContent { PreviewWhatsNewDetailSingleMedia() }
        capture("whatsnew_detail_screen")
    }

    @Test
    fun whatsnew_detail_carousel() {
        composeRule.setContent { PreviewWhatsNewDetailCarousel() }
        capture("whatsnew_detail_carousel")
    }

    // ── app:master-search ────────────────────────────────────────────────────

    @Test
    fun search_masterSearch_results() {
        composeRule.setContent { PreviewMasterSearchResults() }
        capture("search_masterSearch_results")
    }

    @Test
    fun search_masterSearch_empty() {
        composeRule.setContent { PreviewMasterSearchEmpty() }
        capture("search_masterSearch_empty")
    }

    // ── core:ui — Design Language v2 theme picker (one per curated scheme) ─────

    @Test
    fun theme_picker_matrix() {
        composeRule.setContent { PreviewThemePickerMatrix() }
        capture("theme_picker_matrix")
    }

    @Test
    fun theme_picker_amoled() {
        composeRule.setContent { PreviewThemePickerAmoled() }
        capture("theme_picker_amoled")
    }

    @Test
    fun theme_picker_ion() {
        composeRule.setContent { PreviewThemePickerIon() }
        capture("theme_picker_ion")
    }

    @Test
    fun theme_picker_daybreak() {
        composeRule.setContent { PreviewThemePickerDaybreak() }
        capture("theme_picker_daybreak")
    }

    // ── core:ui — minimal matrix tracking top bar (Task 1) ────────────────────

    @Test
    fun tracking_topBar_active() {
        composeRule.setContent { PreviewTrackingTopBarActive() }
        capture("tracking_topBar_active")
    }

    @Test
    fun tracking_topBar_paused() {
        composeRule.setContent { PreviewTrackingTopBarPaused() }
        capture("tracking_topBar_paused")
    }

    @Test
    fun tracking_topBar_idle() {
        composeRule.setContent { PreviewTrackingTopBarIdle() }
        capture("tracking_topBar_idle")
    }

    @Test
    fun tracking_topBar_active_daybreak() {
        composeRule.setContent { PreviewTrackingTopBarActiveDaybreak() }
        capture("tracking_topBar_active_daybreak")
    }

    @Test
    fun tracking_statusPills() {
        composeRule.setContent { PreviewTrackingStatusPills() }
        capture("tracking_statusPills")
    }

    // ── core:ui — matrix-re-tokened hero tracking card (Task 3) ───────────────

    @Test
    fun tracking_heroCard_active() {
        composeRule.setContent { PreviewHeroTrackingCardActive() }
        capture("tracking_heroCard_active")
    }

    @Test
    fun tracking_heroCard_idle() {
        composeRule.setContent { PreviewHeroTrackingCardIdle() }
        capture("tracking_heroCard_idle")
    }

    @Test
    fun tracking_heroCard_active_ion() {
        composeRule.setContent { PreviewHeroTrackingCardActiveIon() }
        capture("tracking_heroCard_active_ion")
    }

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }
    }
}
