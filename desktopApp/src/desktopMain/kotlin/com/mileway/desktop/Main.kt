package com.mileway.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.siddharth.kmp.ai.NoModelManager
import com.siddharth.kmp.ai.UnavailableOnDeviceLlm
import com.siddharth.kmp.designsystem.ai.AiSettingsState
import com.siddharth.kmp.llmchat.SecureKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    // AI card (lane mileway-ai-settings-and-desktop): no on-device model on the JVM, so the
    // on-device tier is always UnavailableOnDeviceLlm/NoModelManager — see Assistant.kt for the
    // BYOK cloud fallback that's the only real answer path here. keyStore has no Context to build
    // from on desktop (unlike Android), only a real file-backed store — see SecureKeyStore.jvm.kt.
    val keyStore = SecureKeyStore()
    val aiSettingsState =
        AiSettingsState(
            modelManager = NoModelManager,
            manifest = emptyList(),
            onDeviceLlm = UnavailableOnDeviceLlm,
            getKey = keyStore::getKey,
            setKey = keyStore::setKey,
            // ponytail: process-lifetime scope — this app has no shorter-lived owner to tie it to
            // (see Main.kt's own single-window, no-navigation shape); same tradeoff
            // ProfileModule.kt's AiSettingsState binding documents on Android.
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

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
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    DashboardScreen(snapshot, trips)
                    AssistantCard(aiSettingsState, keyStore::getKey)
                }
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
            // A plain Column, not LazyColumn: Main.kt now scrolls the whole window (the new
            // Assistant card below needs room), and a lazily-scrolling list measured inside an
            // already-unbounded-height scroll container crashes at runtime ("vertically scrollable
            // component was measured with an infinity maximum height"). Mock data is a handful of
            // rows, so a plain loop costs nothing here.
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.s)) {
                trips.forEach { trip ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(trip.name.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        Text(trip.getFormattedDistance(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
