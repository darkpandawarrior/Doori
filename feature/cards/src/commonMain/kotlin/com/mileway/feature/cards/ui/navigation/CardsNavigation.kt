package com.mileway.feature.cards.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.feature.cards.ui.CardDetailScreen
import com.mileway.feature.cards.ui.CardRequestScreen
import com.mileway.feature.cards.ui.CardsHomeScreen

/** Q.6: cards feature routes + nav graph (the home is the nested graph's start destination). */
object CardRoutes {
    const val HOME = "cards_home"
    const val REQUEST = "cards_request"
    const val DETAIL = "cards_detail/{cardId}"

    fun detail(cardId: Long): String = "cards_detail/$cardId"
}

fun NavGraphBuilder.cardsGraph(navController: NavHostController) {
    composable(CardRoutes.HOME) {
        CardsHomeScreen(
            onOpenCard = { navController.navigate(CardRoutes.detail(it)) },
            onRequestCard = { navController.navigate(CardRoutes.REQUEST) },
        )
    }
    composable(CardRoutes.REQUEST) {
        CardRequestScreen(onDone = { navController.popBackStack() })
    }
    composable(
        route = CardRoutes.DETAIL,
        arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
    ) { entry ->
        val cardId = entry.arguments?.read { getStringOrNull("cardId") }?.toLongOrNull() ?: 0L
        CardDetailScreen(cardId = cardId, onBack = { navController.popBackStack() })
    }
}
