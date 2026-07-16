package com.mileway.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * showcase/T.2: expands the single [DesktopDashboardScreenshotTest] dashboard shot into a curated
 * gallery of the app's signature surfaces — same mechanism (Compose Multiplatform's
 * [runDesktopComposeUiTest], pure-JVM, no emulator/simulator), same fixed mock instant, same
 * 900x700 desktop window size. Each screen lives in `GalleryScreens.kt` (built from `core:ui`
 * widgets + `core:data` mock models — `desktopApp` has no `feature:*` dependency, see that file's
 * kdoc), this test only wires each one to a `docs/screenshots/desktop_*.png` output.
 *
 * All @Ignore'd for the same reason as the dashboard test: flaky under headless Skiko in CI — run
 * explicitly with --tests to regenerate the images.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopScreenshotGalleryTest {
    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureLiveTracking() = capture("desktop_tracking.png") { LiveTrackingScreenForScreenshot() }

    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureTripHistory() =
        capture("desktop_trip_history.png") {
            val nowEpochMs = 10 * 86_400_000L
            TripHistoryScreenForScreenshot(mockTripRows(nowEpochMs))
        }

    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureTripDetail() = capture("desktop_trip_detail.png") { TripDetailScreenForScreenshot() }

    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureLogExpense() = capture("desktop_log_expense.png") { LogExpenseScreenForScreenshot() }

    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureApprovals() = capture("desktop_approvals.png") { ApprovalsScreenForScreenshot() }

    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureProfile() = capture("desktop_profile.png") { ProfileScreenForScreenshot() }

    private fun capture(
        fileName: String,
        content: @Composable () -> Unit,
    ) = runDesktopComposeUiTest(width = 900, height = 700) {
        initKoin(modules = listOf(coreUiModule)) // AppLocaleEnvironment reads LocaleController from Koin.
        setContent { AppHost { content() } }
        val screenshotsDir = File(repoRoot(), "docs/screenshots").also { it.mkdirs() }
        val image = captureToImage().toAwtImage()
        ImageIO.write(image, "png", File(screenshotsDir, fileName))
    }
}
