package com.mileway

import com.mileway.core.data.dao.PaymentWalletDao
import com.mileway.core.data.model.db.PaymentWalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [PaymentWalletDao] (P8.1) — lets JVM/Robolectric tests construct
 * `WalletRepository`/`ConnectedAccountsViewModel` without a Room instance, mirroring
 * [FakeConnectedAccountDao]'s shape.
 */
class FakePaymentWalletDao : PaymentWalletDao {
    private val rows = MutableStateFlow<Map<String, PaymentWalletEntity>>(emptyMap())

    override fun observeAll(): Flow<List<PaymentWalletEntity>> = rows.map { it.values.sortedBy { row -> row.providerName } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun upsertAll(entities: List<PaymentWalletEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun setLinked(
        id: String,
        isLinked: Boolean,
        updatedAtMs: Long,
    ) {
        val existing = rows.value[id] ?: return
        rows.value = rows.value + (id to existing.copy(isLinked = isLinked, updatedAtMs = updatedAtMs))
    }
}
