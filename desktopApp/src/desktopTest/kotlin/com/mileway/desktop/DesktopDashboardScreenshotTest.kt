package com.mileway.desktop

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
 * showcase/T.1: host-renders the desktop dashboard (same mock data as [main]) to a PNG via
 * Compose Multiplatform's [runDesktopComposeUiTest] — pure-JVM, no emulator/simulator needed.
 * Output: docs/screenshots/desktop_dashboard.png.
 */
@OptIn(ExperimentalTestApi::class)
class DesktopDashboardScreenshotTest {
    // Manual screenshot generator (writes docs/screenshots/desktop_dashboard.png). @Ignore'd because
    // it's flaky under headless Skiko in CI — run explicitly with --tests to regenerate the image.
    @Test
    @Ignore("Manual screenshot generator; flaky under headless Skiko — run explicitly with --tests")
    fun captureDesktopDashboard() =
        runDesktopComposeUiTest(width = 900, height = 700) {
            val nowEpochMs = 10 * 86_400_000L // fixed instant, deterministic
            initKoin(modules = listOf(coreUiModule)) // AppLocaleEnvironment reads LocaleController from Koin.
            setContent {
                AppHost {
                    DashboardScreenForScreenshot(mockSnapshot(nowEpochMs), mockTripRows(nowEpochMs))
                }
            }
            val screenshotsDir = File(repoRoot(), "docs/screenshots").also { it.mkdirs() }
            val image = captureToImage().toAwtImage()
            ImageIO.write(image, "png", File(screenshotsDir, "desktop_dashboard.png"))
        }
}

// internal (not private): shared with DesktopScreenshotGalleryTest (showcase/T.2).
internal fun repoRoot(): File {
    val moduleDir = File(System.getProperty("user.dir") ?: ".")
    return if (moduleDir.name == "desktopApp") moduleDir.parentFile else moduleDir
}
