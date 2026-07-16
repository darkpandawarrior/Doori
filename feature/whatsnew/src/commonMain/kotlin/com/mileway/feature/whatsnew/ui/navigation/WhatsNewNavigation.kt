package com.mileway.feature.whatsnew.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.feature.whatsnew.ui.WhatsNewDetailScreen
import com.mileway.feature.whatsnew.ui.WhatsNewListScreen

/** PLAN_V36 P3: What's New feature routes + nav graph. Shape follows `feature:cards`' CardRoutes. */
object WhatsNewRoutes {
    const val LIST = "whatsnew_list"
    const val DETAIL = "whatsnew_detail/{entryId}"

    fun detail(entryId: String): String = "whatsnew_detail/$entryId"
}

/**
 * Nested under [com.mileway.ui.AppGraph.WHATS_NEW] in the app shell (`MilewayAppRoot`), reached
 * from the digest sheet's "See all updates" / row taps and the Settings entry — never a bottom-nav
 * tab, so it always renders full-screen (see `MilewayAppRoot`'s `topLevelRoutes` gate).
 */
fun NavGraphBuilder.whatsNewGraph(navController: NavHostController) {
    composable(WhatsNewRoutes.LIST) {
        WhatsNewListScreen(
            onBack = { navController.popBackStack() },
            onOpenEntry = { entryId -> navController.navigate(WhatsNewRoutes.detail(entryId)) },
        )
    }
    composable(
        route = WhatsNewRoutes.DETAIL,
        arguments = listOf(navArgument("entryId") { type = NavType.StringType }),
    ) { entry ->
        val entryId = entry.arguments?.read { getStringOrNull("entryId") }.orEmpty()
        WhatsNewDetailScreen(
            entryId = entryId,
            onBack = { navController.popBackStack() },
        )
    }
}
