package com.mileway

import com.mileway.core.data.dao.SignatureDao
import com.mileway.core.data.model.db.SignatureEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for [SignatureDao] (P12.7) — lets JVM/Robolectric screenshot tests construct
 * `SignatureRepository`/`SignatureViewModel` without a Room instance. A relaxed mockk would hand back
 * a null-backed Flow and crash `SignatureViewModel`'s `combine` collector, so a real MutableStateFlow
 * is used (memory: screenshot Koin needs deterministic fakes), mirroring [FakePassportDetailsDao].
 */
class FakeSignatureDao : SignatureDao {
    private val row = MutableStateFlow<SignatureEntity?>(null)

    override fun observe(id: String): Flow<SignatureEntity?> = row

    override suspend fun upsert(entity: SignatureEntity) {
        row.value = entity
    }

    override suspend fun clear(id: String) {
        row.value = null
    }
}
