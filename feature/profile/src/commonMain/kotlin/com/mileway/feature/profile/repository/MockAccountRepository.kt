package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.MockAccountDao
import com.mileway.core.data.model.db.MockAccountEntity
import com.mileway.core.network.model.DemoAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * P1.2: reads the switchable-persona list from the shared, Room-backed [MockAccountDao] (P1.1)
 * instead of the static `stub.ProfileMockData.accounts()` list. [DemoAccount] stays the
 * screen-facing model `ProfileScreen`'s `PersonaSwitcherRow` already renders — this repository
 * only changes where that list is sourced from, not its shape. On first run (empty table) it
 * seeds the same personas `ProfileMockData.accounts()` used to hardcode, so existing behavior is
 * preserved (`AgentRepository`'s `seedIfEmpty()` pattern from PLAN_V20 P1.2, also used by
 * `VoucherHistoryRepository` in PLAN_V21 P3.1).
 */
class MockAccountRepository(private val dao: MockAccountDao, private val clock: Clock = Clock.System) {
    /** Live, DAO-ordered (`createdAtMs ASC`) list of switchable personas. */
    fun observeAll(): Flow<List<DemoAccount>> = dao.observeAll().map { rows -> rows.map { it.toDemoAccount() } }

    /** One-shot snapshot — used where a suspend call, not a Flow, is needed. */
    suspend fun accounts(): List<DemoAccount> = observeAll().first()

    /** Marks [accountId] as the sole active persona (atomic clear-then-set in the DAO). */
    suspend fun setActive(accountId: String) = dao.setActive(accountId)

    /** Seeds the original demo personas if the shared table is empty (first run only). */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsertAll(demoSeed(now))
    }

    /**
     * P1.3: adds a new, non-active persona (its `accountId` is generated, not caller-supplied, so
     * the switcher row always has a stable, collision-free key). Never active on creation —
     * callers must switch to it explicitly via [setActive].
     */
    suspend fun add(
        displayName: String,
        employeeCode: String,
        organization: String,
    ) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            MockAccountEntity(
                accountId = "ACC-" + now.toString().takeLast(8),
                displayName = displayName,
                employeeCode = employeeCode,
                organization = organization,
                avatarSeed = displayName,
                isActive = false,
                lastLoginAtMs = now,
                createdAtMs = now,
            ),
        )
    }

    /** Removes [accountId] outright. Guard rules (can't remove active/last) live in the ViewModel. */
    suspend fun remove(accountId: String) = dao.delete(accountId)

    private fun demoSeed(nowMs: Long): List<MockAccountEntity> =
        listOf(
            MockAccountEntity(
                accountId = "ACC-001",
                displayName = "Demo User",
                employeeCode = "EMP001",
                organization = "Demo Logistics Pvt Ltd",
                avatarSeed = "ACC-001",
                isActive = true,
                lastLoginAtMs = nowMs,
                createdAtMs = nowMs,
                // P1.6: ACC-001 + ACC-003 share this number → duplicate-account resolution on login.
                phone = "+919876543210",
            ),
            MockAccountEntity(
                accountId = "ACC-002",
                displayName = "Demo User (Sandbox)",
                employeeCode = "EMP001-SBX",
                organization = "Demo Sandbox Workspace",
                avatarSeed = "ACC-002",
                isActive = false,
                lastLoginAtMs = nowMs,
                createdAtMs = nowMs + 1,
                phone = "+919000000002",
            ),
            MockAccountEntity(
                accountId = "ACC-003",
                displayName = "QA Tester",
                employeeCode = "QA042",
                organization = "Demo QA Workspace",
                avatarSeed = "ACC-003",
                isActive = false,
                lastLoginAtMs = nowMs,
                createdAtMs = nowMs + 2,
                phone = "+919876543210",
            ),
        )

    private fun MockAccountEntity.toDemoAccount(): DemoAccount =
        DemoAccount(
            id = accountId,
            displayName = displayName,
            employeeCode = employeeCode,
            organization = organization,
            isActive = isActive,
            lastLoginAtMs = lastLoginAtMs,
            createdAtMs = createdAtMs,
            phone = phone,
        )
}
