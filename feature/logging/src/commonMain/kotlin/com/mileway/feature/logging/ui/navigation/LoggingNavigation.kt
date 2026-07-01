package com.mileway.feature.logging.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.feature.logging.ui.screens.ExpenseDetailScreen
import com.mileway.feature.logging.ui.screens.ExpenseDetailsInputScreen
import com.mileway.feature.logging.ui.screens.ExpenseEntryScreen
import com.mileway.feature.logging.ui.screens.ExpenseHistoryScreen
import com.mileway.feature.logging.ui.screens.ExpenseSuccessScreen
import com.mileway.feature.logging.ui.screens.LogMilesHistoryScreen
import com.mileway.feature.logging.ui.screens.LogMilesScreen
import com.mileway.feature.logging.ui.screens.LogMilesStep2Screen
import com.mileway.feature.logging.ui.screens.LogMilesSuccessScreen
import com.mileway.feature.logging.ui.screens.SpendsHomeScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Route constants for the Spends tab (Log Miles + Expense flows).
 *
 * [HOME] is the Spends hub landing, renders two primary-action cards.
 * [LOG_MILES] is the Log Miles Step 1 entry; deeper log-miles routes share
 * its ViewModel via [rememberLogMilesEntry].
 */
object LoggingRoutes {
    /** Spends hub, two-card home (top-level tab destination). */
    const val HOME = "spends_home"

    /** Log Miles Step 1, journey basics + travelled locations. */
    const val LOG_MILES = "log_miles"

    /** Log Miles Step 2, expense details + submission. */
    const val STEP2 = "log_miles/step2"

    /** Log Miles post-submission success route. */
    const val SUCCESS = "log_miles/success"

    /** Log Miles drafts + submitted history. */
    const val HISTORY = "log_miles/history"

    /** Add Expense Step 1, category picker. */
    const val EXPENSE_ENTRY = "expense/entry"

    /** Add Expense Step 2, amount, merchant, notes + submit. */
    const val EXPENSE_DETAILS = "expense/details"

    /** Add Expense success screen. */
    const val EXPENSE_SUCCESS = "expense/success"

    /** Expense history list. */
    const val EXPENSE_HISTORY = "expense/history"

    /** Expense detail pushed screen. Route param: id. */
    const val EXPENSE_DETAIL = "expense/detail/{id}"

    fun expenseDetailRoute(id: String) = "expense/detail/$id"
}

/**
 * Logging + Expense destinations as a reusable nav-graph builder.
 *
 * The Spends home renders two action cards; each branches into its own sub-flow:
 *   - Track Mileage → Log Miles two-step flow (shares one ViewModel anchored to LOG_MILES)
 *   - Add Expense   → Expense category → details → success flow (ExpenseViewModel)
 */
fun NavGraphBuilder.loggingGraph(navController: NavHostController) {
    // ── Spends home ──────────────────────────────────────────────────────────
    composable(LoggingRoutes.HOME) {
        SpendsHomeScreen(
            onTrackMileage = { navController.navigate(LoggingRoutes.LOG_MILES) },
            onAddExpense = { navController.navigate(LoggingRoutes.EXPENSE_ENTRY) },
            onMileageHistory = { navController.navigate(LoggingRoutes.HISTORY) },
            onExpenseHistory = { navController.navigate(LoggingRoutes.EXPENSE_HISTORY) },
        )
    }

    // ── Log Miles flow ───────────────────────────────────────────────────────
    composable(LoggingRoutes.LOG_MILES) { entry ->
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.LogMilesViewModel>(
                viewModelStoreOwner = entry,
            )
        LogMilesScreen(
            viewModel = viewModel,
            onNext = { navController.navigate(LoggingRoutes.STEP2) },
            onOpenHistory = { navController.navigate(LoggingRoutes.HISTORY) },
        )
    }

    composable(LoggingRoutes.STEP2) {
        val logMilesEntry = rememberLogMilesEntry(navController)
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.LogMilesViewModel>(
                viewModelStoreOwner = logMilesEntry,
            )
        LogMilesStep2Screen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onSubmitted = {
                navController.navigate(LoggingRoutes.SUCCESS) {
                    popUpTo(LoggingRoutes.STEP2) { inclusive = true }
                }
            },
        )
    }

    composable(LoggingRoutes.SUCCESS) {
        val logMilesEntry = rememberLogMilesEntry(navController)
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.LogMilesViewModel>(
                viewModelStoreOwner = logMilesEntry,
            )
        LogMilesSuccessScreen(
            viewModel = viewModel,
            onLogAnother = {
                navController.navigate(LoggingRoutes.LOG_MILES) {
                    popUpTo(LoggingRoutes.LOG_MILES) { inclusive = true }
                }
            },
        )
    }

    composable(LoggingRoutes.HISTORY) {
        val logMilesEntry = rememberLogMilesEntry(navController)
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.LogMilesViewModel>(
                viewModelStoreOwner = logMilesEntry,
            )
        LogMilesHistoryScreen(
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
            onOpenDraft = {
                navController.popBackStack(LoggingRoutes.LOG_MILES, inclusive = false)
            },
        )
    }

    // ── Expense flow ─────────────────────────────────────────────────────────
    composable(LoggingRoutes.EXPENSE_ENTRY) { entry ->
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.ExpenseViewModel>(
                viewModelStoreOwner = entry,
            )
        ExpenseEntryScreen(
            onBack = { navController.popBackStack() },
            onCategorySelected = { navController.navigate(LoggingRoutes.EXPENSE_DETAILS) },
            viewModel = viewModel,
        )
    }

    composable(LoggingRoutes.EXPENSE_DETAILS) {
        val expenseEntry = rememberExpenseEntry(navController)
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.ExpenseViewModel>(
                viewModelStoreOwner = expenseEntry,
            )
        ExpenseDetailsInputScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = {
                navController.navigate(LoggingRoutes.EXPENSE_SUCCESS) {
                    popUpTo(LoggingRoutes.EXPENSE_DETAILS) { inclusive = true }
                }
            },
            viewModel = viewModel,
        )
    }

    composable(LoggingRoutes.EXPENSE_SUCCESS) {
        val expenseEntry = rememberExpenseEntry(navController)
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.ExpenseViewModel>(
                viewModelStoreOwner = expenseEntry,
            )
        ExpenseSuccessScreen(
            onAddAnother = {
                navController.navigate(LoggingRoutes.EXPENSE_ENTRY) {
                    popUpTo(LoggingRoutes.EXPENSE_ENTRY) { inclusive = true }
                }
            },
            onViewHistory = {
                navController.navigate(LoggingRoutes.EXPENSE_HISTORY) {
                    popUpTo(LoggingRoutes.EXPENSE_SUCCESS) { inclusive = true }
                }
            },
            viewModel = viewModel,
        )
    }

    composable(LoggingRoutes.EXPENSE_HISTORY) {
        ExpenseHistoryScreen(
            onBack = { navController.popBackStack() },
            onOpenDetail = { id -> navController.navigate(LoggingRoutes.expenseDetailRoute(id)) },
        )
    }

    composable(
        route = LoggingRoutes.EXPENSE_DETAIL,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.read { getStringOrNull("id") } ?: return@composable
        ExpenseDetailScreen(
            expenseId = id,
            onBack = { navController.popBackStack() },
        )
    }
}

/**
 * Resolves the [LoggingRoutes.LOG_MILES] back-stack entry so step-2, success and history
 * share the same ViewModel store as Step 1. Falls back to the current entry defensively.
 */
@androidx.compose.runtime.Composable
private fun rememberLogMilesEntry(navController: NavHostController) =
    androidx.compose.runtime.remember(navController.currentBackStackEntry) {
        runCatching { navController.getBackStackEntry(LoggingRoutes.LOG_MILES) }.getOrNull()
    } ?: navController.currentBackStackEntry!!

/**
 * Resolves the [LoggingRoutes.EXPENSE_ENTRY] back-stack entry so the details and success
 * screens share the same ExpenseViewModel as the entry screen.
 */
@androidx.compose.runtime.Composable
private fun rememberExpenseEntry(navController: NavHostController) =
    androidx.compose.runtime.remember(navController.currentBackStackEntry) {
        runCatching { navController.getBackStackEntry(LoggingRoutes.EXPENSE_ENTRY) }.getOrNull()
    } ?: navController.currentBackStackEntry!!
