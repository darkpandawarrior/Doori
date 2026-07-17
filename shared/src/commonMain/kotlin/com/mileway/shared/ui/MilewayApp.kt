package com.mileway.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mileway.core.ui.components.LanguageSelectionSheet
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.app_name
import com.mileway.core.ui.resources.settings_language
import com.mileway.core.ui.resources.tab_home
import com.mileway.core.ui.resources.tab_spends
import com.mileway.core.ui.resources.tab_track
import com.mileway.core.ui.resources.tab_travel
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.tracking.ui.screens.TrackMilesScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.feature.whatsnew.ui.WhatsNewDetailScreen
import com.mileway.feature.whatsnew.ui.WhatsNewListScreen
import com.mileway.ui.home.HomeScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class ShellTab(val label: StringResource, val icon: ImageVector)

/**
 * PLAN_V36 P8 (spec §10) — What's New's overlay state for the reduced iOS shell, which has no
 * Navigation-3 host (that graph is Android-only, see `whatsNewGraph`'s KDoc). A simple `remember`
 * (not `rememberSaveable`) — same choice this file already makes for `tab`/`showLanguage`, process
 * death just re-lands on the tab scaffold. List's header back arrow always → [WhatsNewScreenState.None].
 * Detail's back is origin-aware ([Detail.cameFromList]): opened from the List row → back lands on
 * List; opened directly from Home's digest sheet/banner → back lands on [WhatsNewScreenState.None]
 * (Home), never on a List screen the user never opened — mirrors Android's NavHost backstack pop.
 */
private sealed interface WhatsNewScreenState {
    data object None : WhatsNewScreenState

    data object List : WhatsNewScreenState

    data class Detail(val entryId: String, val cameFromList: Boolean) : WhatsNewScreenState
}

private val shellTabs =
    listOf(
        ShellTab(Res.string.tab_home, Icons.Filled.Home),
        ShellTab(Res.string.tab_track, Icons.Filled.DirectionsCar),
        ShellTab(Res.string.tab_spends, Icons.Filled.ReceiptLong),
        ShellTab(Res.string.tab_travel, Icons.Filled.Flight),
    )

/**
 * The shared app-shell root that renders the real Mileway screens under a bottom-tab bar. Both the
 * Android `:app` (via its Navigation-3 host) and the iOS entry can render Mileway's screens; iOS
 * uses this composable directly (the Navigation-3 graph is Android-only), so iOS now shows the real
 * home dashboard + core features instead of a component showcase — full KMP/CMP shell parity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilewayApp() {
    var tab by remember { mutableIntStateOf(0) }
    var showLanguage by remember { mutableStateOf(false) }
    var whatsNewScreen by remember { mutableStateOf<WhatsNewScreenState>(WhatsNewScreenState.None) }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                    actions = {
                        // Shared language switcher — the only in-app entry on iOS (Android also exposes it
                        // in Settings). Selecting a language flips LocalAppLocale, re-resolving every
                        // string instantly on both platforms.
                        IconButton(onClick = { showLanguage = true }) {
                            Icon(
                                Icons.Filled.Language,
                                contentDescription = stringResource(Res.string.settings_language),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    shellTabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, contentDescription = stringResource(t.label)) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 ->
                        HomeScreen(
                            onStartTracking = { tab = 1 },
                            onAddExpense = { tab = 2 },
                            onOpenAccount = {},
                            onOpenWhatsNewEntry = { entryId ->
                                whatsNewScreen = WhatsNewScreenState.Detail(entryId, cameFromList = false)
                            },
                            onSeeAllWhatsNew = { whatsNewScreen = WhatsNewScreenState.List },
                        )
                    1 ->
                        TrackMilesScreen(
                            onStop = { _, _, _, _, _ -> tab = 0 },
                            onOpenMap = {},
                            onOpenHwEvents = {},
                        )
                    2 ->
                        SpendsHomeScreen(
                            onTrackMileage = { tab = 1 },
                            onAddExpense = {},
                            onMileageHistory = {},
                            onExpenseHistory = {},
                        )
                    else -> TravelHomeScreen()
                }
            }
        }

        // PLAN_V36 P8 (spec §10) — full-screen overlay above the tab scaffold, reached from the
        // digest sheet's "See all updates" / row taps (HomeScreen's already-hoisted callbacks
        // above). No two-pane/shared-transition here — those are Android-only NavHost paths; both
        // screens' transition params default to null and render fine standalone.
        when (val screen = whatsNewScreen) {
            WhatsNewScreenState.None -> Unit
            WhatsNewScreenState.List ->
                WhatsNewListScreen(
                    onBack = { whatsNewScreen = WhatsNewScreenState.None },
                    onOpenEntry = { entryId ->
                        whatsNewScreen = WhatsNewScreenState.Detail(entryId, cameFromList = true)
                    },
                )
            is WhatsNewScreenState.Detail ->
                WhatsNewDetailScreen(
                    entryId = screen.entryId,
                    onBack = {
                        whatsNewScreen =
                            if (screen.cameFromList) WhatsNewScreenState.List else WhatsNewScreenState.None
                    },
                )
        }
    }
    if (showLanguage) {
        LanguageSelectionSheet(onDismiss = { showLanguage = false })
    }
}
