package com.mileway.feature.tracking.repository

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Restores a track + its GPS points from a versioned [JsonExporter] export.
 *
 * Deliberately parses the envelope generically ([Json.parseToJsonElement]) instead of decoding
 * into `@Serializable` mirrors of the ~100-field [SavedTrack] / [LocationData] entities: the
 * exporter only writes a small subset of fields, so import reads exactly that subset and fills the
 * rest from the entities' own defaults. That keeps import decoupled from entity churn.
 *
 * Two guards, both mandatory (trust-boundary — imported JSON is untrusted input):
 *  - **account mismatch**: the export's `account` block must match [current]. A v1 export (no
 *    `account` block) is treated as unbound and allowed. Never silently import another persona's
 *    trips (mirrors the `started_by_*` ownership binding used elsewhere).
 *  - **dedupe**: if a track with the same `routeId` already exists ([trackExists]), skip it — a
 *    re-import is a no-op, not a duplicate or an overwrite.
 *
 * Collaborators are narrow suspend seams rather than the concrete repositories so this stays
 * `commonMain`-clean and trivially fakeable in tests; `:app` wires them to
 * [SavedTrackRepository.getByRouteId]/`insert` and [LocationRepository.insertBatch].
 */
class ImportRepository(
    private val trackExists: suspend (routeId: String) -> Boolean,
    private val insertTrack: suspend (SavedTrack) -> Unit,
    private val insertPoints: suspend (List<LocationData>) -> Unit,
) {
    /** The signed-in identity an import is checked against. */
    data class CurrentAccount(
        val accountId: String?,
        val tenant: String,
    )

    sealed interface ImportResult {
        /** Track restored with [pointCount] points. */
        data class Restored(
            val routeId: String,
            val pointCount: Int,
        ) : ImportResult

        /** A track with this routeId already existed; nothing written. */
        data class Skipped(
            val routeId: String,
        ) : ImportResult

        /** Export belongs to a different account/tenant than [CurrentAccount]; nothing written. */
        data class AccountMismatch(
            val exportAccountId: String?,
            val exportTenant: String,
        ) : ImportResult

        /** JSON was missing required fields or otherwise unparseable. */
        data class Malformed(
            val reason: String,
        ) : ImportResult
    }

    // Boundary catch-all: this is the edge between the app and a platform or backend call that
    // fails in ways no narrower Kotlin type covers on this source set. The failure is logged
    // and surfaced to the caller, never swallowed — crashing the process is the alternative.
    @Suppress("TooGenericExceptionCaught")
    suspend fun import(
        json: String,
        current: CurrentAccount,
    ): ImportResult {
        val root =
            try {
                Json.parseToJsonElement(json).jsonObject
            } catch (e: Exception) {
                return ImportResult.Malformed("Not a JSON object: ${e.message}")
            }

        // account-mismatch guard. v1 exports have no "account" block → unbound → allowed.
        root["account"]?.jsonObject?.let { acc ->
            val exportAccountId = acc["accountId"]?.jsonPrimitive?.contentOrNullSafe()
            val exportTenant = acc["tenant"]?.jsonPrimitive?.content ?: ""
            val mismatch = exportAccountId != current.accountId || exportTenant != current.tenant
            if (mismatch) return ImportResult.AccountMismatch(exportAccountId, exportTenant)
        }

        val trackObj = root["track"]?.jsonObject ?: return ImportResult.Malformed("Missing 'track' object")
        val routeId =
            trackObj["routeId"]?.jsonPrimitive?.contentOrNullSafe()
                ?: return ImportResult.Malformed("Missing track.routeId")

        // dedupe guard.
        if (trackExists(routeId)) return ImportResult.Skipped(routeId)

        val track = restoreTrack(trackObj, routeId, current) ?: return ImportResult.Malformed("Incomplete track fields")
        val points = (root["points"]?.jsonArray ?: emptyList()).map { restorePoint(it.jsonObject, routeId) }

        insertTrack(track)
        if (points.isNotEmpty()) insertPoints(points)
        return ImportResult.Restored(routeId, points.size)
    }

    private fun restoreTrack(
        o: JsonObject,
        routeId: String,
        current: CurrentAccount,
    ): SavedTrack? {
        val name = o["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return null
        return SavedTrack(
            routeId = routeId,
            name = name,
            // Re-bind restored data to the importing account so it can't masquerade as unowned.
            startedByAccountId = current.accountId,
            startedByTenant = current.tenant,
            distance = o.d("distanceM"),
            duration = o.l("durationMs"),
            startTime = o.l("startTime"),
            endTime = o.l("endTime"),
            startLatitude = o.d("startLat"),
            startLongitude = o.d("startLng"),
            endLatitude = o.d("endLat"),
            endLongitude = o.d("endLng"),
            pausedLatitude = 0.0,
            pausedLongitude = 0.0,
            avgSpeed = o.d("avgSpeedMps", -1.0),
            maxSpeed = o.d("maxSpeedMps", -1.0),
            selectedVehicleType = o["vehicleType"]?.jsonPrimitive?.contentOrNullSafe() ?: "NONE",
            isCompleted = o.b("isCompleted"),
            serverUploaded = o.b("serverUploaded"),
        )
    }

    private fun restorePoint(
        o: JsonObject,
        routeId: String,
    ): LocationData =
        LocationData(
            token = routeId,
            date = o.l("timestamp"),
            lat = o.d("lat"),
            lng = o.d("lng"),
            speed = o.f("speed"),
            accuracy = o.f("accuracy"),
            altitude = o.d("altitude"),
            bearing = o.f("bearing"),
            provider = o["provider"]?.jsonPrimitive?.contentOrNullSafe() ?: "NONE",
            activity = o["activity"]?.jsonPrimitive?.contentOrNullSafe() ?: "",
            displacement = o.d("displacement"),
            isMock = o.b("isMock"),
            isAbnormal = o.b("isAbnormal"),
            isPaused = o.b("isPaused"),
            batteryPercentage = o.d("batteryPct"),
            wasCheckInPoint = o.b("wasCheckIn"),
        )

    // ── tiny typed accessors (missing/JSON-null → default) ──────────────────────
    private fun JsonObject.d(
        k: String,
        default: Double = 0.0,
    ): Double = this[k]?.jsonPrimitive?.doubleOrNull ?: default

    private fun JsonObject.f(
        k: String,
        default: Float = 0f,
    ): Float = this[k]?.jsonPrimitive?.doubleOrNull?.toFloat() ?: default

    private fun JsonObject.l(
        k: String,
        default: Long = 0L,
    ): Long = this[k]?.jsonPrimitive?.longOrNull ?: default

    private fun JsonObject.b(
        k: String,
        default: Boolean = false,
    ): Boolean = this[k]?.jsonPrimitive?.booleanOrNull ?: default
}

/** kotlinx JSON `null` primitive has content "null"; treat it as absent. */
private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? = if (this is kotlinx.serialization.json.JsonNull) null else content
