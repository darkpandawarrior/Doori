package com.mileway.webpreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.webpreview.screens.DashboardScreen
import com.mileway.webpreview.screens.ExpensesScreen
import com.mileway.webpreview.screens.TrackingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Captures the web preview shell's screens.
 *
 * **What this proves, and what it does not.** Every screen here lives in `commonMain` — plain
 * Compose with no wasm-specific code; only `Main.kt` and one theme actual are wasmJs-only. So the
 * identical composables render on the JVM and can be captured, which covers the shell's UI.
 *
 * It does **not** prove the wasm binary runs in a browser. That needs a real browser harness
 * (Playwright against the built distribution) and is separate work. Saying so plainly matters: a
 * capture that quietly implied wasm-runtime coverage would be worse than no capture, because it
 * would retire the question without answering it.
 */
@OptIn(ExperimentalTestApi::class)
class WebPreviewScreenshotTest {
    @Test
    fun dashboard() =
        capture("web_dashboard.png") {
            DashboardScreen(
                engine = demoEngine(),
                store = DemoExpenseStore(),
                onStartTracking = {},
                onAddExpense = {},
            )
        }

    @Test
    fun tracking() = capture("web_tracking.png") { TrackingScreen(engine = demoEngine()) }

    @Test
    fun expenses() = capture("web_expenses.png") { ExpensesScreen(store = DemoExpenseStore()) }

    // A scope that is never started: the demo engine only ticks when explicitly driven, so the
    // captured frame is the deterministic initial state rather than whatever the animation happened
    // to be showing. A screenshot that races a coroutine is a screenshot that diffs at random.
    private fun demoEngine() = DemoTrackingEngine(CoroutineScope(Dispatchers.Unconfined))

    private fun capture(
        fileName: String,
        content: @Composable () -> Unit,
    ) = runDesktopComposeUiTest(width = 900, height = 700) {
        setContent { MilewayTheme { content() } }
        val dir = File(repoRoot(), "docs/screenshots").also { it.mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", File(dir, fileName))
    }

    /** Walks up from the module dir, so the output lands in the repo's one screenshots folder. */
    private fun repoRoot(): File {
        var d = File(System.getProperty("user.dir") ?: ".")
        while (d.name != "Mileway" && d.parentFile != null && !File(d, "settings.gradle.kts").exists()) {
            d = d.parentFile
        }
        return if (File(d, "settings.gradle.kts").exists()) d else d.parentFile ?: d
    }
}
