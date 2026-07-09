package com.mileway

import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.model.db.MockAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for the shared [MockAccountDao] (P1.1/P1.2) — lets tests construct
 * `MockAccountRepository`/`AuthViewModel` without a Room instance. Duplicated into androidTest
 * (the JVM `src/test` copy is not visible to the instrumented source set).
 */
class FakeMockAccountDao : MockAccountDao {
    private val rows = MutableStateFlow<Map<String, MockAccountEntity>>(emptyMap())

    override fun observeAll(): Flow<List<MockAccountEntity>> = rows.map { it.values.sortedBy { row -> row.createdAtMs } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun getById(accountId: String): MockAccountEntity? = rows.value[accountId]

    override suspend fun upsert(account: MockAccountEntity) {
        rows.value = rows.value + (account.accountId to account)
    }

    override suspend fun upsertAll(accounts: List<MockAccountEntity>) {
        rows.value = rows.value + accounts.associateBy { it.accountId }
    }

    override suspend fun delete(accountId: String) {
        rows.value = rows.value - accountId
    }

    override suspend fun clearActive() {
        rows.value = rows.value.mapValues { it.value.copy(isActive = false) }
    }

    override suspend fun markActive(accountId: String) {
        val existing = rows.value[accountId] ?: return
        rows.value = rows.value + (accountId to existing.copy(isActive = true))
    }

    override suspend fun setActive(accountId: String) {
        clearActive()
        markActive(accountId)
    }
}
