@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.mileway.feature.tracking.route

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mileway.core.data.domain.policy.AutoClassificationRules
import com.mileway.core.data.domain.policy.ClassificationDecision
import com.mileway.core.data.domain.policy.ClassificationHours
import com.mileway.core.data.domain.policy.PolicyEngine
import com.mileway.core.data.domain.policy.TripClassification
import com.mileway.core.data.domain.policy.classifyTrip
import com.mileway.core.data.model.db.FavouriteRouteEntity
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.data.model.db.SavedTrack
import com.mileway.core.data.session.ActiveAccountSource
import com.mileway.core.network.routing.OsrmClient
import com.mileway.core.network.routing.OsrmConfiguration
import com.mileway.core.network.routing.RouteEstimate
import com.mileway.feature.tracking.places.SavedPlacesRepository
import com.mileway.feature.tracking.repository.SavedTrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class PointToPointState(
    val busy: Boolean = false,
    val estimate: RouteEstimate? = null,
    val returnAlreadyRecorded: Boolean = false,
    val decision: ClassificationDecision? = null,
    val savedRouteId: String? = null,
    val message: String? = null,
)

/** Routes only after an explicit request; persists reviewable drafts in the existing trip model. */
class PointToPointViewModel(
    val places: SavedPlacesRepository,
    private val tracks: SavedTrackRepository,
    private val accounts: ActiveAccountSource,
    private val osrm: OsrmClient,
    private val configuration: OsrmConfiguration,
    private val policy: PolicyEngine = PolicyEngine(emptyList()),
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {
    private val mutableState = MutableStateFlow(PointToPointState())
    val state = mutableState.asStateFlow()
    private var routing: Job? = null
    private var quotedInput: RouteInput? = null
    private var quotedAccountId: String? = null

    /** Editing an input invalidates the old quote, including any in-flight response. */
    fun invalidate() {
        routing?.cancel()
        quotedInput = null
        quotedAccountId = null
        mutableState.value = PointToPointState()
    }

    fun estimate(input: RouteInput) {
        invalidate()
        routing =
            viewModelScope.launch {
                mutableState.value = PointToPointState(busy = true)
                try {
                    val at = now()
                    val accountId = accounts.activeAccountId.first()
                    val recorded = tracks.rawTracksFlow().first().filter { it.startedByAccountId == accountId }
                    val origin = places.routePoint(input.origin)
                    val destination = places.routePoint(input.destination)
                    val paired = origin != null && destination != null && RoundTripGuard.hasRecordedReturn(origin, destination, at, recorded)
                    configuration.baseUrl = input.server
                    val estimate =
                        if (origin == null || destination == null) {
                            RouteEstimate.ManualRequired("Coordinates are missing; enter distance manually (approximate)")
                        } else {
                            osrm.route(origin, destination, input.roundTrip && !paired)
                        }
                    val rows = places.savedPlaces.first()
                    val rules =
                        AutoClassificationRules(
                            vehicles = input.vehicleRule?.let { mapOf(input.vehicleKey to it) }.orEmpty(),
                            places =
                                rows
                                    .mapNotNull { row ->
                                        when (row.type) {
                                            "HOME" -> row.id to TripClassification.PERSONAL
                                            "WORK" -> row.id to TripClassification.BUSINESS
                                            else -> null
                                        }
                                    }.toMap(),
                            hours = if (input.useWorkingHours) ClassificationHours(9, 18, TripClassification.BUSINESS) else null,
                        )
                    val last =
                        recorded
                            .filter { it.isCompleted && !it.isDiscarded && it.endTime <= at }
                            .maxByOrNull { it.endTime }
                            ?.notes
                            ?.let { value -> TripClassification.entries.firstOrNull { it.name == value } }
                    val hour =
                        kotlin.time.Instant
                            .fromEpochMilliseconds(at)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .hour
                    val decision = policy.classifyTrip(rules, input.vehicleKey, listOf(input.destination.id, input.origin.id), hour, last)
                    quotedInput = input
                    quotedAccountId = accountId
                    mutableState.value = PointToPointState(estimate = estimate, returnAlreadyRecorded = paired, decision = decision)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.value = PointToPointState(message = "Could not read trip data; try again")
                }
            }
    }

    /** Manual distance is the total entered by the user, never doubled silently. */
    fun save(
        input: RouteInput,
        manualTotalKm: Double?,
        override: TripClassification? = null,
        favourite: Boolean = false,
    ) {
        if (mutableState.value.busy || mutableState.value.savedRouteId != null) return
        val quote = mutableState.value
        mutableState.value = quote.copy(busy = true, message = null)
        viewModelScope.launch {
            var savedId: String? = null
            try {
                require(quotedInput == input) { "Estimate this route after editing it" }
                val km = (quote.estimate as? RouteEstimate.Routed)?.distanceKm ?: manualTotalKm
                require(km != null && km.isFinite() && km > 0.0 && (km * 1_000.0).isFinite()) { "Enter a positive total distance (approximate)" }
                val at = now()
                val accountId = accounts.activeAccountId.first()
                require(!accountId.isNullOrBlank()) { "Select an active account before saving" }
                require(accountId == quotedAccountId) { "Account changed; estimate again" }
                val recorded = tracks.rawTracksFlow().first().filter { it.startedByAccountId == accountId }
                val origin = places.routePoint(input.origin)
                val destination = places.routePoint(input.destination)
                val paired = origin != null && destination != null && RoundTripGuard.hasRecordedReturn(origin, destination, at, recorded)
                require(!input.roundTrip || quote.returnAlreadyRecorded == paired) { "Return-leg data changed; estimate again" }
                val roundTrip = input.roundTrip && !paired
                val classification = override ?: quote.decision?.classification
                val approximate = quote.estimate !is RouteEstimate.Routed
                val id = Uuid.random().toString()
                val name = if (approximate) "Manual route (approximate)" else "OSRM route estimate"
                tracks.insert(
                    SavedTrack(
                        routeId = id,
                        name = name,
                        isDraft = true,
                        draftSavedAt = at,
                        startedByAccountId = accountId,
                        startLatitude = origin?.latitude ?: 0.0,
                        startLongitude = origin?.longitude ?: 0.0,
                        endLatitude = if (roundTrip) origin?.latitude ?: 0.0 else destination?.latitude ?: 0.0,
                        endLongitude = if (roundTrip) origin?.longitude ?: 0.0 else destination?.longitude ?: 0.0,
                        pausedLatitude = 0.0,
                        pausedLongitude = 0.0,
                        startTime = at,
                        endTime = at,
                        distance = km * 1_000.0,
                        duration = 0L,
                        createdAt = at,
                        selectedVehicleType = input.vehicleKey,
                        roundTrip = roundTrip,
                        notes = classification?.name ?: "-",
                        violationRemarks = if (approximate) "MANUAL_APPROXIMATE" else "OSRM_ESTIMATE",
                    ),
                )
                savedId = id
                if (favourite) places.pin(FavouriteRouteEntity(id, id, name, classification?.name ?: "", km, at))
                mutableState.value = quote.copy(savedRouteId = id, message = "Route draft saved")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = quote.copy(savedRouteId = savedId, message = failure.message ?: "Could not save route draft")
            }
        }
    }

    fun savePlace(place: SavedPlaceEntity) =
        viewModelScope.launch {
            try {
                places.save(place)
                invalidate()
                mutableState.value = mutableState.value.copy(message = "Place saved")
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutableState.value = mutableState.value.copy(message = failure.message ?: "Could not save place")
            }
        }
}

/** Immutable snapshot used to reject stale quotes. */
data class RouteInput(
    val origin: SavedPlaceEntity,
    val destination: SavedPlaceEntity,
    val roundTrip: Boolean = false,
    val vehicleKey: String = "CAR",
    val server: String? = null,
    val vehicleRule: TripClassification? = null,
    val useWorkingHours: Boolean = false,
)
