package com.mileway.core.data.outbox

import com.mileway.core.data.model.network.SubmitMilesRequestK
import com.siddharth.kmp.offlineoutbox.SubmitOutbox
import kotlinx.serialization.Serializable

// PLAN_V33 A5: durable trip-submission payload enqueued into SubmitOutbox when a trip is submitted.
// Wraps the already-@Serializable SubmitMilesRequestK in full (not a reduced projection) so no
// submission detail (roundTrip, odometerNotWorking, notes, forms, ...) is lost between enqueue and
// the eventual network call. routeId doubles as the outbox uniqueKey: re-submitting the same trip
// upserts (latest-write-wins) instead of enqueueing a duplicate draft — see MilesSubmitSyncer.
@Serializable
data class TripDraft(
    val routeId: String,
    val request: SubmitMilesRequestK,
)

typealias TripDraftOutbox = SubmitOutbox<TripDraft>
