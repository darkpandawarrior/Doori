package com.mileway.core.data.model.network

// PLAN_V33 A1: physically lives in :contract now (shared client+server wire DTOs, kotlinx only —
// no android.*/java.*/Room/Ktor deps), but keeps its original package name so every existing
// `import com.mileway.core.data.model.network.*` across the repo still resolves unchanged.
// core:data re-exports it via `api(project(":contract"))` — zero import edits, smallest green diff.

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Track miles event (start / stop / discard) ────────────────────────────────

@Serializable
data class PostMileageEventRequestK(
    @SerialName("token") val token: String,
    @SerialName("device") val device: String = "DEMO",
    @SerialName("model") val model: String = "DEMO",
    @SerialName("version") val version: String = "1.0.0",
    @SerialName("origin") val origin: CoordsV2? = null,
    @SerialName("event") val eventType: String? = null,
    @SerialName("tag") val tag: String? = null,
    @SerialName("timestamp") val timestamp: Long = 0L,
    @SerialName("lat") val latitude: Double? = null,
    @SerialName("lng") val longitude: Double? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("activity") val activity: String? = null,
    @SerialName("battery") val battery: Double? = null,
)

// ── Track miles submission ────────────────────────────────────────────────────

@Serializable
data class SubmitMilesRequestK(
    @SerialName("token") val token: String? = null,
    @SerialName("vehicleType") val vehicleType: String? = "NONE",
    @SerialName("start") val start: String? = null,
    @SerialName("startLabel") val startLabel: String? = null,
    @SerialName("startReading") val startReading: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("endLabel") val endLabel: String? = null,
    @SerialName("endReading") val endReading: String? = null,
    @SerialName("origin") val origin: CoordsV2? = null,
    @SerialName("destination") val destination: CoordsV2? = null,
    @SerialName("distance") val distance: Double = 0.0,
    @SerialName("originalDistance") val originalDistance: Double = 0.0,
    @SerialName("forms") val forms: Map<Long, String> = emptyMap(),
    @SerialName("processor") val processor: Map<Long, String> = emptyMap(),
    @SerialName("device") val device: String = "DEMO",
    @SerialName("model") val model: String = "DEMO",
    @SerialName("provider") val provider: String? = null,
    @SerialName("locationV2") val locationV2: Boolean = true,
    @SerialName("notes") val notes: String? = null,
    @SerialName("startTime") val startTime: Long? = null,
    @SerialName("endTime") val endTime: Long? = null,
    @SerialName("journeyEndTime") val journeyEndTime: Long? = null,
    @SerialName("submissionTime") val submissionTime: Long? = null,
    @SerialName("draftTrackMiles") val draftTrackMiles: Boolean? = null,
    @SerialName("milesAmountByOdometer") val milesAmountByOdometer: Boolean = false,
    @SerialName("files") val files: List<String>? = emptyList(),
    @SerialName("version") val version: String = "1.0.0",
    @SerialName("roundTrip") val roundTrip: Boolean = false,
    @SerialName("force") val force: Long? = null,
    @SerialName("petty") val petty: Long? = null,
    @SerialName("date") val date: Long? = null,
    @SerialName("time") val time: Long? = null,
    @SerialName("tripId") val tripId: String? = null,
    @SerialName("tripV2Id") val tripV2Id: String? = null,
    @SerialName("itineraryId") val itineraryId: String? = null,
    @SerialName("odometerDistance") val odometerDistance: Double? = null,
    @SerialName("odometerNotWorking") val odometerNotWorking: Boolean = false,
    @SerialName("violationRemarks") val violationRemarks: String? = null,
    @SerialName("employees") val employees: List<String>? = emptyList(),
    @SerialName("officeId") val officeId: Long? = null,
    @SerialName("entityId") val entityId: Long? = null,
    @SerialName("mjpId") val mjpId: String? = null,
    @SerialName("mjpItemId") val mjpItemId: Long? = null,
)

// ── Location sync requests ────────────────────────────────────────────────────

@Serializable
data class LocationPayload(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("token") val token: String,
    @SerialName("date") val date: Long,
    @SerialName("speed") val speed: Float = 0f,
    @SerialName("activity") val activity: String = "",
    @SerialName("isMock") val isMock: Boolean = false,
    @SerialName("isAbnormal") val isAbnormal: Boolean = false,
    @SerialName("displacement") val displacement: Double = 0.0,
    @SerialName("accuracy") val accuracy: Float = 0f,
)

@Serializable
data class LocationRequest(
    @SerialName("data") val data: LocationPayload,
)

@Serializable
data class BulkLocationRequest(
    @SerialName("data") val data: List<LocationPayload>,
)

@Serializable
data class EventPayload(
    @SerialName("token") val token: String,
    @SerialName("event") val event: String,
    @SerialName("time") val time: Long,
    @SerialName("audience") val audience: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lng") val lng: Double? = null,
)

@Serializable
data class EventRequest(
    @SerialName("data") val data: EventPayload,
)

@Serializable
data class BulkEventRequest(
    @SerialName("data") val data: List<EventPayload>,
)

// ── V2 sync requests/responses ────────────────────────────────────────────────

@Serializable
data class LocationPayloadV2(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("token") val token: String,
    @SerialName("date") val date: Long,
    @SerialName("speed") val speed: Float = 0f,
    @SerialName("activity") val activity: String = "",
    @SerialName("isMock") val isMock: Boolean = false,
    @SerialName("isAbnormal") val isAbnormal: Boolean = false,
    @SerialName("displacement") val displacement: Double = 0.0,
    @SerialName("accuracy") val accuracy: Float = 0f,
    @SerialName("provider") val provider: String? = null,
)

@Serializable
data class LocationRequestV2(
    @SerialName("data") val data: LocationPayloadV2,
)

@Serializable
data class BulkLocationRequestV2(
    @SerialName("data") val data: List<LocationPayloadV2>,
)

@Serializable
data class EventPayloadV2(
    @SerialName("token") val token: String,
    @SerialName("event") val event: String,
    @SerialName("time") val time: Long,
    @SerialName("eventType") val eventType: String? = null,
    @SerialName("audience") val audience: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lng") val lng: Double? = null,
    @SerialName("metadata") val metadata: String? = null,
)

@Serializable
data class EventRequestV2(
    @SerialName("data") val data: EventPayloadV2,
)

@Serializable
data class BulkEventRequestV2(
    @SerialName("data") val data: List<EventPayloadV2>,
)

@Serializable
data class LocationResponseV2(
    @SerialName("data") val data: List<LocationPayloadV2> = emptyList(),
)

@Serializable
data class EventResponseV2(
    @SerialName("data") val data: List<EventPayloadV2> = emptyList(),
)

// ── Check-in models ───────────────────────────────────────────────────────────

@Serializable
data class CheckInRequestV2(
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("typeId") val typeId: Long? = null,
)

@Serializable
data class CheckInDetailsResponseV2(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("radius") val radius: Double = 0.0,
)

@Serializable
data class SubmittedCheckInResponseV2(
    @SerialName("checkIns") val checkIns: List<CheckInItem> = emptyList(),
)

@Serializable
data class CheckInItem(
    @SerialName("id") val id: Long = 0,
    @SerialName("time") val time: Long = 0,
)

@Serializable
data class AllTypesResponseV2(
    @SerialName("types") val types: List<CheckInDetailsResponseV2> = emptyList(),
)

// ── Map / address ─────────────────────────────────────────────────────────────

@Serializable
data class MapResponse(
    @SerialName("address") val address: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lng") val lng: Double? = null,
)

// ── Expense tagging ───────────────────────────────────────────────────────────

@Serializable
data class TaggedExpenseItem(
    @SerialName("id") val id: Long = 0,
    @SerialName("title") val title: String = "",
    @SerialName("amount") val amount: Double = 0.0,
)

@Serializable
data class AllTaggedExpenseResponse(
    @SerialName("data") val data: List<TaggedExpenseItem> = emptyList(),
)

// ── Success wrappers ──────────────────────────────────────────────────────────

@Serializable
data class SuccessResponseV2(
    @SerialName("status") val status: String = "SUCCESS",
)

@Serializable
data class EmptyRequest(val _empty: String? = null)

// ── Log Miles submit ──────────────────────────────────────────────────────────

@Serializable
data class LogMilesSubmitRequestV2(
    @SerialName("vehicleType") val vehicleType: String? = null,
    @SerialName("distance") val distance: Double = 0.0,
    @SerialName("date") val date: Long? = null,
    @SerialName("origin") val origin: CoordsV2? = null,
    @SerialName("destination") val destination: CoordsV2? = null,
    @SerialName("files") val files: List<String>? = emptyList(),
    @SerialName("forms") val forms: Map<Long, String> = emptyMap(),
    @SerialName("roundTrip") val roundTrip: Boolean = false,
    @SerialName("employees") val employees: List<String> = emptyList(),
    @SerialName("notes") val notes: String? = null,
    @SerialName("serviceId") val serviceId: Long? = null,
    @SerialName("invoiceDate") val invoiceDate: Long? = null,
    /** end-minus-start odometer reading, in km, when odometer capture (P5.3) was completed. */
    @SerialName("odometerDistance") val odometerDistance: Int? = null,
    /**
     * Set on a resubmit after the user resolved a policy violation (P5.4) — mirrors the
     * `submitMiles`/`PolicyViolationSheet` "Ask Authorities" resolution's intent, so the same
     * request shape carries a forced-through submission for the tracking pipeline too.
     */
    @SerialName("force") val force: Boolean? = null,
    /** User-entered remarks explaining a `POLICY_VIOLATION`-tier resubmit (P5.4); required by the
     * dialog before resubmit is enabled, carried through so the policy engine has an audit trail. */
    @SerialName("violationRemarks") val violationRemarks: String? = null,
)

@Serializable
data class LogMilesRequestV2(
    @SerialName("vehicleType") val vehicleType: String? = null,
    @SerialName("distance") val distance: Double = 0.0,
)

@Serializable
data class LogMilesResponseV2(
    @SerialName("limit") val limit: Double? = null,
    @SerialName("limitPeriod") val limitPeriod: String? = null,
)
