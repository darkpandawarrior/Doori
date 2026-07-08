package com.mileway.feature.profile.repository

import com.mileway.core.data.coupon.CouponApplyResult
import com.mileway.core.data.coupon.CouponStatus
import com.mileway.core.data.dao.CouponDao
import com.mileway.core.data.model.db.CouponEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * PLAN_V24 P5.2: covers [CouponsRepository]'s seed and the four apply-code outcomes
 * (success/invalid/expired/already-used), plus case-insensitive matching.
 */
class CouponsRepositoryTest {
    private fun repo(dao: FakeCouponDao = FakeCouponDao()) = CouponsRepository(dao)

    @Test
    fun `seedIfEmpty seeds the catalogue once`() =
        runTest {
            val dao = FakeCouponDao()
            val r = repo(dao)
            r.seedIfEmpty()
            val first = r.observeAll().first().size
            r.seedIfEmpty()
            val second = r.observeAll().first().size

            assertEquals(6, first)
            assertEquals(6, second)
        }

    @Test
    fun `an active code applies and flips the coupon to redeemed`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            assertEquals(CouponApplyResult.SUCCESS, r.applyCode("WELCOME50"))
            val coupon = r.observeAll().first().single { it.code == "WELCOME50" }
            assertEquals(CouponStatus.REDEEMED, coupon.status)
        }

    @Test
    fun `matching is case-insensitive`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()
            assertEquals(CouponApplyResult.SUCCESS, r.applyCode("fuel10"))
        }

    @Test
    fun `unknown, expired and already-used codes return the right outcomes`() =
        runTest {
            val r = repo()
            r.seedIfEmpty()

            assertEquals(CouponApplyResult.INVALID, r.applyCode("NOPE"))
            assertEquals(CouponApplyResult.EXPIRED, r.applyCode("SUMMER20"))
            assertEquals(CouponApplyResult.ALREADY_USED, r.applyCode("FIRSTRIDE"))
        }
}

/** In-memory fake for [CouponDao] — mirrors the app-test fake shape. */
private class FakeCouponDao : CouponDao {
    private val rows = MutableStateFlow<Map<String, CouponEntity>>(emptyMap())

    override fun observeAll(): Flow<List<CouponEntity>> = rows.map { it.values.sortedBy { row -> row.status } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun findByCode(code: String): CouponEntity? = rows.value.values.firstOrNull { it.code.equals(code, ignoreCase = true) }

    override suspend fun upsertAll(entities: List<CouponEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: CouponEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
