package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.model.db.MockAccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * P1.2: [MockAccountRepository] now reads switchable personas from [MockAccountDao] instead of
 * the static `stub.ProfileMockData.accounts()` list. This proves that behavior against an
 * in-memory fake DAO (no Room needed on the JVM) — the same shape `MockAccountDaoTest`
 * (`core:data`) uses for the DAO's own contract.
 */
class MockAccountRepositoryTest {
    @Test
    fun `seedIfEmpty populates the table exactly once`() =
        runTest {
            val dao = FakeMockAccountDao()
            val repository = MockAccountRepository(dao)

            repository.seedIfEmpty()
            val firstSeed = repository.accounts()
            repository.seedIfEmpty()
            val secondSeed = repository.accounts()

            assertEquals(3, firstSeed.size, "expected 3 seeded demo personas")
            assertEquals(firstSeed, secondSeed, "seeding twice must not duplicate rows")
        }

    @Test
    fun `accounts list survives a fresh repository instance backed by the same dao`() =
        runTest {
            // Acceptance: proves accounts() is now reading from the DAO, not a static object —
            // a brand-new MockAccountRepository wrapping the *same* dao instance sees the same
            // data a prior repository instance wrote, which a static-list-backed accessor never would.
            val dao = FakeMockAccountDao()
            MockAccountRepository(dao).seedIfEmpty()

            val accounts = MockAccountRepository(dao).accounts()

            assertEquals(3, accounts.size)
            assertTrue(accounts.any { it.employeeCode == "EMP001" })
        }

    @Test
    fun `observeAll maps entities to DemoAccount preserving createdAtMs order`() =
        runTest {
            val dao = FakeMockAccountDao()
            val repository = MockAccountRepository(dao)
            repository.seedIfEmpty()

            val accounts = repository.observeAll().first()

            assertEquals(listOf("ACC-001", "ACC-002", "ACC-003"), accounts.map { it.id })
        }

    @Test
    fun `setActive marks exactly one account active via the dao`() =
        runTest {
            val dao = FakeMockAccountDao()
            val repository = MockAccountRepository(dao)
            repository.seedIfEmpty()

            repository.setActive("ACC-002")

            assertEquals(true, dao.getById("ACC-002")?.isActive)
            assertEquals(
                listOf("ACC-002"),
                dao
                    .observeAll()
                    .first()
                    .filter { it.isActive }
                    .map { it.accountId },
            )
        }

    @Test
    fun `add persists a new, non-active persona`() =
        runTest {
            val dao = FakeMockAccountDao()
            val repository = MockAccountRepository(dao)
            repository.seedIfEmpty()

            repository.add("New Persona", "EMP999", "New Org")
            val accounts = repository.accounts()

            assertEquals(4, accounts.size)
            // Locate by employeeCode, not position: add() stamps createdAtMs from the wall clock, which
            // on a fast machine lands in the same millisecond as the last seeded persona (nowMs+2), so
            // the createdAtMs-ordered list has no stable "last" — asserting on accounts.last() flaked.
            val added = accounts.first { it.employeeCode == "EMP999" }
            assertEquals("New Persona", added.displayName)
            assertEquals("EMP999", added.employeeCode)
            assertEquals(false, added.isActive)
        }

    @Test
    fun `remove deletes exactly the requested persona`() =
        runTest {
            val dao = FakeMockAccountDao()
            val repository = MockAccountRepository(dao)
            repository.seedIfEmpty()

            repository.remove("ACC-002")
            val accounts = repository.accounts()

            assertEquals(2, accounts.size)
            assertTrue(accounts.none { it.id == "ACC-002" })
        }
}

/** In-memory fake mirroring [MockAccountDao]'s semantics — see `MockAccountDaoTest` in core:data. */
private class FakeMockAccountDao : MockAccountDao {
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
