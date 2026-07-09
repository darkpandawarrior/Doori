package com.mileway.feature.profile.model

/**
 * PLAN_V24 P8.1: a single external payment wallet on the "Payment wallets" section of Connected
 * Accounts. [balanceLabel] is the formatted seeded balance shown only while [isLinked]; [mobile]
 * is the masked registered number the offline OTP is dispatched to. Linking never calls a real
 * payment SDK (see CLAUDE.md "The backend").
 */
data class PaymentWallet(
    val id: String,
    val providerName: String,
    val mobile: String,
    val isLinked: Boolean,
    val balanceLabel: String,
)
