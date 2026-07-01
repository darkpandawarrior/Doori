package com.mileway.core.data.outbox

import kotlinx.serialization.Serializable

// Durable trip payload enqueued into SubmitOutbox when a tracking session ends.
// Serialized to JSON for storage in submit_drafts.payloadJson.
@Serializable
data class TripDraft(
    val routeId: String,
    val distanceKm: Double,
    val vehicleKey: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val locationsJson: String = "[]",
)
