package com.mileway.feature.events.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mileway.feature.events.ui.screens.CreateEventScreen
import com.mileway.feature.events.ui.screens.EventsHistoryScreen

/**
 * EV: the shared, commonMain events navigation graph (mirrors `payablesGraph`): an events history hub plus the
 * create-event flow. Reachable from the Home quick-action grid + master search (wired in the EN phase).
 */
object EventsRoutes {
    const val HOME = "events_home"
    const val CREATE = "events/create"
}

fun NavGraphBuilder.eventsGraph(navController: NavHostController) {
    composable(EventsRoutes.HOME) {
        EventsHistoryScreen(onBack = { navController.popBackStack() })
    }
    composable(EventsRoutes.CREATE) {
        CreateEventScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
}
