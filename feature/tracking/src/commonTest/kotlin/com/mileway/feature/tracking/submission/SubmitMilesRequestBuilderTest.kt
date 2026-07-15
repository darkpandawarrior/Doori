package com.mileway.feature.tracking.submission

import com.mileway.core.data.model.db.LocationData
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.model.state.TrackMilesPluginConfig
import com.mileway.feature.tracking.viewmodel.SubmissionFormUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** PLAN_V33 C5: [SubmitMilesRequestBuilder] field-by-field coverage. */
class SubmitMilesRequestBuilderTest {
    private fun track(
        tripId: String? = null,
        tripV2Id: String? = null,
        itineraryId: String? = null,
        pettyId: Long = -1L,
        officeId: Long? = null,
        entityId: Long? = null,
    ) = SavedTrack(
        routeId = "route-1",
        name = "trip",
        startLatitude = 0.0,
        startLongitude = 0.0,
        endLatitude = 0.0,
        endLongitude = 0.0,
        pausedLatitude = 0.0,
        pausedLongitude = 0.0,
        startTime = 0L,
        endTime = 0L,
        distance = 1.0,
        duration = 1L,
        tripId = tripId,
        tripV2Id = tripV2Id,
        itineraryId = itineraryId,
        pettyId = pettyId,
        officeId = officeId,
        entityId = entityId,
    )

    private fun point(
        lat: Double,
        lng: Double,
    ) = LocationData(activity = "Stationary", speed = 0f, lat = lat, lng = lng, token = "route-1", batteryPercentage = 100.0)

    private fun build(
        form: SubmissionFormUi = SubmissionFormUi(),
        track: SavedTrack? = null,
        routePoints: List<LocationData> = emptyList(),
    ) = SubmitMilesRequestBuilder.build(
        routeId = "route-1",
        vehicleKey = "fourWheelerPetrol",
        distanceKm = 12.5,
        startTime = 100L,
        endTime = 200L,
        submissionTime = 300L,
        form = form,
        track = track,
        routePoints = routePoints,
    )

    // ── Fields already set today keep working ──────────────────────────────────

    @Test
    fun `preserves the fields already set today`() {
        val form = SubmissionFormUi(roundTrip = true)
        val request = build(form = form)

        assertEquals("route-1", request.token)
        assertEquals("fourWheelerPetrol", request.vehicleType)
        assertEquals(12.5, request.distance, 1e-9)
        assertEquals(100L, request.startTime)
        assertEquals(200L, request.endTime)
        assertEquals(300L, request.submissionTime)
        assertEquals(true, request.roundTrip)
    }

    // ── odometer-not-working -> violationRemarks (not notes) ──────────────────

    @Test
    fun `odometer-not-working fallback appends the marker to violationRemarks not notes`() {
        val form =
            SubmissionFormUi(
                config = TrackMilesPluginConfig(calculateExpenseViaOdometer = true),
                odometerNotWorking = true,
            )
        val request = build(form = form)

        assertEquals(true, request.odometerNotWorking)
        assertEquals("ODOMETER_NOT_WORKING", request.violationRemarks)
        assertNull(request.notes)
        assertEquals(true, request.milesAmountByOdometer)
    }

    @Test
    fun `fallback not active leaves violationRemarks and milesAmountByOdometer unset`() {
        val request = build(form = SubmissionFormUi())
        assertNull(request.violationRemarks)
        assertEquals(false, request.milesAmountByOdometer)
    }

    // ── odometer reading zeroing while the fallback is active ─────────────────

    @Test
    fun `zeroes odometer readings while the fallback is active even if stale readings exist`() {
        val form =
            SubmissionFormUi(
                config = TrackMilesPluginConfig(calculateExpenseViaOdometer = true),
                odometerNotWorking = true,
                simulatedStartOdo = 45_000,
                simulatedEndOdo = 45_050,
            )
        val request = build(form = form)

        assertEquals("0", request.startReading)
        assertEquals("0", request.endReading)
    }

    @Test
    fun `passes through the real odometer readings when the fallback is not active`() {
        val form = SubmissionFormUi(simulatedStartOdo = 45_000, simulatedEndOdo = 45_050)
        val request = build(form = form)

        assertEquals("45000", request.startReading)
        assertEquals("45050", request.endReading)
    }

    // ── startLabel/endLabel OCR/MANUAL/NA normalization ────────────────────────

    @Test
    fun `label is OCR when a reading was captured and not flagged manual`() {
        val form = SubmissionFormUi(simulatedStartOdo = 45_000, simulatedEndOdo = 45_050, isManualStartOdo = false, isManualEndOdo = false)
        val request = build(form = form)
        assertEquals("OCR", request.startLabel)
        assertEquals("OCR", request.endLabel)
    }

    @Test
    fun `label is MANUAL when the reading was flagged manual`() {
        val form = SubmissionFormUi(simulatedStartOdo = 45_000, simulatedEndOdo = 45_050, isManualStartOdo = true, isManualEndOdo = true)
        val request = build(form = form)
        assertEquals("MANUAL", request.startLabel)
        assertEquals("MANUAL", request.endLabel)
    }

    @Test
    fun `label is dropped (null, not the literal NA) when no reading was captured`() {
        val request = build(form = SubmissionFormUi())
        assertNull(request.startLabel)
        assertNull(request.endLabel)
    }

    @Test
    fun `label is dropped when the odometer fallback is active even if a stale reading exists`() {
        val form =
            SubmissionFormUi(
                config = TrackMilesPluginConfig(calculateExpenseViaOdometer = true),
                odometerNotWorking = true,
                simulatedStartOdo = 45_000,
                simulatedEndOdo = 45_050,
            )
        val request = build(form = form)
        assertNull(request.startLabel)
        assertNull(request.endLabel)
    }

    // ── itineraryV2Id split (v1/v2), resolved into the DTO's itineraryId field ─

    @Test
    fun `prefers tripV2Id over itineraryId when tripV2Id is present`() {
        val request = build(track = track(tripV2Id = "v2-123", itineraryId = "v1-456"))
        assertEquals("v2-123", request.itineraryId)
    }

    @Test
    fun `falls back to itineraryId when tripV2Id is blank or null`() {
        assertEquals("v1-456", build(track = track(tripV2Id = null, itineraryId = "v1-456")).itineraryId)
        assertEquals("v1-456", build(track = track(tripV2Id = "", itineraryId = "v1-456")).itineraryId)
    }

    @Test
    fun `itineraryId is null when neither id exists (no track)`() {
        assertNull(build(track = null).itineraryId)
    }

    // ── coords: origin/destination from the trip's first/last GPS point ───────

    @Test
    fun `builds origin and destination from the first and last route points`() {
        val points = listOf(point(1.0, 2.0), point(3.0, 4.0), point(5.0, 6.0))
        val request = build(routePoints = points)

        assertEquals(1.0, request.origin?.lat)
        assertEquals(2.0, request.origin?.lng)
        assertEquals(5.0, request.destination?.lat)
        assertEquals(6.0, request.destination?.lng)
    }

    @Test
    fun `origin and destination are null when there are no route points`() {
        val request = build(routePoints = emptyList())
        assertNull(request.origin)
        assertNull(request.destination)
    }

    // ── trip/petty/office/entity id resolution from the persisted trip row ────

    @Test
    fun `resolves tripId, petty, office and entity ids from the persisted trip row`() {
        val request = build(track = track(tripId = "trip-9", pettyId = 42L, officeId = 7L, entityId = 3L))
        assertEquals("trip-9", request.tripId)
        assertEquals(42L, request.petty)
        assertEquals(7L, request.officeId)
        assertEquals(3L, request.entityId)
    }

    @Test
    fun `drops the sentinel -1 pettyId to null instead of sending it`() {
        val request = build(track = track(pettyId = -1L))
        assertNull(request.petty)
    }
}
