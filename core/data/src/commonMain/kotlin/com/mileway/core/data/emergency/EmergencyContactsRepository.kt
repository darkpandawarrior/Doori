package com.mileway.core.data.emergency

import com.mileway.core.data.dao.EmergencyContactDao
import com.mileway.core.data.model.db.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** The most emergency contacts the reference app allows a user to register. */
const val MAX_EMERGENCY_CONTACTS = 5

/**
 * PLAN_V24 P3.5: Room-backed store for emergency contacts, capped at [MAX_EMERGENCY_CONTACTS].
 * Lives in core:data so both the profile management screen (feature:profile) and the SOS sheet
 * (feature:tracking) can reach it without a feature→feature dependency.
 */
class EmergencyContactsRepository(private val dao: EmergencyContactDao, private val clock: Clock = Clock.System) {
    /** Live, creation-ordered list of the user's emergency contacts. */
    fun observeAll(): Flow<List<EmergencyContact>> = dao.observeAll().map { rows -> rows.map { it.toContact() } }

    /**
     * Inserts a new contact (blank [id]) or updates an existing one. Adding a brand-new contact
     * fails (returns false) once [MAX_EMERGENCY_CONTACTS] are stored; editing an existing one is
     * always allowed. Callers validate name/phone before calling — this only enforces the cap.
     */
    suspend fun save(
        id: String,
        name: String,
        phoneNo: String,
        countryCode: String,
    ): Boolean {
        val isNew = id.isBlank()
        if (isNew && dao.count() >= MAX_EMERGENCY_CONTACTS) return false
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            EmergencyContactEntity(
                id = id.ifBlank { "EC-" + now.toString().takeLast(10) },
                name = name,
                phoneNo = phoneNo,
                countryCode = countryCode,
                createdAtMs = now,
            ),
        )
        return true
    }

    /** Deletes [id] outright. */
    suspend fun delete(id: String) = dao.delete(id)

    private fun EmergencyContactEntity.toContact(): EmergencyContact = EmergencyContact(id = id, name = name, phoneNo = phoneNo, countryCode = countryCode)
}
