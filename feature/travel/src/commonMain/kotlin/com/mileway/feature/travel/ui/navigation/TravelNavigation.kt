package com.mileway.feature.travel.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.mileway.feature.travel.ui.screens.BookingHistoryScreen
import com.mileway.feature.travel.ui.screens.CreateBusScreen
import com.mileway.feature.travel.ui.screens.CreateFlightScreen
import com.mileway.feature.travel.ui.screens.CreateHotelScreen
import com.mileway.feature.travel.ui.screens.CreateMjpScreen
import com.mileway.feature.travel.ui.screens.CreateTripScreen
import com.mileway.feature.travel.ui.screens.CreateVisaScreen
import com.mileway.feature.travel.ui.screens.TravelHomeScreen
import com.mileway.feature.travel.ui.screens.TripHistoryScreen

/**
 * TR.1: the shared, commonMain travel navigation graph. Promotes `feature:travel` off the app-only direct
 * `TravelHomeScreen()` call onto a real nested graph (mirrors `payablesGraph`) so the booking hub plus every TR
 * create flow / history surface render from one graph on Android and iOS alike. Create / history routes are
 * added by the later TR tasks; the hub's quick actions navigate into them.
 */
object TravelRoutes {
    const val HOME = "travel_home"
    const val CREATE_TRIP = "travel/create_trip"
    const val CREATE_FLIGHT = "travel/create_flight"
    const val CREATE_BUS = "travel/create_bus"
    const val CREATE_HOTEL = "travel/create_hotel"
    const val CREATE_MJP = "travel/create_mjp"
    const val CREATE_VISA = "travel/create_visa"
    const val TRIP_HISTORY = "travel/trip_history"
    const val BOOKING_HISTORY = "travel/booking_history"
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
    composable(TravelRoutes.CREATE_HOTEL) {
        CreateHotelScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(TravelRoutes.CREATE_MJP) {
        CreateMjpScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(TravelRoutes.CREATE_VISA) {
        CreateVisaScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(TravelRoutes.TRIP_HISTORY) {
        TripHistoryScreen(onBack = { navController.popBackStack() })
    }
    composable(TravelRoutes.BOOKING_HISTORY) {
        BookingHistoryScreen(onBack = { navController.popBackStack() })
    }
}
