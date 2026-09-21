package com.mileway.feature.profile.repository

import com.mileway.core.data.dao.VehicleDetailsDao
import com.mileway.core.data.model.db.VehicleDetailsEntity
import com.mileway.feature.profile.model.VehicleDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * P6.2: reads/writes the profile's linked [VehicleDetails] from the Room-backed
 * [VehicleDetailsDao] singleton row. `null` means no vehicle has been added yet.
 */
class VehicleDetailsRepository(
    private val dao: VehicleDetailsDao,
    private val clock: Clock = Clock.System,
) {
    /** Live vehicle details, or null while none is on file. */
    fun observe(): Flow<VehicleDetails?> = dao.observe().map { it?.toVehicleDetails() }

    suspend fun save(details: VehicleDetails) {
        dao.upsert(
            VehicleDetailsEntity(
                make = details.make,
                model = details.model,
                registrationNumber = details.registrationNumber,
                fuelType = details.fuelType,
                seatingCapacity = details.seatingCapacity,
                updatedAtMs = clock.now().toEpochMilliseconds(),
            ),
        )
    }

    private fun VehicleDetailsEntity.toVehicleDetails(): VehicleDetails =
        VehicleDetails(
            make = make,
            model = model,
            registrationNumber = registrationNumber,
            fuelType = fuelType,
            seatingCapacity = seatingCapacity,
        )
}
