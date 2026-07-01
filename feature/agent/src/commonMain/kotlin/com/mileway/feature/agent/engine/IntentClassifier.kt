package com.mileway.feature.agent.engine

enum class Intent {
    MILEAGE_WEEK, MILEAGE_RATE,
    EXPENSE_REJECTION, POLICY_CAP, ADVANCE_STATUS,
    CARD_BALANCE, PENDING_APPROVALS, TRIP_SUMMARY,
    GENERIC,
}

internal object IntentClassifier {
    fun classify(message: String): Intent {
        val lower = message.lowercase()
        return when {
            lower.containsAny("km this week", "km last week", "mileage this week", "distance this week", "trips this week", "how many km", "km did i", "tracked") ->
                Intent.MILEAGE_WEEK
            lower.containsAny("reimbursement rate", "per km", "rate per km", "mileage rate") ->
                Intent.MILEAGE_RATE
            lower.containsAny("expense", "rejected", "rejection", "exp-") ->
                Intent.EXPENSE_REJECTION
            lower.containsAny("policy cap", "policy limit", "policy alert", "₹10/km", "mileage cap") ->
                Intent.POLICY_CAP
            lower.containsAny("advance", "adv-") ->
                Intent.ADVANCE_STATUS
            lower.containsAny("card balance", "card", "petty cash") ->
                Intent.CARD_BALANCE
            lower.containsAny("pending approval", "approvals", "approve") ->
                Intent.PENDING_APPROVALS
            lower.containsAny("trip summary", "trip", "travel", "flight", "booking") ->
                Intent.TRIP_SUMMARY
            else -> Intent.GENERIC
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }
}
