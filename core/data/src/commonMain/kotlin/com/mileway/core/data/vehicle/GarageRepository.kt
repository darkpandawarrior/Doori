package com.mileway.core.data.vehicle

import com.mileway.core.data.dao.VehicleDao
import com.mileway.core.data.model.db.VehicleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * PLAN_V24 P11.2: the multi-vehicle garage store. Lives in core:data (not a feature module) because
 * both the profile garage screen AND tracking's active-vehicle default read it. Pure/offline over
 * Room — no backend. Exactly one vehicle is active at a time (enforced by [VehicleDao.setActive]).
 */
class GarageRepository(
    private val dao: VehicleDao,
    private val clock: Clock = Clock.System,
) {
    fun observeAll(): Flow<List<GarageVehicle>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActive(): Flow<GarageVehicle?> = dao.observeActive().map { it?.toDomain() }

    suspend fun getActive(): GarageVehicle? = dao.getActive()?.toDomain()

    suspend fun count(): Int = dao.count()

    /** Seeds a starter garage once (mirrors the other repos' seed-once idiom). */
    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            VehicleEntity(
                id = "veh_seed_1",
                brand = "Honda",
                model = "Activa",
                registrationNumber = "MH12AB1234",
                year = 2022,
                color = "Grey",
                seats = 2,
                vehicleTypeKey = "twoWheeler",
                isActive = true,
                servicesCsv = "${VehicleServices.COMMUTE},${VehicleServices.DELIVERY}",
                createdAtMs = now,
            ),
        )
        dao.upsert(
            VehicleEntity(
                id = "veh_seed_2",
                brand = "Maruti Suzuki",
                model = "Swift",
                registrationNumber = "MH12CD5678",
                year = 2021,
                color = "White",
                seats = 5,
                vehicleTypeKey = "fourWheelerPetrol",
                isActive = false,
                servicesCsv = "${VehicleServices.COMMUTE},${VehicleServices.BUSINESS}",
                createdAtMs = now + 1,
            ),
        )
        dao.setActive("veh_seed_1")
    }

    /** Adds a vehicle. The first vehicle added becomes active automatically. */
    suspend fun add(vehicle: GarageVehicle) {
        val firstOne = dao.count() == 0
        dao.upsert(vehicle.toEntity(active = firstOne || vehicle.isActive))
        if (firstOne || vehicle.isActive) dao.setActive(vehicle.id)
    }

    suspend fun update(vehicle: GarageVehicle) = dao.upsert(vehicle.toEntity(active = vehicle.isActive))

    suspend fun remove(id: String) {
        val wasActive = dao.getActive()?.id == id
        dao.delete(id)
        // Keep exactly one active vehicle: if the active one was removed, promote the first remaining.
        if (wasActive) dao.getAll().firstOrNull()?.let { dao.setActive(it.id) }
    }

    suspend fun setActive(id: String) = dao.setActive(id)

    suspend fun setServices(
        id: String,
        services: Set<String>,
    ) {
        val current = dao.get(id) ?: return
        dao.upsert(current.copy(servicesCsv = services.joinToString(",")))
    }

    suspend fun setAvailability(
        id: String,
        window: AvailabilityWindow?,
    ) {
        val current = dao.get(id) ?: return
        dao.upsert(
            current.copy(
                availabilityStartMinute = window?.startMinute ?: -1,
                availabilityEndMinute = window?.endMinute ?: -1,
                availabilityRatePerHour = window?.ratePerHour ?: -1.0,
            ),
        )
    }

    private fun GarageVehicle.toEntity(active: Boolean): VehicleEntity =
        VehicleEntity(
            id = id,
            brand = brand,
            model = model,
            registrationNumber = registrationNumber,
            year = year,
            color = color,
            seats = seats,
            vehicleTypeKey = vehicleTypeKey,
            photoUri = photoUri,
            isActive = active,
            servicesCsv = services.joinToString(","),
            availabilityStartMinute = availability?.startMinute ?: -1,
            availabilityEndMinute = availability?.endMinute ?: -1,
            availabilityRatePerHour = availability?.ratePerHour ?: -1.0,
            createdAtMs = if (createdAtMs > 0) createdAtMs else clock.now().toEpochMilliseconds(),
        )
}
