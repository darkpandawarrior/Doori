package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.SignatureDao
import com.mileway.core.data.model.db.SignatureEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P12.7: reads/writes the profile's digital-signature PNG path from the Room-backed
 * [SignatureDao] singleton row. The bitmap itself is rasterised and saved to the app files dir by
 * the platform screen; only the file path is persisted here. `null` means no signature on file.
 */
class SignatureRepository(private val dao: SignatureDao, private val clock: Clock = Clock.System) {
    /** Live signature PNG path, or null while none is on file. */
    fun observe(): Flow<String?> = dao.observe().map { it?.imagePath }

    suspend fun save(imagePath: String) {
        dao.upsert(SignatureEntity(imagePath = imagePath, updatedAtMs = clock.now().toEpochMilliseconds()))
    }

    suspend fun clear() = dao.clear()
}
