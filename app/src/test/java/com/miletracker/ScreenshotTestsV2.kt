package com.miletracker

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.miletracker.core.ui.theme.MileTrackerTheme
import com.miletracker.feature.events.di.eventsModule
import com.miletracker.feature.events.ui.screens.CreateEventScreen
import com.miletracker.feature.events.ui.screens.EventsHistoryScreen
import com.miletracker.feature.payments.di.paymentsModule
import com.miletracker.feature.payments.ui.screens.CreatePaymentScreen
import com.miletracker.feature.payments.ui.screens.PaymentsHistoryScreen
import com.miletracker.feature.payables.di.payablesModule
import com.miletracker.feature.travel.di.travelModule
import com.miletracker.feature.travel.ui.screens.BookingHistoryScreen
import com.miletracker.feature.travel.ui.screens.TripHistoryScreen
import io.mockk.mockk
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

// ---------------------------------------------------------------------------
// Roborazzi screenshots for the V17 breadth modules — payments + events create
// flows and the travel/payables history scaffolds.
//
// Isolated from [ScreenshotTests] on purpose: these screens resolve their
// ViewModels via koinViewModel(), so they need paymentsModule / eventsModule /
// travelModule / payablesModule in the graph. Wiring those into the existing
// setup() would touch all 13 baseline cases; a separate @BeforeClass keeps the
// blast radius to this file. The repositories these modules provide are already
// offline in-memory fakes (no-arg constructors), so no extra fake DAOs/repos
// are needed here.
//
// Record / update:
//   ./gradlew :app:testNoGmsDebugUnitTest \
//     --tests "com.miletracker.ScreenshotTestsV2" -Proborazzi.test.record=true
//
// Output: docs/screenshots/<name>.png
// ---------------------------------------------------------------------------

// Use plain Application to skip MileTrackerApplication.onCreate → startKoin
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTestsV2 {

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "app") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        @BeforeClass @JvmStatic
        fun setup() {
            System.setProperty("roborazzi.test.record", "true")
            try { stopKoin() } catch (_: Exception) {}
            startKoin {
                androidContext(mockk<Context>(relaxed = true))
                modules(paymentsModule, eventsModule, travelModule, payablesModule)
            }
        }

        @AfterClass @JvmStatic
        fun teardown() {
            try { stopKoin() } catch (_: Exception) {}
        }
    }

    @get:Rule
    val composeRule = createComposeRule()

    // ── feature:payments ─────────────────────────────────────────────────────

    @Test
    fun createPaymentScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreatePaymentScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("create_payment_screen")
    }

    @Test
    fun paymentsHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                PaymentsHistoryScreen(onBack = {})
            }
        }
        capture("payments_history_screen")
    }

    // ── feature:events ───────────────────────────────────────────────────────

    @Test
    fun createEventScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                CreateEventScreen(onBack = {}, onSubmitted = {})
            }
        }
        capture("create_event_screen")
    }

    @Test
    fun eventsHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                EventsHistoryScreen(onBack = {})
            }
        }
        capture("events_history_screen")
    }

    // ── feature:travel ───────────────────────────────────────────────────────

    @Test
    fun bookingHistoryScreen() {
        composeRule.setContent {
            MileTrackerTheme {
                BookingHistoryScreen(onBack = {})
            }
        }
        capture("booking_history_screen")
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

    private fun capture(name: String) =
        composeRule.onRoot().captureRoboImage(File(screenshotsDir, "$name.png").absolutePath)
}
