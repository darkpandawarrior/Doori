package com.mileway.feature.agent.engine

internal object ThinkingPhrases {
    fun forIntent(intent: Intent): String = when (intent) {
        Intent.MILEAGE_WEEK -> "Reviewing your trip history…"
        Intent.MILEAGE_RATE -> "Checking reimbursement policy…"
        Intent.EXPENSE_REJECTION -> "Looking up rejection reason…"
        Intent.POLICY_CAP -> "Reviewing policy rules…"
        Intent.ADVANCE_STATUS -> "Checking advance records…"
        Intent.CARD_BALANCE -> "Fetching card balance…"
        Intent.PENDING_APPROVALS -> "Looking up pending items…"
        Intent.TRIP_SUMMARY -> "Checking your travel data…"
        Intent.GENERIC -> "Thinking…"
    }
}
