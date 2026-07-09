package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P8.1: a single external payment wallet (Paytm/Mobikwik-style) shown on the
 * "Payment wallets" section of Connected Accounts. Linking runs an offline OTP challenge
 * (purpose WALLET_LINK, via the shared `LocalOtpEngine`) — there is NO real payment SDK or
 * network call (see CLAUDE.md "The backend"). [balanceMinor] is a seeded mock balance in the
 * currency's minor unit (paise); [isLinked] is the only field the link/unlink flow mutates.
 */
@Entity(tableName = "payment_wallets")
data class PaymentWalletEntity(
    @PrimaryKey
    val id: String,
    val providerName: String,
    /** The registered mobile the OTP is "sent" to — also the masked display target. */
    val mobile: String,
    val isLinked: Boolean,
    val balanceMinor: Long,
    val updatedAtMs: Long,
)
