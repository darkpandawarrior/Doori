package com.miletracker.core.data.search

/**
 * A launchable create-flow / service entry (F0.7) — surfaced by both the Home quick-actions grid and the
 * master-search "Actions" mode. [deeplink] routes into the flow; [iconKey] is a stable string the UI maps to
 * an ImageVector (keeps this model icon-library-free for commonMain purity).
 */
data class AppAction(
    val id: String,
    val label: String,
    val iconKey: String,
    val scope: SearchScope,
    val deeplink: String,
)

/**
 * The central list of create flows / services (F0.7). Master search "Actions" mode and the Home quick-action
 * grid both read it. Seeded as V17 flows land; [forScope] gates entries by scope tab.
 */
object QuickActionRegistry {
    val actions: List<AppAction> =
        listOf(
            AppAction("add_expense", "Add Expense", "receipt", SearchScope.EXPENSES, "miletracker://log/expense"),
            AppAction("log_miles", "Log Miles", "route", SearchScope.EXPENSES, "miletracker://log"),
            AppAction("track_miles", "Track Miles", "navigation", SearchScope.EXPENSES, "miletracker://track"),
            AppAction("request_advance", "Request Advance", "payments", SearchScope.EXPENSES, "miletracker://profile/advance/new"),
            AppAction("create_voucher", "Create Voucher", "folder", SearchScope.EXPENSES, "miletracker://log/voucher/new"),
            AppAction("create_invoice", "Create Invoice", "description", SearchScope.PAYABLES, "miletracker://payables/invoice/new"),
            AppAction("create_pr", "Purchase Request", "shopping_cart", SearchScope.PAYABLES, "miletracker://payables/pr/new"),
            AppAction("create_trip", "Create Trip", "flight", SearchScope.TRAVEL, "miletracker://travel/trip/new"),
            AppAction("qr_pay", "QR Pay", "qr_code", SearchScope.PAYABLES, "miletracker://payments/qr/pay"),
            AppAction("create_event", "Create Event", "event", SearchScope.VIEW_ALL, "miletracker://events/new"),
            AppAction("raise_query", "Raise Query", "support", SearchScope.VIEW_ALL, "miletracker://support/clarify/new"),
        )

    fun forScope(scope: SearchScope): List<AppAction> =
        if (scope == SearchScope.VIEW_ALL) actions else actions.filter { it.scope == scope || it.scope == SearchScope.VIEW_ALL }
}
