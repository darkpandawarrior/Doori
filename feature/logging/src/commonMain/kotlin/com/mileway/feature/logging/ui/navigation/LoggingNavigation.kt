package com.mileway.feature.logging.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.mileway.core.data.model.ExpenseSourceContext
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
import com.mileway.feature.logging.ui.screens.VoucherDetailsScreen
import com.mileway.feature.logging.ui.screens.VoucherHistoryScreen
import com.mileway.feature.logging.viewmodel.ExpenseAction
import org.koin.compose.viewmodel.koinViewModel

// P27.E.5/7/8: [LoggingRoutes.EXPENSE_ENTRY]'s context query-param names, shared between
// [LoggingRoutes.expenseEntryRoute] (encode) and [decodeExpenseSourceContext] (decode).
private const val CTX_ARG_TYPE = "ctxType"
private const val CTX_ARG_ID1 = "ctxId1"
private const val CTX_ARG_ID2 = "ctxId2"
private const val CTX_ARG_LABEL = "ctxLabel"
private const val CTX_ARG_AMOUNT = "ctxAmount"

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

    /**
     * Add Expense Step 1, category picker. Optional query params (all default to blank) carry an
     * [ExpenseSourceContext] the entry screen decodes back via [decodeExpenseSourceContext] — see
     * [expenseEntryRoute]. A bare navigate to this literal pattern (no query) still matches, since
     * every arg has a default — that's how the plain "Add Expense" tap and "Add Another" continue
     * to work unchanged.
     */
    const val EXPENSE_ENTRY =
        "expense/entry?$CTX_ARG_TYPE={$CTX_ARG_TYPE}&$CTX_ARG_ID1={$CTX_ARG_ID1}&$CTX_ARG_ID2={$CTX_ARG_ID2}" +
            "&$CTX_ARG_LABEL={$CTX_ARG_LABEL}&$CTX_ARG_AMOUNT={$CTX_ARG_AMOUNT}"

    /** Add Expense Step 2, amount, merchant, notes + submit. */
    const val EXPENSE_DETAILS = "expense/details"

    /** Add Expense success screen. */
    const val EXPENSE_SUCCESS = "expense/success"

    /** Expense history list. */
    const val EXPENSE_HISTORY = "expense/history"

    /** Expense detail pushed screen. Route param: id. */
    const val EXPENSE_DETAIL = "expense/detail/{id}"

    fun expenseDetailRoute(id: String) = "expense/detail/$id"

    /** Voucher history list (submitted vouchers, Room-backed via the shared VoucherDao). */
    const val VOUCHER_HISTORY = "voucher/history"

    /** P27.E.12: voucher detail pushed screen, reachable from [VOUCHER_HISTORY]'s cards. Route param: voucherNumber. */
    const val VOUCHER_DETAIL = "voucher/detail/{voucherNumber}"

    fun voucherDetailRoute(voucherNumber: String) = "voucher/detail/$voucherNumber"

    /**
     * P27.E.5/7/8: encodes [context] onto [EXPENSE_ENTRY]'s query params — the shared seam that
     * lets feature:tracking/cards/profile CTAs hand an [ExpenseSourceContext] to the expense flow
     * through a plain route string, so every feature still meets feature:logging only at :app's
     * composition root (never a direct feature→feature dependency; see CLAUDE.md's module-
     * boundary rule). Only the P27.E.5/7/8 variants (Trip/Card/Advance) are encoded richly; every
     * other variant (including the default [ExpenseSourceContext.Regular]) falls back to the bare
     * "no context" entry — identical to the pre-existing plain "Add Expense" tap. Reverse of
     * [decodeExpenseSourceContext].
     */
    fun expenseEntryRoute(context: ExpenseSourceContext = ExpenseSourceContext.Regular): String {
        val type: String
        val id1: String
        val id2: String
        val label: String
        val amount: String
        when (context) {
            is ExpenseSourceContext.Trip -> {
                type = "trip"
                id1 = context.tripId
                id2 = ""
                label = context.tripLabel.orEmpty()
                amount = ""
            }
            is ExpenseSourceContext.Card -> {
                type = "card"
                id1 = context.cardId
                id2 = context.transactionId
                label = context.merchantName.orEmpty()
                amount = context.transactionAmountRupees?.toString().orEmpty()
            }
            is ExpenseSourceContext.Advance -> {
                type = "advance"
                id1 = context.advanceId
                id2 = ""
                label = context.advanceLabel.orEmpty()
                amount = ""
            }
            else -> return "expense/entry"
        }
        return "expense/entry?$CTX_ARG_TYPE=$type&$CTX_ARG_ID1=${encodeRouteArg(id1)}&$CTX_ARG_ID2=${encodeRouteArg(id2)}" +
            "&$CTX_ARG_LABEL=${encodeRouteArg(label)}&$CTX_ARG_AMOUNT=${encodeRouteArg(amount)}"
    }
}

/**
 * Reverse of [LoggingRoutes.expenseEntryRoute]: rebuilds the [ExpenseSourceContext] the
 * expense-entry route arrived with. A blank/unrecognized [ctxType] (the plain "Add Expense" tap,
 * or any variant this task didn't wire — Event/Message/Scanner/Edit/TripAdvance) resolves to
 * [ExpenseSourceContext.None], identical to the pre-P27.E.5/7/8 fresh-ViewModel default.
 */
internal fun decodeExpenseSourceContext(
    ctxType: String,
    ctxId1: String,
    ctxId2: String,
    ctxLabel: String,
    ctxAmount: String,
): ExpenseSourceContext {
    val id1 = decodeRouteArg(ctxId1)
    val id2 = decodeRouteArg(ctxId2)
    val label = decodeRouteArg(ctxLabel).ifBlank { null }
    val amount = decodeRouteArg(ctxAmount).toDoubleOrNull()
    return when (ctxType) {
        "trip" -> ExpenseSourceContext.Trip(id1, label)
        "card" -> ExpenseSourceContext.Card(id1, id2, label, amount)
        "advance" -> ExpenseSourceContext.Advance(id1, label)
        else -> ExpenseSourceContext.None
    }
}

/**
 * Hand-rolled percent-encoding for a single nav query-arg value. `java.net.URLEncoder` isn't
 * available outside the JVM/Android source sets, and this file is commonMain (targets iOS too via
 * navigation-compose's KMP artifact), so route-arg encoding has to be pure Kotlin stdlib.
 * Byte-level: only ASCII letters/digits/`-_.~` pass through unescaped, everything else (including
 * every non-ASCII UTF-8 byte) becomes `%XX`. Reverse: [decodeRouteArg].
 */
private fun encodeRouteArg(s: String): String =
    buildString {
        for (byte in s.encodeToByteArray()) {
            val v = byte.toInt() and 0xFF
            val safe = v < 128 && (v.toChar().isLetterOrDigit() || v.toChar() in "-_.~")
            if (safe) append(v.toChar()) else append('%').append(v.toHex2())
        }
    }

private fun decodeRouteArg(s: String): String {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '%' && i + 2 < s.length) {
            bytes.add(s.substring(i + 1, i + 3).toInt(16).toByte())
            i += 3
        } else {
            bytes.add(c.code.toByte())
            i += 1
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun Int.toHex2(): String {
    val hex = "0123456789ABCDEF"
    return "" + hex[(this shr 4) and 0xF] + hex[this and 0xF]
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
            onAddExpense = { navController.navigate(LoggingRoutes.expenseEntryRoute()) },
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
            onOpenDraft = { draftId ->
                // P5.1: rehydrate the shared LogMilesViewModel from the persisted draft before
                // returning to Step 1, so the fields the user saved are actually restored, not just
                // a no-op back-navigation onto whatever the form happened to hold.
                viewModel.onAction(com.mileway.feature.logging.viewmodel.LogMilesAction.LoadDraft(draftId))
                navController.popBackStack(LoggingRoutes.LOG_MILES, inclusive = false)
            },
        )
    }

    // ── Expense flow ─────────────────────────────────────────────────────────
    composable(
        route = LoggingRoutes.EXPENSE_ENTRY,
        arguments =
            listOf(
                navArgument("ctxType") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("ctxId1") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("ctxId2") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("ctxLabel") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("ctxAmount") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
    ) { entry ->
        val viewModel =
            koinViewModel<com.mileway.feature.logging.viewmodel.ExpenseViewModel>(
                viewModelStoreOwner = entry,
            )
        val a = entry.arguments
        // P27.E.5/7/8: rebuild the ExpenseSourceContext the trip/card/advance CTA handed off
        // through the route, then hand it straight to the ViewModel's existing openWithContext
        // (P27.E.4) action. A bare "Add Expense" tap decodes to None, same as before this task.
        val context =
            decodeExpenseSourceContext(
                ctxType = a?.read { getStringOrNull("ctxType") }.orEmpty(),
                ctxId1 = a?.read { getStringOrNull("ctxId1") }.orEmpty(),
                ctxId2 = a?.read { getStringOrNull("ctxId2") }.orEmpty(),
                ctxLabel = a?.read { getStringOrNull("ctxLabel") }.orEmpty(),
                ctxAmount = a?.read { getStringOrNull("ctxAmount") }.orEmpty(),
            )
        LaunchedEffect(entry) { viewModel.onAction(ExpenseAction.OpenWithContext(context)) }
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
                navController.navigate(LoggingRoutes.expenseEntryRoute()) {
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
            // P1.8: OpenEdit is dispatched on this route's own ExpenseViewModel instance, so the
            // edit destination shares that same instance (looked up below by this exact route,
            // the same pattern rememberExpenseEntry uses for EXPENSE_ENTRY) to see the
            // pre-filled form, instead of getting a fresh one from EXPENSE_ENTRY's store.
            onEdit = { navController.navigate(LoggingRoutes.EXPENSE_DETAILS) },
        )
    }

    // ── Voucher history + detail (P27.E.12) ─────────────────────────────────────
    composable(LoggingRoutes.VOUCHER_HISTORY) {
        VoucherHistoryScreen(
            onBack = { navController.popBackStack() },
            onOpenDetail = { voucherNumber -> navController.navigate(LoggingRoutes.voucherDetailRoute(voucherNumber)) },
        )
    }

    composable(
        route = LoggingRoutes.VOUCHER_DETAIL,
        arguments = listOf(navArgument("voucherNumber") { type = NavType.StringType }),
    ) { backStackEntry ->
        val voucherNumber = backStackEntry.arguments?.read { getStringOrNull("voucherNumber") } ?: return@composable
        VoucherDetailsScreen(
            voucherNumber = voucherNumber,
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
 * screens share the same ExpenseViewModel as the entry screen. P1.8: when the details screen was
 * reached via the edit/resubmit path instead (no [LoggingRoutes.EXPENSE_ENTRY] on the stack),
 * falls back to the [LoggingRoutes.EXPENSE_DETAIL] entry that dispatched `OpenEdit`, so the
 * pre-filled form is visible here rather than a fresh ViewModel instance.
 */
@androidx.compose.runtime.Composable
private fun rememberExpenseEntry(navController: NavHostController) =
    androidx.compose.runtime.remember(navController.currentBackStackEntry) {
        runCatching { navController.getBackStackEntry(LoggingRoutes.EXPENSE_ENTRY) }.getOrNull()
            ?: runCatching { navController.getBackStackEntry(LoggingRoutes.EXPENSE_DETAIL) }.getOrNull()
    } ?: navController.currentBackStackEntry!!
