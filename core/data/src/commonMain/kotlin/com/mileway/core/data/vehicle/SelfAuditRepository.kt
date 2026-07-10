package com.mileway.core.data.vehicle

import com.mileway.core.data.dao.VehicleAuditDao
import com.mileway.core.data.model.db.VehicleAuditEntity
import com.mileway.core.data.review.ReviewResult
import com.mileway.core.data.review.SimulatedReviewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** The checklist item keys a driver confirms during a self-audit (the reference inspection set). */
object SelfAuditChecklist {
    const val TYRES = "tyres"
    const val LIGHTS = "lights"
    const val HORN = "horn"
    const val CLEANLINESS = "cleanliness"
    const val DOCUMENTS = "documents"
    const val SEATBELTS = "seatbelts"

    private val base = listOf(TYRES, LIGHTS, HORN, CLEANLINESS, DOCUMENTS)

    /** Seeded per vehicle type: four-wheelers add a seatbelts check the base (two-wheeler) set omits. */
    fun forVehicleType(vehicleTypeKey: String): List<String> = if (vehicleTypeKey.startsWith("fourWheeler")) base + SEATBELTS else base
}

/** Domain view of a submitted audit; [verdict] is resolved live by [SimulatedReviewEngine]. */
data class VehicleAudit(
    val id: String,
    val vehicleId: String,
    val submittedAtMs: Long,
    val checkedItems: Set<String>,
    val note: String,
    val verdict: ReviewResult,
)

/**
 * PLAN_V24 P12.6: the vehicle self-audit store. Lives in core:data next to [GarageRepository] since
 * it is vehicle-domain and needs the shared [SimulatedReviewEngine]. Pure/offline over Room. The
 * verdict is derived at read time (Pending until the sim delay elapses, then Passed/Failed by the
 * reject marker in the note) — so navigating back into the audit history resolves pending items.
 *
 * ponytail: no live ticker — a Pending verdict flips to a decided one on the next observe (screen
 * re-entry), matching the engine's "resolves next time you look" contract; a poller can be added if
 * a live countdown is ever wanted.
 */
class SelfAuditRepository(
    private val dao: VehicleAuditDao,
    private val engine: SimulatedReviewEngine,
    private val clock: Clock = Clock.System,
) {
    fun observeForVehicle(vehicleId: String): Flow<List<VehicleAudit>> = dao.observeForVehicle(vehicleId).map { rows -> rows.map { it.toDomain() } }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun submit(
        vehicleId: String,
        checkedItems: Set<String>,
        note: String,
    ): String {
        val id = "audit_${Uuid.random()}"
        dao.upsert(
            VehicleAuditEntity(
                id = id,
                vehicleId = vehicleId,
                submittedAtMs = clock.now().toEpochMilliseconds(),
                checkedItemsCsv = checkedItems.joinToString(","),
                note = note,
            ),
        )
        return id
    }

    private fun VehicleAuditEntity.toDomain(): VehicleAudit =
        VehicleAudit(
            id = id,
            vehicleId = vehicleId,
            submittedAtMs = submittedAtMs,
            checkedItems = checkedItemsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
            note = note,
            verdict = engine.resolve(submittedAtMs, note),
        )
}
