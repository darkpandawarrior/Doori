package com.mileway.webpreview

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mileway.core.ui.theme.MilewayTheme
import com.mileway.webpreview.screens.DashboardScreen
import com.mileway.webpreview.screens.ExpensesScreen
import com.mileway.webpreview.screens.TrackingScreen

private enum class Tab(val label: String) { DASHBOARD("Home"), TRACKING("Track"), EXPENSES("Expenses") }

/**
 * The web preview shell: the real MilewayTheme (compiled from core:ui's theme sources) wrapping a
 * three-tab demo — dashboard, live simulated tracking, expense log — over in-memory fakes.
 */
@Composable
fun MilewayWebPreviewApp() {
    MilewayTheme {
        val scope = rememberCoroutineScope()
        val engine = remember { DemoTrackingEngine(scope) }
        val store = remember { DemoExpenseStore() }
        var tab by remember { mutableStateOf(Tab.DASHBOARD) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = {
                                Icon(
                                    imageVector =
                                        when (t) {
                                            Tab.DASHBOARD -> Icons.Default.Home
                                            Tab.TRACKING -> Icons.Default.GpsFixed
                                            Tab.EXPENSES -> Icons.Default.ReceiptLong
                                        },
                                    contentDescription = t.label,
                                )
                            },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (tab) {
                Tab.DASHBOARD ->
                    DashboardScreen(
                        engine = engine,
                        store = store,
                        onStartTracking = {
                            engine.start()
                            tab = Tab.TRACKING
                        },
                        onAddExpense = { tab = Tab.EXPENSES },
                        modifier = contentModifier,
                    )
                Tab.TRACKING -> TrackingScreen(engine = engine, modifier = contentModifier)
                Tab.EXPENSES -> ExpensesScreen(store = store, modifier = contentModifier)
            }
        }
    }
}
