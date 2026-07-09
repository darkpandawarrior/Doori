package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.PaymentWalletDao
import com.mileway.core.data.model.db.PaymentWalletEntity
import com.mileway.feature.profile.model.PaymentWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P8.1: Room-backed store for the "Payment wallets" section of Connected Accounts —
 * external wallet linking (Paytm/Mobikwik-style) via the shared offline `LocalOtpEngine`. Mirrors
 * [ConnectedAccountsRepository]'s seed-once-then-observe shape. Linking/unlinking only flips the
 * persisted [PaymentWalletEntity.isLinked] flag — there is no real payment SDK or network call.
 */
class WalletRepository(private val dao: PaymentWalletDao, private val clock: Clock = Clock.System) {
    /** Live, provider-name-ordered wallet list. */
    fun observeAll(): Flow<List<PaymentWallet>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    /** Seeds the two demo wallet providers on first run only; a no-op afterwards. */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(
            listOf(
                PaymentWalletEntity(
                    id = "wallet_paytm",
                    providerName = "Paytm Wallet",
                    mobile = "+91 90000 12345",
                    isLinked = false,
                    balanceMinor = 124_050L,
                    updatedAtMs = now,
                ),
                PaymentWalletEntity(
                    id = "wallet_mobikwik",
                    providerName = "Mobikwik",
                    mobile = "+91 90000 67890",
                    isLinked = false,
                    balanceMinor = 58_025L,
                    updatedAtMs = now,
                ),
            ),
        )
    }

    /** Marks [id] linked/unlinked — a local flag flip only, gated behind an OTP verify at the VM. */
    suspend fun setLinked(
        id: String,
        isLinked: Boolean,
    ) = dao.setLinked(id, isLinked, clock.now().toEpochMilliseconds())

    private fun PaymentWalletEntity.toModel(): PaymentWallet =
        PaymentWallet(
            id = id,
            providerName = providerName,
            mobile = mobile,
            isLinked = isLinked,
            balanceLabel = formatInr(balanceMinor),
        )

    private companion object {
        /** ₹ with two decimals + thousands separators — a small, locale-independent formatter. */
        fun formatInr(minor: Long): String {
            val whole = minor / 100
            val paise = (minor % 100).toInt()
            val grouped =
                whole.toString()
                    .reversed()
                    .chunked(3)
                    .joinToString(",")
                    .reversed()
            return "₹$grouped.${paise.toString().padStart(2, '0')}"
        }
    }
}
