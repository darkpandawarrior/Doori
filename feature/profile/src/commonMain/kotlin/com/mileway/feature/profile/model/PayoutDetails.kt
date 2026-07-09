package com.mileway.feature.profile.model

/**
 * PLAN_V24 P8.2: the payout-identity block shown on Profile Details — a seeded, display-only bank
 * account (the reference app `bank_account_no`/`ifsc_code`/`bank_name`) plus an editable UPI handle rendered
 * as a QR. Bank fields are mock constants (no backend); only the UPI handle is user-editable and
 * persisted (in the session — see `SessionRepository.setUpiHandle`).
 */
object PayoutDetails {
    const val BANK_NAME: String = "HDFC Bank"
    const val IFSC: String = "HDFC0001234"

    /** The seeded (mock) full account number — never shown in full; [maskedAccount] is displayed. */
    const val ACCOUNT_NUMBER: String = "50100123456789"

    /** Masks all but the last four digits, e.g. "•••• •••• 6789". */
    val maskedAccount: String
        get() {
            val last4 = ACCOUNT_NUMBER.takeLast(4)
            return "•••• •••• $last4"
        }

    /** A UPI handle is valid when it is `name@provider` — one or more word chars either side of `@`. */
    private val UPI_REGEX = Regex("^\\w+@\\w+$")

    fun isValidUpiHandle(handle: String): Boolean = UPI_REGEX.matches(handle.trim())

    /** The `upi://pay?pa=<handle>` string a real UPI QR would encode (illustrative QR here). */
    fun upiPayString(handle: String): String = "upi://pay?pa=${handle.trim()}&pn=Mileway"
}
