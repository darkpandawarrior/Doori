package com.mileway.core.data.model.network

// PLAN_V33 A1: the wire DTOs below moved to :contract, keeping their original package name so
// every existing `import com.mileway.core.data.model.network.*` still resolves unchanged (core:data
// re-exports via `api(project(":contract"))`). `LogMilesService` — a client-side domain mapping
// built from `LogMilesServiceDto`, not itself a wire DTO — stayed behind in core:data (see that
// module's own `model/network/NetworkModels.kt`).

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Coordinates ──────────────────────────────────────────────────────────────

@Serializable
data class CoordsV2(
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lng") val lng: Double? = null,
    @SerialName("name") val name: String? = null,
)

@Serializable
data class CoordsPayload(
    @SerialName("coords") val coords: List<CoordsV2>? = null,
)

// ── Vehicles ──────────────────────────────────────────────────────────────────

@Serializable
data class ApprovedVehicle(
    @SerialName("key") val vehicleKey: String? = null,
    @SerialName("value") val vehicleName: String? = null,
    @SerialName("pricing") val vehiclePricing: Double? = null,
)

@Serializable
data class PolicyApprovedVehiclesResponse(
    @SerialName("items") val vehicles: List<ApprovedVehicle> = emptyList(),
)

@Serializable
data class ApprovedVehiclePricingResponse(
    @SerialName("data") val data: Map<String, Double> = emptyMap(),
)

// ── Submission response ───────────────────────────────────────────────────────

@Serializable
data class ExpenseSubmissionResponse(
    @SerialName("status") val status: Int = 0,
    @SerialName("policyViolationsList") val policyViolations: List<PolicyViolationItem>? = emptyList(),
    @SerialName("reimbursableAmount") val reimbursableAmount: Double? = null,
    @SerialName("amount") val amount: Double? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("voucher") val voucher: VoucherInfo? = null,
    @SerialName("distance") val distance: Double = 0.0,
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("transId") val transId: String? = null,
    @SerialName("submissionStatus") val submissionStatus: SubmissionStatus = SubmissionStatus.SUCCESS,
    @SerialName("violations") val violations: List<PolicyViolation> = emptyList(),
    @SerialName("issuedVoucher") val issuedVoucher: Voucher? = null,
    @SerialName("transaction") val transaction: TransactionRef? = null,
)

@Serializable
data class PolicyViolationItem(
    @SerialName("error") val error: String? = null,
)

@Serializable
data class VoucherInfo(
    @SerialName("transId") val transId: String? = null,
    @SerialName("id") val id: Long? = null,
    @SerialName("purpose") val purpose: String? = null,
    @SerialName("title") val title: String? = null,
)

// ── Submission policy outcome ─────────────────────────────────────────────────
//
// These types are re-exported under `com.mileway.core.network.model` (see
// core:network model/PolicyModels.kt). They are defined here because
// ExpenseSubmissionResponse embeds them and the module graph points
// core:network -> core:data -> :contract.

/** Overall outcome of a mileage submission as evaluated by the policy engine. */
@Serializable
enum class SubmissionStatus {
    SUCCESS,
    NEEDS_APPROVAL,
    REIMBURSABLE_ADJUSTED,
    POLICY_VIOLATION,
    HARD_STOP,
}

/** Severity of a single policy violation attached to a submission. */
@Serializable
enum class ViolationSeverity {
    REIMBURSABLE,
    VIOLATION,
    HARDSTOP,
}

@Serializable
data class PolicyViolation(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("severity") val severity: ViolationSeverity = ViolationSeverity.VIOLATION,
)

/** Declaration text the user must acknowledge before a voucher can be filed. */
@Serializable
data class VoucherDeclaration(
    @SerialName("text") val text: String = "",
    @SerialName("requiresAcknowledgement") val requiresAcknowledgement: Boolean = true,
)

@Serializable
enum class VoucherStatus {
    UNCLAIMED,
    FILED,
    CREATED,
}

@Serializable
data class Voucher(
    @SerialName("id") val id: Long = 0L,
    @SerialName("number") val number: String = "",
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("status") val status: VoucherStatus = VoucherStatus.UNCLAIMED,
)

/** Reference to the ledger transaction created for a submission. */
@Serializable
data class TransactionRef(
    @SerialName("id") val id: String = "",
    @SerialName("createdAtMillis") val createdAtMillis: Long = 0L,
    @SerialName("amount") val amount: Double = 0.0,
    @SerialName("serviceTag") val serviceTag: String = "",
)

// ── Track status ──────────────────────────────────────────────────────────────

@Serializable
data class TrackMileageStatusResponse(
    @SerialName("statusCode") val statusCode: Int = 0,
    @SerialName("description") val description: String = "",
) {
    fun isActive(): Boolean = statusCode == 200

    fun isCancelledDueToConfig(): Boolean = statusCode == 505

    fun isDeactivatedByUser(): Boolean = statusCode == 504
}

// ── Log Miles services ────────────────────────────────────────────────────────

@Serializable
data class LogMilesServicesResponse(
    @SerialName("services") val services: List<LogMilesServiceDto>? = null,
)

@Serializable
data class LogMilesServiceDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("glCode") val glCode: String? = null,
)

// ── Log Miles routes ──────────────────────────────────────────────────────────

@Serializable
data class LogMilesRoutesResponse(
    @SerialName("location") val locations: List<LogMilesRouteGroup> = emptyList(),
)

@Serializable
data class LogMilesRouteGroup(
    @SerialName("coords") val coords: List<CoordsV2> = emptyList(),
    @SerialName("count") val count: Int = 0,
)

// ── Distance ──────────────────────────────────────────────────────────────────

@Serializable
data class DistanceRequestV2(
    @SerialName("coords") val coords: List<CoordsV2>,
)

@Serializable
data class DistanceResponseV2(
    @SerialName("distance") val distance: Double = 0.0,
    @SerialName("unit") val unit: String = "km",
)

// ── User config ───────────────────────────────────────────────────────────────

@Serializable
data class UserConfigResponseV2(
    @SerialName("currency") val currency: String = "INR",
    @SerialName("miles") val miles: Boolean = false,
    @SerialName("logMiles") val logMiles: Boolean = false,
    @SerialName("mileage") val mileage: MileageConfig? = null,
    @SerialName("isOdometerMandatory") val isOdometerMandatory: Boolean = false,
    @SerialName("geoCheckIn") val geoCheckIn: Boolean = false,
    @SerialName("autoDiscardTrackMileage") val autoDiscardTrackMileage: Boolean = false,
    @SerialName("calculateDistanceOnBackend") val calculateDistanceOnBackend: Boolean = false,
    @SerialName("trackMilesV2") val trackMilesV2: Boolean = true,
    @SerialName("draftTrackMiles") val draftTrackMiles: Boolean = true,
    @SerialName("multiServiceLogMiles") val multiServiceLogMiles: Boolean = false,
    @SerialName("profile") val profile: UserProfile? = null,
)

@Serializable
data class MileageConfig(
    @SerialName("time") val time: Int = 10,
    @SerialName("distance") val distance: Double = 0.0,
)

@Serializable
data class UserProfile(
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("tenant") val tenant: String = "",
    @SerialName("currency") val currency: String = "INR",
)
