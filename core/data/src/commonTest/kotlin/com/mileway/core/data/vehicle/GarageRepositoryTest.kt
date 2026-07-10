package com.mileway.core.data.vehicle

import com.mileway.core.data.dao.VehicleDao
import com.mileway.core.data.model.db.VehicleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLAN_V24 P11.2: covers [GarageRepository]'s branching logic — the single-active invariant, the
 * first-vehicle-becomes-active rule, and active promotion when the active vehicle is removed.
 */
class GarageRepositoryTest {
    private class FakeVehicleDao : VehicleDao {
        val rows = MutableStateFlow<List<VehicleEntity>>(emptyList())

        override fun observeAll(): Flow<List<VehicleEntity>> = rows

        override suspend fun getAll(): List<VehicleEntity> = rows.value

        override suspend fun get(id: String): VehicleEntity? = rows.value.firstOrNull { it.id == id }

        override fun observeActive(): Flow<VehicleEntity?> = rows.map { list -> list.firstOrNull { it.isActive } }

        override suspend fun getActive(): VehicleEntity? = rows.value.firstOrNull { it.isActive }

        override suspend fun count(): Int = rows.value.size

        override suspend fun upsert(entity: VehicleEntity) {
            rows.value = rows.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun delete(id: String) {
            rows.value = rows.value.filterNot { it.id == id }
        }

        override suspend fun clearActive() {
            rows.value = rows.value.map { it.copy(isActive = false) }
        }

        override suspend fun markActive(id: String) {
            rows.value = rows.value.map { it.copy(isActive = it.id == id) }
        }
    }

    private fun vehicle(id: String) =
        GarageVehicle(
            id = id,
            brand = "Honda",
            model = "Activa",
            registrationNumber = "MH12$id",
            year = 2022,
            color = "Grey",
            seats = 2,
            vehicleTypeKey = "twoWheeler",
            photoUri = "",
            isActive = false,
            services = setOf(VehicleServices.COMMUTE),
            availability = null,
            createdAtMs = 0L,
        )

    @Test
    fun `first vehicle added becomes active`() =
        runTest {
            val repo = GarageRepository(FakeVehicleDao())
            repo.add(vehicle("A"))
            assertEquals("A", repo.getActive()?.id)
        }

    @Test
    fun `setActive is exclusive`() =
        runTest {
            val dao = FakeVehicleDao()
            val repo = GarageRepository(dao)
            repo.add(vehicle("A"))
            repo.add(vehicle("B"))
            repo.setActive("B")
            assertEquals("B", repo.getActive()?.id)
            assertEquals(1, dao.rows.value.count { it.isActive })
        }

    @Test
    fun `removing the active vehicle promotes another`() =
        runTest {
            val repo = GarageRepository(FakeVehicleDao())
            repo.add(vehicle("A"))
            repo.add(vehicle("B"))
            repo.setActive("A")
            repo.remove("A")
            assertEquals("B", repo.getActive()?.id)
        }

    @Test
    fun `removing the last vehicle leaves none active`() =
        runTest {
            val repo = GarageRepository(FakeVehicleDao())
            repo.add(vehicle("A"))
            repo.remove("A")
            assertNull(repo.getActive())
        }

    @Test
    fun `seedIfEmpty seeds once with a single active vehicle`() =
        runTest {
            val repo = GarageRepository(FakeVehicleDao())
            repo.seedIfEmpty()
            repo.seedIfEmpty()
            assertEquals(2, repo.count())
            assertTrue(repo.getActive() != null)
        }
}
