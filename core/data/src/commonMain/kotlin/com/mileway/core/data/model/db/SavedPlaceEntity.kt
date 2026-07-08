package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P3.4: a saved place (home/work/other) — label + free-text address + optional
 * coordinates. Multi-row table (unlike the single-row passport/vehicle stores), same shape as
 * [DelegationEntity]. [type] is a `SavedPlaceType` name (HOME/WORK/OTHER); the HOME row is the
 * canonical home location surfaced alongside `EmployeeProfile.homeLocation`.
 *
 * [latitude]/[longitude] are nullable: coordinates are optional (manual entry — no map picker in
 * this phase), so a place can carry only a text address.
 */
@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey
    val id: String,
    val type: String,
    val label: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val updatedAtMs: Long,
)
