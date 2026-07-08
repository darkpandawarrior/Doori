package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P3.5: a single emergency contact (the reference app/the reference app `EmergencyContact{id,name,phoneNo,
 * countryCode}`). Multi-row table, capped at 5 by the repository. Consumed by both the profile
 * management screen and the tracking SOS sheet, so it lives in core:data (shared) rather than a
 * feature module.
 */
@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phoneNo: String,
    val countryCode: String,
    val createdAtMs: Long,
)
