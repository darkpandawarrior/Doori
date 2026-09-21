package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.PassportDetailsDao
import com.mileway.core.data.model.db.PassportDetailsEntity
import com.mileway.feature.profile.model.PassportDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * P6.2: reads/writes the profile's linked [PassportDetails] from the Room-backed
 * [PassportDetailsDao] singleton row. `null` means no passport has been added yet.
 */
class PassportDetailsRepository(
    private val dao: PassportDetailsDao,
    private val clock: Clock = Clock.System,
) {
    /** Live passport details, or null while none is on file. */
    fun observe(): Flow<PassportDetails?> = dao.observe().map { it?.toPassportDetails() }

    suspend fun save(details: PassportDetails) {
        dao.upsert(
            PassportDetailsEntity(
                passportNumber = details.passportNumber,
                issuingCountry = details.issuingCountry,
                expiryDateMillis = details.expiryDateMillis,
                updatedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun PassportDetailsEntity.toPassportDetails(): PassportDetails =
        PassportDetails(
            passportNumber = passportNumber,
            issuingCountry = issuingCountry,
            expiryDateMillis = expiryDateMillis,
        )
}
