package com.mileway.feature.events.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.core.data.model.ExpenseSourceContext
import com.mileway.feature.events.ui.screens.CreateEventScreen
import com.mileway.feature.events.ui.screens.EventDetailScreen
import com.mileway.feature.events.ui.screens.EventsHistoryScreen

/**
 * EV: the shared, commonMain events navigation graph (mirrors `payablesGraph`): an events history hub, the
 * create-event flow, and (V29 P29.E.1) an event-detail push route. Reachable from the Home quick-action grid +
 * master search (wired in the EN phase).
 */
object EventsRoutes {
    const val HOME = "events_home"
    const val CREATE = "events/create"

    /**
     * V29 P29.E.1: event-detail pushed screen, reachable from a history card tap. Route param: id.
     * Prefixed `detail/` (mirrors `LoggingRoutes.EXPENSE_DETAIL`) rather than the bare `events/{id}`
     * the plan doc sketches, so it can't collide with the literal [CREATE] destination.
     */
    const val DETAIL = "events/detail/{id}"

    fun detailRoute(id: String) = "events/detail/$id"
}

/**
 * @param onLogExpense V27 P27.E.6's deferred events→expense CTA: [EventDetailScreen] hands back an
 * [ExpenseSourceContext.Event] the same way `cardsGraph`'s `onClaimTransaction` does — `:app`'s
 * composition root threads it to `LoggingRoutes.expenseEntryRoute`, keeping feature:events off of
 * feature:logging (module-boundary rule, CLAUDE.md).
 */
fun NavGraphBuilder.eventsGraph(
    navController: NavHostController,
    onLogExpense: (ExpenseSourceContext) -> Unit = {},
) {
    composable(EventsRoutes.HOME) {
        EventsHistoryScreen(
            onBack = { navController.popBackStack() },
            onOpenEvent = { id -> navController.navigate(EventsRoutes.detailRoute(id)) },
        )
    }
    composable(EventsRoutes.CREATE) {
        CreateEventScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() },
        )
    }
    composable(
        EventsRoutes.DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.read { getStringOrNull("id") }.orEmpty()
        EventDetailScreen(
            eventId = id,
            onBack = { navController.popBackStack() },
            onLogExpense = onLogExpense,
        )
    }
}
