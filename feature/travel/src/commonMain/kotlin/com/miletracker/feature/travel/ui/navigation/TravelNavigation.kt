package com.miletracker.feature.travel.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.miletracker.feature.travel.ui.screens.CreateBusScreen
import com.miletracker.feature.travel.ui.screens.CreateFlightScreen
import com.miletracker.feature.travel.ui.screens.CreateTripScreen
import com.miletracker.feature.travel.ui.screens.TravelHomeScreen

/**
 * TR.1 — the shared, commonMain travel navigation graph. Promotes `feature:travel` off the app-only direct
 * `TravelHomeScreen()` call onto a real nested graph (mirrors `payablesGraph`) so the booking hub plus every TR
 * create flow / history surface render from one graph on Android and iOS alike. Create / history routes are
 * added by the later TR tasks; the hub's quick actions navigate into them.
 */
object TravelRoutes {
    const val HOME = "travel_home"
    const val CREATE_TRIP = "travel/create_trip"
    const val CREATE_FLIGHT = "travel/create_flight"
    const val CREATE_BUS = "travel/create_bus"
}

fun NavGraphBuilder.travelGraph(navController: NavHostController) {
    composable(TravelRoutes.HOME) {
        TravelHomeScreen()
    }
    composable(TravelRoutes.CREATE_TRIP) {
        CreateTripScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(TravelRoutes.CREATE_FLIGHT) {
        CreateFlightScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(TravelRoutes.CREATE_BUS) {
        CreateBusScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
}
