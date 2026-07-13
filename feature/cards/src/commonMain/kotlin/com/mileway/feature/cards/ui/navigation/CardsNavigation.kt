package com.mileway.feature.cards.ui.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.feature.cards.ui.CardDetailScreen
import com.mileway.feature.cards.ui.CardKycScreen
import com.mileway.feature.cards.ui.CardRequestScreen
import com.mileway.feature.cards.ui.CardsHomeScreen

/** Key CardKycScreen's completion result is stashed under on the DETAIL entry's savedStateHandle. */
private const val KYC_VERIFIED_KEY = "kyc_verified"

/** Q.6: cards feature routes + nav graph (the home is the nested graph's start destination). */
object CardRoutes {
    const val HOME = "cards_home"
    const val REQUEST = "cards_request"
    const val DETAIL = "cards_detail/{cardId}"
    const val KYC = "cards_kyc"

    fun detail(cardId: Long): String = "cards_detail/$cardId"
}

/**
 * [onClaimTransaction] (P27.E.7) is supplied by the app shell so feature:cards never depends on
 * feature:logging directly — it only hands back the [ExpenseSourceContext] the app shell needs to
 * build feature:logging's expense-entry route.
 */
fun NavGraphBuilder.cardsGraph(
    navController: NavHostController,
    onClaimTransaction: (ExpenseSourceContext) -> Unit = {},
) {
    composable(CardRoutes.HOME) {
        CardsHomeScreen(
            onOpenCard = { navController.navigate(CardRoutes.detail(it)) },
            onRequestCard = { navController.navigate(CardRoutes.REQUEST) },
            onStartKyc = { navController.navigate(CardRoutes.KYC) },
        )
    }
    composable(CardRoutes.REQUEST) {
        CardRequestScreen(onDone = { navController.popBackStack() })
    }
    composable(CardRoutes.KYC) {
        CardKycScreen(
            onDone = { completed ->
                // P29.C.1: stash the result on the DETAIL entry (not this one, which is about to
                // be popped) — the standard Navigation-Compose "pass a result back" pattern.
                if (completed) {
                    navController.previousBackStackEntry?.savedStateHandle?.set(KYC_VERIFIED_KEY, true)
                }
                navController.popBackStack()
            },
        )
    }
    composable(
        route = CardRoutes.DETAIL,
        arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
    ) { entry ->
        val cardId = entry.arguments?.read { getStringOrNull("cardId") }?.toLongOrNull() ?: 0L
        val kycJustVerified by entry.savedStateHandle.getStateFlow(KYC_VERIFIED_KEY, false).collectAsState()
        CardDetailScreen(
            cardId = cardId,
            onBack = { navController.popBackStack() },
            onClaimTransaction = onClaimTransaction,
            onStartKyc = { navController.navigate(CardRoutes.KYC) },
            kycJustVerified = kycJustVerified,
            onKycAcknowledged = { entry.savedStateHandle[KYC_VERIFIED_KEY] = false },
        )
    }
}
