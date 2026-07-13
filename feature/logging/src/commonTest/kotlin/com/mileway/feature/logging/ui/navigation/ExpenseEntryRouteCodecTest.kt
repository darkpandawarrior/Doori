package com.mileway.feature.logging.ui.navigation

import com.mileway.core.data.model.ExpenseSourceContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P27.E.5/7/8: [LoggingRoutes.expenseEntryRoute] / [decodeExpenseSourceContext] round-trip. This
 * is the pure half of the shared nav seam — [LoggingNavigation.kt]'s `composable(EXPENSE_ENTRY)`
 * wires the same two functions to real nav args, which isn't unit-testable without a Compose
 * host, so the codec itself (encode → parse the literal query string → decode) is what's proven
 * here.
 */
class ExpenseEntryRouteCodecTest {
    /** Mirrors how a real `NavArgument` read would see each `key=value` query segment: raw, un-decoded. */
    private fun queryParam(
        route: String,
        key: String,
    ): String {
        val query = route.substringAfter('?', "")
        return query.split('&')
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            .orEmpty()
    }

    private fun decode(route: String): ExpenseSourceContext =
        decodeExpenseSourceContext(
            ctxType = queryParam(route, "ctxType"),
            ctxId1 = queryParam(route, "ctxId1"),
            ctxId2 = queryParam(route, "ctxId2"),
            ctxLabel = queryParam(route, "ctxLabel"),
            ctxAmount = queryParam(route, "ctxAmount"),
        )

    @Test
    fun `a bare Add Expense tap round-trips to None`() {
        val route = LoggingRoutes.expenseEntryRoute()
        assertEquals("expense/entry", route)
        assertEquals(ExpenseSourceContext.None, decode(route))
    }

    @Test
    fun `Trip round-trips through the route string`() {
        val ctx = ExpenseSourceContext.Trip(tripId = "trip-1", tripLabel = "Mumbai to Pune")
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `Trip with a null label round-trips`() {
        val ctx = ExpenseSourceContext.Trip(tripId = "trip-2", tripLabel = null)
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `Card round-trips both ids, the merchant name and the amount`() {
        val ctx =
            ExpenseSourceContext.Card(
                cardId = "card-1",
                transactionId = "txn-1",
                merchantName = "Indigo Airlines",
                transactionAmountRupees = 4500.5,
            )
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `Advance round-trips`() {
        val ctx = ExpenseSourceContext.Advance(advanceId = "adv-1", advanceLabel = "Field visit advance")
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `labels with reserved route characters survive the round trip`() {
        val ctx = ExpenseSourceContext.Trip(tripId = "trip-3", tripLabel = "Mumbai & Pune / 100% done?")
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `Edit round-trips through the route string`() {
        val ctx = ExpenseSourceContext.Edit(expenseId = "EXP-007")
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }

    @Test
    fun `Event round-trips through the route string`() {
        val ctx = ExpenseSourceContext.Event(eventId = "evt-1", eventLabel = "Annual Summit")
        assertEquals(ctx, decode(LoggingRoutes.expenseEntryRoute(ctx)))
    }
}
