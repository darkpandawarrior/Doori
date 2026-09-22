package com.mileway.feature.tracking.model

import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.display.TrackDisplayData

/**
 * What a [SavedTrack] is submitted/linked against, if anything.
 *
 * Parity §2.1/§3 Wave 3: "linked context" generalizes the reference app's per-flow linkage
 * (trip / itinerary / petty-cash / event) into one typed model, read off the existing
 * [SavedTrack] linkage columns — no new persistence. This lives in feature:tracking for now;
 * it's the natural seam for a future core:booking module once trip/booking data grows beyond
 * a couple of ID columns.
 */
sealed interface LinkedContextKind {
    data class Voucher(
        val number: String,
    ) : LinkedContextKind

    data class Trip(
        val id: String,
    ) : LinkedContextKind

    data class Booking(
        val id: String,
    ) : LinkedContextKind

    data class Event(
        val id: String,
    ) : LinkedContextKind
}

data class LinkedContext(
    val kind: LinkedContextKind,
    val label: String,
    val value: String,
)

/**
 * Maps a [SavedTrack]'s existing linkage fields to a single typed [LinkedContext], or null when
 * none of them are populated.
 *
 * Precedence when more than one linkage is present, most "final"/settled state wins: a track
 * already claimed by a voucher is showing its actual filed destination, so that outranks the
 * trip/itinerary/petty-cash context it originated from.
 *   Voucher > Trip > Booking (itinerary) > Event (petty cash)
 *
 * [tripId]/[tripV2Id] are the v1/v2 columns for the same trip linkage (never both meaningfully
 * distinct here); tripId wins when both are populated.
 */
fun SavedTrack.toLinkedContext(): LinkedContext? =
    linkedContextOf(
        claimedByVoucherNumber = claimedByVoucherNumber,
        tripId = tripId,
        tripV2Id = tripV2Id,
        itineraryId = itineraryId,
        pettyId = pettyId,
    )

/**
 * Same mapping as [SavedTrack.toLinkedContext], for the hub screen which renders off
 * [TrackDisplayData] (the flattened display model) rather than the raw entity.
 */
fun TrackDisplayData.toLinkedContext(): LinkedContext? =
    linkedContextOf(
        claimedByVoucherNumber = claimedByVoucherNumber,
        tripId = tripId,
        tripV2Id = tripV2Id,
        itineraryId = itineraryId,
        pettyId = pettyId,
    )

private fun linkedContextOf(
    claimedByVoucherNumber: String?,
    tripId: String?,
    tripV2Id: String?,
    itineraryId: String?,
    pettyId: Long,
): LinkedContext? {
    val trip = tripId?.takeIf { it.isNotBlank() } ?: tripV2Id?.takeIf { it.isNotBlank() }
    return when {
        !claimedByVoucherNumber.isNullOrBlank() ->
            LinkedContext(LinkedContextKind.Voucher(claimedByVoucherNumber), "Voucher", claimedByVoucherNumber)
        trip != null ->
            LinkedContext(LinkedContextKind.Trip(trip), "Trip", trip)
        !itineraryId.isNullOrBlank() ->
            LinkedContext(LinkedContextKind.Booking(itineraryId), "Booking", itineraryId)
        pettyId >= 0L ->
            LinkedContext(LinkedContextKind.Event(pettyId.toString()), "Petty Cash", "PC-$pettyId")
        else -> null
    }
}
