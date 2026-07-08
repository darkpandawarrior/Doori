package com.mileway

import com.mileway.core.data.dao.CouponDao
import com.mileway.core.data.model.db.CouponEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [CouponDao] (PLAN_V24 P5.2) — lets JVM/Robolectric tests construct
 * `CouponsRepository`/`CouponsViewModel` without a Room instance. A relaxed mockk would return a
 * null Flow the ViewModel's `init` collector dereferences, so a real fake is required.
 */
class FakeCouponDao : CouponDao {
    private val rows = MutableStateFlow<Map<String, CouponEntity>>(emptyMap())

    override fun observeAll(): Flow<List<CouponEntity>> = rows.map { it.values.sortedBy { row -> row.status } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun findByCode(code: String): CouponEntity? =
        rows.value.values.firstOrNull { it.code.equals(code, ignoreCase = true) }

    override suspend fun upsertAll(entities: List<CouponEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: CouponEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
