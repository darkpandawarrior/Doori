package com.mileway.core.data.outbox

import kotlinx.serialization.Serializable

// Wave-4 §2.3: durable payload for the location-sync outbox. Carries only point ids (refs into
// the `locations` table) — the actual point rows never leave Room, mirroring the reference app's
// batching contract without a real upload.
@Serializable
data class LocationBatch(
    val token: String,
    val pointIds: List<Long>,
)

typealias LocationBatchOutbox = SubmitOutbox<LocationBatch>
