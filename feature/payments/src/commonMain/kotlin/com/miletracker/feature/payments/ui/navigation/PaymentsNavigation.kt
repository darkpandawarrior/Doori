package com.miletracker.feature.payments.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.miletracker.feature.payments.ui.screens.CreatePaymentScreen
import com.miletracker.feature.payments.ui.screens.PaymentsHistoryScreen

/**
 * PM: the shared, commonMain payments navigation graph (mirrors `payablesGraph`/`travelGraph`): a payments
 * history hub plus the QR/UPI pay-or-request create flow. Reachable from the Home quick-action grid + master
 * search (wired in the EN phase).
 */
object PaymentsRoutes {
    const val HOME = "payments_home"
    const val CREATE = "payments/create"
}

fun NavGraphBuilder.paymentsGraph(navController: NavHostController) {
    composable(PaymentsRoutes.HOME) {
        PaymentsHistoryScreen(onBack = { navController.popBackStack() })
    }
    composable(PaymentsRoutes.CREATE) {
        CreatePaymentScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
}
