package com.miletracker.feature.payments.model

/** Whether a payment sends money out (pay) or asks for money in (request) (PM). */
enum class PaymentDirection(val label: String) {
    PAY("Pay"),
    REQUEST("Request"),
}

/** Lifecycle status of a UPI/QR payment (PM). */
enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
}

/** One row in the payments history (PM) — a mocked UPI/QR pay or request. */
data class PaymentRecord(
    val id: String,
    val direction: PaymentDirection,
    val counterparty: String,
    val amount: Double,
    val status: PaymentStatus,
    val note: String,
    val dateMillis: Long,
)
