package com.mileway.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mileway.core.data.model.display.SurfaceSnapshot
import com.mileway.core.data.model.display.TrackDisplayData
import com.mileway.core.ui.AppHost
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.di.coreUiModule
import com.mileway.core.ui.di.initKoin
import com.mileway.core.ui.theme.DesignTokens
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * PLAN_V23 D.2: `:desktopApp`'s entry point — a thin Compose Desktop window rendering the
 * dashboard + trip list over mock data (Option b: no live backend, see CLAUDE.md "The backend").
 *
 * Reuses [AppHost]/[MilewayTheme]/[SectionCard] from `core:ui` (D.1's opted-in desktop target) —
 * same Matrix/terminal design language as the phone app, no bespoke desktop skin.
 */
@OptIn(ExperimentalTime::class)
fun main() {
    // coreUiModule provides LocaleController/ThemeController, which AppHost reads on every
    // screen (pre-existing gap: every other platform entry point already passes it, see
    // e.g. shared/src/iosMain/MilewayAppViewController.kt).
    initKoin(modules = listOf(coreUiModule))
    val nowEpochMs = Clock.System.now().toEpochMilliseconds()
    val snapshot = mockSnapshot(nowEpochMs)
    val trips = mockTripRows(nowEpochMs)

    // Compose Hot Reload sets this on the launched JVM (see the `-Dcompose.reload.isActive=true`
    // entry in desktopApp/build/run/desktopMain/desktopMain.argfile). Under `hotRunDesktop` the
    // window becomes a phone-shaped, always-on-top canvas that floats beside the editor — the
    // point of running UI on the JVM instead of booting an emulator. The SHIPPED desktop app
    // (nativeDistributions → Dmg/Deb/Msi) must not inherit either behaviour, hence the gate.
    val hotReloadCanvas = System.getProperty("compose.reload.isActive").toBoolean()

    application {
        // ponytail: ~9:19.5 portrait, the standard phone frame. Still resizable — drag a corner to
        // check compact → foldable → tablet breakpoints without a second AVD.
        val windowState = rememberWindowState(size = if (hotReloadCanvas) PhoneCanvasSize else DpSize(1280.dp, 800.dp))
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            alwaysOnTop = hotReloadCanvas,
            title = if (hotReloadCanvas) "Doori — Hot Reload canvas" else "Doori Dashboard",
        ) {
            AppHost {
                DashboardScreen(snapshot, trips)
            }
        }
    }
}

private val PhoneCanvasSize = DpSize(width = 390.dp, height = 844.dp)

// internal (not private): shared with DesktopDashboardScreenshotTest (showcase/T.1).
@Composable
internal fun DashboardScreenForScreenshot(
    snapshot: SurfaceSnapshot,
    trips: List<TrackDisplayData>,
) = DashboardScreen(snapshot, trips)

@Composable
private fun DashboardScreen(
    snapshot: SurfaceSnapshot,
    trips: List<TrackDisplayData>,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.l),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.l),
    ) {
        SectionCard(title = "Today") {
            Text("${snapshot.todayDistanceKm} km  ·  ${snapshot.todayTrips} trips", style = MaterialTheme.typography.bodyLarge)
        }
        SectionCard(title = "This week") {
            Text("${snapshot.weekDistanceKm} km  ·  ${snapshot.weekTrips} trips", style = MaterialTheme.typography.bodyLarge)
        }
        SectionCard(title = "Recent trips") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                items(trips, key = { it.token }) { trip ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(trip.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        Text(trip.getFormattedDistance(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
