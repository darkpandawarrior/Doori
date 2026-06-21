package com.miletracker.feature.travel.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.miletracker.feature.travel.ui.screens.TravelHomeScreen

/**
 * TR.1 — the shared, commonMain travel navigation graph. Promotes `feature:travel` off the app-only direct
 * `TravelHomeScreen()` call onto a real nested graph (mirrors `payablesGraph`) so the booking hub plus every TR
 * create flow / history surface render from one graph on Android and iOS alike. Create / history routes are
 * added by the later TR tasks; the hub's quick actions navigate into them.
 */
object TravelRoutes {
    const val HOME = "travel_home"
}

fun NavGraphBuilder.travelGraph(
    @Suppress("UNUSED_PARAMETER") navController: NavHostController,
) {
    composable(TravelRoutes.HOME) {
        TravelHomeScreen()
    }
}
