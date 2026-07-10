package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P11.3: the per-account "Head home" destination state. One row per account (PK
 * [accountId]) — the active destination a persona is heading toward, its expiry budget, and the
 * (preference-only) selected region set. Modelled on the reference app's driver destination-ride +
 * region-preference shapes, rebuilt as local Room data (no backend).
 *
 * [expiresAt] is the epoch-ms instant the head-home budget runs out; `null` means "no active
 * destination". The live countdown is `expiresAt − now`, floored at 0 (auto-expire). [placeId]/
 * [address]/[lat]/[lng] snapshot the chosen saved place so the panel renders without re-reading it.
 * [selectedRegionsCsv] is a comma-separated set of region ids — a pure preference store today (chip
 * row only, no routing engine — see PROGRESS ceiling note).
 */
@Entity(tableName = "destination_mode")
data class DestinationModeEntity(
    @PrimaryKey
    val accountId: String,
    val placeId: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val expiresAt: Long? = null,
    val selectedRegionsCsv: String = "",
)
