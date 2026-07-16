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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

    application {
        Window(onCloseRequest = ::exitApplication, title = "Mileway Dashboard") {
            AppHost {
                DashboardScreen(snapshot, trips)
            }
        }
    }
}

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
