package com.mileway.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import com.mileway.feature.tracking.ui.screens.TrackMilesScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.ui.home.HomeScreen

private data class ShellTab(val label: String, val icon: ImageVector)

private val shellTabs =
    listOf(
        ShellTab("Home", Icons.Filled.Home),
        ShellTab("Track", Icons.Filled.DirectionsCar),
        ShellTab("Spends", Icons.Filled.ReceiptLong),
        ShellTab("Travel", Icons.Filled.Flight),
    )

/**
 * The shared app-shell root that renders the real Mileway screens under a bottom-tab bar. Both the
 * Android `:app` (via its Navigation-3 host) and the iOS entry can render Mileway's screens; iOS
 * uses this composable directly (the Navigation-3 graph is Android-only), so iOS now shows the real
 * home dashboard + core features instead of a component showcase — full KMP/CMP shell parity.
 */
@Composable
fun MilewayApp() {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                shellTabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
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
}
