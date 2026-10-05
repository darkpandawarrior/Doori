package com.mileway.feature.travel.request

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.policy.AnnualMileageDistance
import com.mileway.core.data.domain.policy.MileageRateMirror
import com.mileway.core.data.domain.policy.rates.HmrcMileageRates
import com.mileway.core.data.domain.policy.rates.IrsMileageRates
import com.mileway.core.data.domain.travel.TravelEstimate
import com.mileway.core.data.domain.travel.TravelRequest
import com.mileway.core.data.domain.travel.estimateTravel
import com.mileway.core.data.domain.travel.travelDateMillis
import com.mileway.core.network.routing.OsrmClient
import com.mileway.core.network.routing.OsrmConfiguration
import com.mileway.core.network.routing.RouteEstimate
import com.mileway.core.network.routing.RoutePoint
import com.siddharth.kmp.mvi.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

enum class TravelRequestField { PURPOSE, DATE, ORIGIN_LAT, ORIGIN_LON, DESTINATION_LAT, DESTINATION_LON, SERVER, ANNUAL_DISTANCE, MANUAL_DISTANCE }

data class TravelRequestUiState(
    val fields: Map<TravelRequestField, String> = emptyMap(),
    val owner: String? = null,
    val hmrc: Boolean = false,
    val originHome: Boolean = false,
    val destinationHome: Boolean = false,
    val roundTrip: Boolean = false,
    val busy: Boolean = false,
    val estimate: TravelEstimate? = null,
    val manualReason: String? = null,
    val message: String? = null,
    val requests: List<TravelRequest> = emptyList(),
    val selected: TravelRequest? = null,
    val reviewer: String = "",
    val comment: String = "",
) {
    fun field(key: TravelRequestField): String = fields[key].orEmpty()
    val canSubmit: Boolean get() = !busy && owner != null && estimate != null && field(TravelRequestField.PURPOSE).isNotBlank() && selected == null
}

sealed interface TravelRequestAction {
    data class Edit(val field: TravelRequestField, val value: String) : TravelRequestAction
    data class Home(val origin: Boolean, val enabled: Boolean) : TravelRequestAction
    data class Hmrc(val enabled: Boolean) : TravelRequestAction
    data class RoundTrip(val enabled: Boolean) : TravelRequestAction
    data class Reviewer(val value: String) : TravelRequestAction
    data class Comment(val value: String) : TravelRequestAction
    data class Review(val action: ApprovalAction) : TravelRequestAction
    data class Transition(val event: ReportLifecycleEvent) : TravelRequestAction
    data class Open(val request: TravelRequest) : TravelRequestAction
    data object Estimate : TravelRequestAction
    data object Manual : TravelRequestAction
    data object Submit : TravelRequestAction
    data object New : TravelRequestAction
}

/** Cancels stale quotes on edits and persona changes. Routing happens only after an explicit action. */
class TravelRequestViewModel(
    private val store: TravelRequestStore,
    accounts: Flow<String?>,
    private val route: suspend (RoutePoint, RoutePoint, Boolean, String?) -> RouteEstimate = { a, b, round, server ->
        OsrmClient(OsrmConfiguration(server)).route(a, b, round)
    },
    private val mirrors: Map<Boolean, MileageRateMirror> = mapOf(false to IrsMileageRates.mirror, true to HmrcMileageRates.mirror),
) : BaseViewModel<TravelRequestUiState, Unit, TravelRequestAction>(TravelRequestUiState()) {
    private var work: Job? = null
    private var revision = 0L

    init {
        viewModelScope.launch {
            combine(accounts, store.requests) { account, requests ->
                val owner = account?.takeIf { it.isNotBlank() }
                owner to requests.filter { it.employeeId == owner }
            }
                .collect { (owner, requests) ->
                    if (owner != currentState.owner) {
                        revision++
                        work?.cancel()
                        setState { TravelRequestUiState(owner = owner?.takeIf { it.isNotBlank() }) }
                    }
                    setState { copy(requests = requests, selected = selected?.let { old -> requests.find { it.id == old.id } }) }
                }
        }
    }

    override fun onAction(action: TravelRequestAction) {
        when (action) {
            is TravelRequestAction.Edit -> invalidate { copy(fields = fields + (action.field to action.value)) }
            is TravelRequestAction.Home -> invalidate { if (action.origin) copy(originHome = action.enabled) else copy(destinationHome = action.enabled) }
            is TravelRequestAction.Hmrc -> invalidate { copy(hmrc = action.enabled) }
            is TravelRequestAction.RoundTrip -> invalidate { copy(roundTrip = action.enabled) }
            is TravelRequestAction.Reviewer -> setState { copy(reviewer = action.value) }
            is TravelRequestAction.Comment -> setState { copy(comment = action.value) }
            is TravelRequestAction.Open -> {
                if (!currentState.busy) {
                    setState { copy(selected = requests.find { it.id == action.request.id }, reviewer = "", comment = "", message = null) }
                }
            }
            is TravelRequestAction.Review -> review(action.action)
            is TravelRequestAction.Transition -> transition(action.event)
            TravelRequestAction.Estimate -> estimate()
            TravelRequestAction.Manual -> manual()
            TravelRequestAction.Submit -> submit()
            TravelRequestAction.New -> invalidate { TravelRequestUiState(owner = owner, requests = requests) }
        }
    }

    fun nextRole(request: TravelRequest): String = store.nextRole(request)

    private fun invalidate(edit: TravelRequestUiState.() -> TravelRequestUiState) {
        if (currentState.busy && currentState.selected != null) return
        revision++
        work?.cancel()
        setState { edit().copy(estimate = null, busy = false, message = null) }
    }

    private fun quote(
        input: TravelRequestUiState,
        distance: Double,
        approximate: Boolean,
    ): TravelEstimate {
        val mirror = mirrors.getValue(input.hmrc)
        val at = travelDateMillis(input.field(TravelRequestField.DATE))
        val before = input.field(TravelRequestField.ANNUAL_DISTANCE).ifBlank { "0" }.toLongOrNull()
        require(before != null && before >= 0) { "Enter nonnegative whole annual distance units" }
        return estimateTravel(distance, approximate, input.field(TravelRequestField.DATE), mirror, AnnualMileageDistance(mirror.annualPeriod.startFor(at), before))
    }

    private fun estimate() {
        if (currentState.busy) return
        val input = currentState
        execute {
            travelDateMillis(input.field(TravelRequestField.DATE))
            val a = point(input, true)
            val b = point(input, false)
            val result =
                if (a == null || b == null) {
                    RouteEstimate.ManualRequired("Coordinates are missing; enter total distance manually (approximate)")
                } else {
                    route(a, b, input.roundTrip, input.field(TravelRequestField.SERVER).takeIf { it.isNotBlank() })
                }
            currentCoroutineContext().ensureActive()
            when (result) {
                is RouteEstimate.Routed -> setState { copy(estimate = quote(input, result.distanceKm, false), manualReason = null) }
                is RouteEstimate.ManualRequired -> setState { copy(estimate = null, manualReason = result.reason) }
            }
        }
    }

    private fun point(input: TravelRequestUiState, origin: Boolean): RoutePoint? {
        val latKey = if (origin) TravelRequestField.ORIGIN_LAT else TravelRequestField.DESTINATION_LAT
        val lonKey = if (origin) TravelRequestField.ORIGIN_LON else TravelRequestField.DESTINATION_LON
        val lat = input.field(latKey)
        val lon = input.field(lonKey)
        if (lat.isBlank() && lon.isBlank()) return null
        val latitude = lat.toDoubleOrNull()
        val longitude = lon.toDoubleOrNull()
        require(latitude != null && longitude != null) { "Enter valid latitude and longitude" }
        return RoutePoint(latitude, longitude, if (origin) input.originHome else input.destinationHome)
    }

    private fun manual() {
        if (currentState.manualReason == null) return
        val input = currentState
        execute {
            val distance = input.field(TravelRequestField.MANUAL_DISTANCE).toDoubleOrNull()
            require(distance != null) { "Enter total distance in kilometres" }
            setState { copy(estimate = quote(input, distance, true)) }
        }
    }

    private fun submit() {
        val input = currentState
        if (!input.canSubmit) return
        execute {
            val request =
                TravelRequest(
                    Uuid.random().toString(),
                    requireNotNull(input.owner),
                    input.field(TravelRequestField.PURPOSE).trim(),
                    input.field(TravelRequestField.DATE),
                    requireNotNull(input.estimate),
                )
            val saved = store.submit(request)
            setState { copy(selected = saved) }
        }
    }

    private fun review(action: ApprovalAction) {
        val input = currentState
        val request = input.selected ?: return
        execute {
            val saved = store.act(request.id, request.recordVersion, input.reviewer, store.nextRole(request), action, input.comment)
            setState { copy(selected = saved, reviewer = "", comment = "") }
        }
    }

    private fun transition(event: ReportLifecycleEvent) {
        val request = currentState.selected ?: return
        execute {
            val saved = store.transition(request.id, request.recordVersion, event)
            setState { copy(selected = saved) }
        }
    }

    private fun execute(block: suspend () -> Unit) {
        if (currentState.busy) return
        val startedRevision = revision
        setState { copy(busy = true, message = null) }
        work =
            viewModelScope.launch {
                try {
                    block()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (revision == startedRevision) {
                        setState { copy(message = failure.message ?: "Travel request failed", estimate = null) }
                    }
                } finally {
                    if (revision == startedRevision) setState { copy(busy = false) }
                }
            }
    }
}
