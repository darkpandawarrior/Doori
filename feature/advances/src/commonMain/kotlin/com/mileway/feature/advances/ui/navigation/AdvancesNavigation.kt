package com.mileway.feature.advances.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.feature.advances.ui.AdvancesHomeScreen
import com.mileway.feature.advances.ui.AskAdvanceFormScreen
import com.mileway.feature.advances.ui.PettyCardDetailScreen
import com.mileway.feature.advances.ui.QrCardDetailScreen
import com.mileway.feature.advances.ui.QrRequestFormScreen

/** PLAN_V35 P4: advances feature routes + nav graph (mirrors feature:cards' CardsNavigation). */
object AdvancesRoutes {
    const val HOME = "advances_home"
    const val ASK_ADVANCE = "advances_ask"
    const val QR_REQUEST = "advances_qr_request"
    const val PETTY_DETAIL = "advances_petty/{cardId}"
    const val QR_DETAIL = "advances_qr/{cardId}"

    fun pettyDetail(cardId: Long): String = "advances_petty/$cardId"

    fun qrDetail(cardId: Long): String = "advances_qr/$cardId"
}

/**
 * Cross-feature actions (add expense / log miles / track miles / scan-to-pay) are supplied by the
 * app shell so feature:advances never depends on another feature module — same seam pattern as
 * cardsGraph's onClaimTransaction.
 */
fun NavGraphBuilder.advancesGraph(
    navController: NavHostController,
    onAddExpense: () -> Unit = {},
    onLogMiles: () -> Unit = {},
    onTrackMiles: () -> Unit = {},
    onScanQr: () -> Unit = {},
) {
    composable(AdvancesRoutes.HOME) {
        AdvancesHomeScreen(
            onOpenPettyCard = { navController.navigate(AdvancesRoutes.pettyDetail(it)) },
            onOpenQrCard = { navController.navigate(AdvancesRoutes.qrDetail(it)) },
            onRequestPettyAdvance = { navController.navigate(AdvancesRoutes.ASK_ADVANCE) },
            onRequestQrCard = { navController.navigate(AdvancesRoutes.QR_REQUEST) },
        )
    }
    composable(AdvancesRoutes.ASK_ADVANCE) {
        AskAdvanceFormScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() },
        )
    }
    composable(AdvancesRoutes.QR_REQUEST) {
        QrRequestFormScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() },
        )
    }
    composable(
        route = AdvancesRoutes.PETTY_DETAIL,
        arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
    ) { entry ->
        val cardId = entry.arguments?.read { getStringOrNull("cardId") }?.toLongOrNull() ?: 0L
        PettyCardDetailScreen(
            cardId = cardId,
            onBack = { navController.popBackStack() },
            onAddExpense = onAddExpense,
            onLogMiles = onLogMiles,
            onTrackMiles = onTrackMiles,
        )
    }
    composable(
        route = AdvancesRoutes.QR_DETAIL,
        arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
    ) { entry ->
        val cardId = entry.arguments?.read { getStringOrNull("cardId") }?.toLongOrNull() ?: 0L
        QrCardDetailScreen(
            cardId = cardId,
            onBack = { navController.popBackStack() },
            onScanQr = onScanQr,
        )
    }
}
