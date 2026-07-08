package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SubscriptionDao
import com.mileway.core.data.model.db.ActiveSubscriptionEntity
import com.mileway.core.data.model.db.SubscriptionPlanEntity
import com.mileway.core.data.subscription.SubscriptionRepository
import com.mileway.core.data.subscription.SubscriptionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

private const val NOW_MS = 1_700_000_000_000L
private const val MONTH_MS = 30L * 86_400_000L

private class SubFixedClock(private val ms: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(ms)
}

/**
 * PLAN_V24 P6.2: covers [SubscriptionRepository] — seeding, mock purchase, cancel-keeps-access,
 * renew and upgrade.
 */
class SubscriptionRepositoryTest {
    private fun repo(dao: FakeSubscriptionDao = FakeSubscriptionDao()) = SubscriptionRepository(dao, SubFixedClock(NOW_MS))

    @Test
    fun `seedIfEmpty seeds the plans once`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val first = r.observePlans().first().size
            r.seedIfEmpty()
            val second = r.observePlans().first().size

            assertEquals(3, first)
            assertEquals(3, second)
        }

    @Test
    fun `purchase creates an active subscription with a one-period window`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            val monthly = r.observePlans().first().first { it.id == "commuter" }

            r.purchase(monthly.id)
            val active = r.observeActive().first()

            assertNotNull(active)
            assertEquals("commuter", active.planId)
            assertEquals(SubscriptionStatus.ACTIVE, active.status)
            assertEquals(NOW_MS, active.startedAtMs)
            assertEquals(NOW_MS + MONTH_MS, active.renewsAtMs)
            assertFalse(active.cancelAtPeriodEnd)
        }

    @Test
    fun `cancel keeps access until period end`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            r.purchase("commuter")

            r.cancel()
            val active = r.observeActive().first()

            assertNotNull(active)
            // Access is retained: still ACTIVE with the same renewal date, only auto-renew suppressed.
            assertEquals(SubscriptionStatus.ACTIVE, active.status)
            assertEquals(NOW_MS + MONTH_MS, active.renewsAtMs)
            assertTrue(active.cancelAtPeriodEnd)
        }

    @Test
    fun `renew extends the window and clears a pending cancel`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            r.purchase("commuter")
            r.cancel()

            r.renew()
            val active = r.observeActive().first()

            assertNotNull(active)
            assertFalse(active.cancelAtPeriodEnd)
            assertEquals(NOW_MS + 2 * MONTH_MS, active.renewsAtMs)
        }

    @Test
    fun `upgrade swaps the plan on the existing subscription`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            r.purchase("commuter")

            r.upgrade("pro")
            val active = r.observeActive().first()

            assertNotNull(active)
            assertEquals("pro", active.planId)
            assertEquals(SubscriptionStatus.ACTIVE, active.status)
        }
}

/** In-memory fake for [SubscriptionDao] — mirrors the app-test fake shape. */
private class FakeSubscriptionDao : SubscriptionDao {
    private val plans = MutableStateFlow<Map<String, SubscriptionPlanEntity>>(emptyMap())
    private val active = MutableStateFlow<ActiveSubscriptionEntity?>(null)

    override fun observePlans(): Flow<List<SubscriptionPlanEntity>> = plans.map { it.values.sortedBy { row -> row.tierRank } }

    override suspend fun planCount(): Int = plans.value.size

    override suspend fun getPlan(id: String): SubscriptionPlanEntity? = plans.value[id]

    override suspend fun upsertPlans(entities: List<SubscriptionPlanEntity>) {
        plans.value = plans.value + entities.associateBy { it.id }
    }

    override fun observeActive(): Flow<ActiveSubscriptionEntity?> = active

    override suspend fun getActive(): ActiveSubscriptionEntity? = active.value

    override suspend fun upsertActive(entity: ActiveSubscriptionEntity) {
        active.value = entity
    }

    override suspend fun clearActive() {
        active.value = null
    }
}
