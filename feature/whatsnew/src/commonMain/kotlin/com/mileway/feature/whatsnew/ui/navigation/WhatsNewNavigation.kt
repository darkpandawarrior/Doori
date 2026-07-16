package com.mileway.feature.whatsnew.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.feature.whatsnew.ui.WhatsNewAdaptiveScaffold
import com.mileway.feature.whatsnew.ui.WhatsNewDetailScreen

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
 *
 * [sharedTransitionScope] is `null` unless the caller wraps its `NavHost` in a
 * `SharedTransitionLayout` (PLAN_V36 P6, spec §6.1 — `MilewayAppRoot` does); each `composable()`'s
 * own content-lambda receiver supplies the per-destination `AnimatedContentScope` half. Both are
 * threaded down as plain parameters (not a `CompositionLocal`) — see `whatsNewSharedBounds`'s KDoc
 * in `feature.whatsnew.ui.components` for why.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.whatsNewGraph(
    navController: NavHostController,
    sharedTransitionScope: SharedTransitionScope? = null,
) {
    composable(WhatsNewRoutes.LIST) {
        WhatsNewAdaptiveScaffold(
            onBack = { navController.popBackStack() },
            onOpenEntryFullscreen = { entryId -> navController.navigate(WhatsNewRoutes.detail(entryId)) },
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = this,
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
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = this,
        )
    }
}
