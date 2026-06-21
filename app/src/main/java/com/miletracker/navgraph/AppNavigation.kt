package com.miletracker.navgraph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miletracker.core.ui.AppHost
import com.miletracker.feature.approvals.ui.navigation.approvalsGraph
import com.miletracker.feature.logging.ui.navigation.loggingGraph
import com.miletracker.feature.tracking.ui.navigation.trackingGraph

// Cross-feature wiring lives HERE only, features never import each other.
// Each feature exposes NavGraphBuilder.xGraph(nav, callbacks) and this file
// hands them lambdas that reference AppKey destinations.

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    AppHost {
        NavHost(navController = navController, startDestination = "saved_tracks") {

            // --- Tracking feature ---
            trackingGraph(
                navController = navController,
            )

            // --- Approvals feature ---
            approvalsGraph(
                navController = navController,
            )

            // --- Logging / Expense feature ---
            loggingGraph(
                navController = navController,
            )
        }
    }
}
