package com.mileway

import com.mileway.core.data.dao.ReferralTxnDao
import com.mileway.core.data.model.db.ReferralTxnEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [ReferralTxnDao] (PLAN_V24 P5.1) — lets JVM/Robolectric tests construct
 * `ReferralProgramRepository`/`ReferralHubViewModel` without a Room instance. A relaxed mockk would
 * return a null Flow the ViewModel's `init` collector dereferences, so a real fake is required.
 */
class FakeReferralTxnDao : ReferralTxnDao {
    private val rows = MutableStateFlow<Map<String, ReferralTxnEntity>>(emptyMap())

    override fun observeAll(): Flow<List<ReferralTxnEntity>> =
        rows.map { it.values.sortedByDescending { row -> row.submittedAtMillis } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun get(id: String): ReferralTxnEntity? = rows.value[id]

    override suspend fun upsertAll(entities: List<ReferralTxnEntity>) {
        rows.value = rows.value + entities.associateBy { it.id }
    }

    override suspend fun upsert(entity: ReferralTxnEntity) {
        rows.value = rows.value + (entity.id to entity)
    }
}
