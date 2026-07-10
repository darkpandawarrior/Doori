package com.mileway.core.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PLAN_V24 P12.7: the profile's digital signature. Single-row table by design, same pattern as
 * [PassportDetailsEntity] / [VehicleDetailsEntity]. [imagePath] points at the rasterised signature
 * PNG saved to the app files dir; the row is deleted when the user clears their signature.
 */
@Entity(tableName = "signature")
data class SignatureEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    val imagePath: String,
    val updatedAtMs: Long,
) {
    companion object {
        const val SINGLETON_ID: String = "signature_singleton"
    }
}
