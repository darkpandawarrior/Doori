package com.mileway.core.data.vehicle

import com.mileway.core.data.dao.VehicleAuditDao
import com.mileway.core.data.model.db.VehicleAuditEntity
import com.mileway.core.data.review.ReviewResult
import com.mileway.core.data.review.SimulatedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * PLAN_V24 P12.6 — the self-audit store derives its verdict live from the shared review simulator:
 * a submitted audit is Pending until the sim delay elapses, then Passed, or Failed when the note
 * carries the reject marker. Per-type checklists are also pinned.
 */
class SelfAuditRepositoryTest {
    private class MutableClock(var millis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private class FakeAuditDao : VehicleAuditDao {
        val rows = MutableStateFlow<List<VehicleAuditEntity>>(emptyList())

        override fun observeForVehicle(vehicleId: String): Flow<List<VehicleAuditEntity>> =
            rows.map { list -> list.filter { it.vehicleId == vehicleId }.sortedByDescending { it.submittedAtMs } }

        override suspend fun count(): Int = rows.value.size

        override suspend fun upsert(entity: VehicleAuditEntity) {
            rows.value = rows.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun deleteForVehicle(vehicleId: String) {
            rows.value = rows.value.filterNot { it.vehicleId == vehicleId }
        }
    }

    @Test
    fun `four-wheeler checklist adds seatbelts`() {
        assertTrue(SelfAuditChecklist.SEATBELTS in SelfAuditChecklist.forVehicleType("fourWheelerPetrol"))
        assertTrue(SelfAuditChecklist.SEATBELTS !in SelfAuditChecklist.forVehicleType("twoWheeler"))
    }

    @Test
    fun `a clean audit stays pending then passes after the delay`() =
        runTest {
            val clock = MutableClock(0)
            val repo = SelfAuditRepository(FakeAuditDao(), SimulatedReviewEngine(clock, simDelayMillis = 5_000), clock)
            repo.submit("veh_1", setOf("tyres", "lights"), note = "")

            assertEquals(ReviewResult.Pending, repo.observeForVehicle("veh_1").first().single().verdict)

            clock.millis = 5_000
            assertEquals(ReviewResult.Approved, repo.observeForVehicle("veh_1").first().single().verdict)
        }

    @Test
    fun `an audit noting an issue with the reject marker fails`() =
        runTest {
            val clock = MutableClock(0)
            val repo = SelfAuditRepository(FakeAuditDao(), SimulatedReviewEngine(clock, simDelayMillis = 5_000), clock)
            repo.submit("veh_1", setOf("tyres"), note = "reject: bald tyres")

            clock.millis = 5_000
            val verdict = repo.observeForVehicle("veh_1").first().single().verdict
            assertTrue(verdict is ReviewResult.Rejected)
            assertEquals("bald tyres", (verdict as ReviewResult.Rejected).reason)
        }
}
