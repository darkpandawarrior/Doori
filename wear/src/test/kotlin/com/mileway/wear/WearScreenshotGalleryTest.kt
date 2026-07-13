package com.mileway.wear

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.github.takahirom.roborazzi.captureRoboImage
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.wear.theme.WearMilewayTheme
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * showcase/Wear.1: Roborazzi host-render of the Ember Wear dashboard + trip list + tile over
 * deterministic mock data — Robolectric + Roborazzi, no device/emulator. Uses the
 * `captureRoboImage { content }` composable-content form (NO ComposeRule/Activity) so the Wear
 * manifest's watch-only launcher doesn't break Robolectric activity resolution.
 * Output: docs/screenshots/wear_*.png.
 */
@RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
@Config(sdk = [33], application = Application::class, qualifiers = "w227dp-h227dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WearScreenshotGalleryTest {

    companion object {
        private val screenshotsDir: File by lazy {
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = if (moduleDir.name == "wear") moduleDir.parentFile else moduleDir
            File(repoRoot, "docs/screenshots").also { it.mkdirs() }
        }

        @org.junit.BeforeClass
        @JvmStatic
        fun setup() {
            System.setProperty("roborazzi.test.record", "true")
        }
    }

    @Test
    fun wearDashboard() {
        captureRoboImage(File(screenshotsDir, "wear_dashboard.png").absolutePath) {
            WearMilewayTheme {
                AppScaffold {
                    val listState = rememberScalingLazyListState()
                    // Z.5c: ScreenScaffold's default timeText renders the real wall-clock time, so
                    // the golden drifted by a few pixels on every re-record depending on when the
                    // test ran. Suppress it here (production still shows the live clock) — a fixed
                    // empty header is the deterministic choice for a golden image.
                    ScreenScaffold(scrollState = listState, timeText = {}) {
                        WearDashboard(
                            uiState =
                                WearRootUiState(
                                    todayDistanceKm = 12.4,
                                    weekDistanceKm = 58.7,
                                    isTracking = true,
                                    weekGoalKm = 100.0,
                                    weekGoalProgress = 0.587f,
                                    trips = mockTrips(),
                                ),
                            listState = listState,
                            onTripsClick = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun wearTripList() {
        captureRoboImage(File(screenshotsDir, "wear_trip_list.png").absolutePath) {
            WearMilewayTheme {
                AppScaffold {
                    val listState = rememberScalingLazyListState()
                    // Z.5c: see wearDashboard()'s comment — fixed empty timeText for determinism.
                    ScreenScaffold(scrollState = listState, timeText = {}) {
                        TripListScreen(
                            trips = mockTrips(),
                            listState = listState,
                            onTripClick = {},
                        )
                    }
                }
            }
        }
    }

    // MileageTileService renders a ProtoLayout tile, not a Composable — this approximates its
    // content (today's distance label + the "Mileway" app label) as a Compose render so the Ember
    // tile visual is documented without a ProtoLayout renderer on the JVM.
    @Test
    fun wearTile() {
        captureRoboImage(File(screenshotsDir, "wear_tile.png").absolutePath) {
            WearMilewayTheme {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = WearPresentation.toTodayDistanceLabel(SurfaceSnapshot(todayDistanceKm = 12.4)),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(text = "Mileway", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }

    private fun mockTrips() =
        listOf(
            TripListItemUi(id = "t1", label = "Commute", km = 12.4, endMs = 1_700_000_000_000L),
            TripListItemUi(id = "t2", label = "Airport pickup", km = 42.1, endMs = 1_700_000_000_000L - 86_400_000L),
            TripListItemUi(id = "t3", label = "Warehouse run", km = 9.8, endMs = 1_700_000_000_000L - 3 * 86_400_000L),
        )
}
