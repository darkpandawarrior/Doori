package com.mileway.feature.logging.ui.model

/**
 * A submitted log-miles voucher row shown on the History → Submitted tab. These
 * are voucher-style cards grouped by expense date in the UI, mirroring the
 * reference "Voucher Not Created / Self Paid" layout.
 */
data class SubmittedVoucher(
    val id: String,
    val voucherState: String,
    val payment: String,
    val chips: List<String>,
    val office: String,
    val amount: Double,
    val serviceTag: String,
    val expenseDateMillis: Long,
    val expenseId: String,
    val submittedOnMillis: Long,
    val violationCount: Int,
)

/**
 * Deterministic offline sample of submitted vouchers for the History screen.
 * Mirrors the reference screenshots (Voucher Not Created, Self Paid, policy
 * violations, "Log Conveyance"/"log_trip" service tags) without any backend.
 */
object SubmittedVoucherSamples {
    private const val DAY_MS = 86_400_000L

    /** Built once relative to [now] so the grouped date headers look current. */
    fun sample(now: Long): List<SubmittedVoucher> =
        listOf(
            SubmittedVoucher(
                id = "O-INDIAN-000048767",
                voucherState = "Voucher Not Created",
                payment = "Self Paid",
                chips = listOf("Attachments", "Acknowledged"),
                office = "HQ_NORTH",
                amount = 22_629.00,
                serviceTag = "Log Conveyance",
                expenseDateMillis = now - 5 * DAY_MS,
                expenseId = "O-INDIAN-000048767",
                submittedOnMillis = now - 4 * DAY_MS,
                violationCount = 1,
            ),
            SubmittedVoucher(
                id = "O-INDIAN-000048746",
                voucherState = "Voucher Not Created",
                payment = "Self Paid",
                chips = listOf("Attachments", "Acknowledged"),
                office = "HQ_NORTH",
                amount = 14_850.00,
                serviceTag = "log_trip",
                expenseDateMillis = now - 38 * DAY_MS,
                expenseId = "O-INDIAN-000048746",
                submittedOnMillis = now - 18 * DAY_MS,
                violationCount = 0,
            ),
            SubmittedVoucher(
                id = "O-INDIAN-000048747",
                voucherState = "Voucher Not Created",
                payment = "Self Paid",
                chips = listOf("Attachments", "Acknowledged"),
                office = "HQ_NORTH",
                amount = 9_240.00,
                serviceTag = "log_trip",
                expenseDateMillis = now - 42 * DAY_MS,
                expenseId = "O-INDIAN-000048747",
                submittedOnMillis = now - 20 * DAY_MS,
                violationCount = 0,
            ),
            SubmittedVoucher(
                id = "O-INDIAN-000048701",
                voucherState = "Voucher Not Created",
                payment = "Self Paid",
                chips = listOf("Acknowledged"),
                office = "HQ_WEST",
                amount = 6_310.00,
                serviceTag = "Log Conveyance",
                expenseDateMillis = now - 70 * DAY_MS,
                expenseId = "O-INDIAN-000048701",
                submittedOnMillis = now - 60 * DAY_MS,
                violationCount = 2,
            ),
        )

    /** Local roster of teammates used by the Tagged Employees multi-select. */
    val taggableEmployees: List<String> =
        listOf(
            "Aarav Mehta",
            "Diya Sharma",
            "Kabir Nair",
            "Ananya Iyer",
            "Rohan Verma",
        )
}
