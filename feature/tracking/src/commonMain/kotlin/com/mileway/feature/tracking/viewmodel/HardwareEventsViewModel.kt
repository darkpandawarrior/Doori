@file:Suppress("ktlint:standard:max-line-length", "ktlint:standard:comment-wrapping")

package com.mileway.feature.tracking.viewmodel

import androidx.lifecycle.viewModelScope
import com.mileway.core.data.model.db.EventAudience
import com.mileway.core.data.model.db.EventType
import com.mileway.core.data.model.db.HardwareEvent
import com.mileway.feature.tracking.repository.HardwareEventRepository
import com.siddharth.kmp.mvi.BaseViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch

enum class ExportFormat { CSV, JSON }

data class EventStats(
    val totalCount: Int = 0,
    val audienceCounts: Map<EventAudience, Int> = emptyMap(),
    val timeRange: Pair<Long, Long>? = null,
)

data class HardwareEventsUiState(
    val allEvents: List<HardwareEvent> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedAudiences: Set<EventAudience> = setOf(EventAudience.USER),
    val filteredEvents: List<HardwareEvent> = emptyList(),
    val eventStats: EventStats = EventStats(),
)

private fun HardwareEventsUiState.withRecomputed(): HardwareEventsUiState {
    val filtered =
        allEvents.filter { event ->
            val matchesSearch =
                searchQuery.isBlank() ||
                    event.event.contains(searchQuery, ignoreCase = true) ||
                    event.activity?.contains(searchQuery, ignoreCase = true) == true
            val matchesAudience = selectedAudiences.isEmpty() || selectedAudiences.contains(event.audience)
            matchesSearch && matchesAudience
        }
    val stats =
        EventStats(
            totalCount = allEvents.size,
            audienceCounts = allEvents.groupBy { it.audience }.mapValues { it.value.size },
            timeRange =
                if (allEvents.isNotEmpty()) {
                    allEvents.minOf { it.time } to allEvents.maxOf { it.time }
                } else {
                    null
                },
        )
    return copy(filteredEvents = filtered, eventStats = stats)
}

sealed interface HardwareEventsAction {
    data class LoadByToken(val token: String) : HardwareEventsAction

    data class SetSearchQuery(val query: String) : HardwareEventsAction

    data class ToggleAudienceFilter(val audience: EventAudience) : HardwareEventsAction

    data object ClearFilters : HardwareEventsAction
}

sealed interface HardwareEventsEffect

class HardwareEventsViewModel(
    private val eventRepo: HardwareEventRepository,
) : BaseViewModel<HardwareEventsUiState, HardwareEventsEffect, HardwareEventsAction>(HardwareEventsUiState()) {
    companion object {
        private const val TAG = "HardwareEventsVM"

        private fun demoEvents(token: String): List<HardwareEvent> {
            val base = 1_781_654_400_000L
            val min = 60_000L
            return listOf(
                HardwareEvent(
                    id = 1,
                    token = token,
                    eventType = EventType.TRACKING_STARTED,
                    event = "Tracking started: FC Road, Pune",
                    time = base,
                    audience = EventAudience.USER,
                    battery = 87.0,
                    activity = "IN_VEHICLE",
                ),
                HardwareEvent(
                    id = 2,
                    token = token,
                    eventType = EventType.ODOMETER_START_CAPTURED,
                    event = "Odometer start captured: 045782 km",
                    time = base + min,
                    audience = EventAudience.USER,
                    battery = 86.5,
                ),
                HardwareEvent(id = 3, token = token, eventType = EventType.GPS_REGAINED, event = "GPS signal strong: accuracy 6 m", time = base + 2 * min, audience = EventAudience.USER, lat = 18.5204, lng = 73.8567, speed = 0f),
                HardwareEvent(
                    id = 4,
                    token = token,
                    eventType = EventType.BATTERY_OPTIMIZATION_ON,
                    event = "Battery optimisation ON: tracking may be interrupted",
                    time = base + 5 * min,
                    audience = EventAudience.USER,
                    battery = 85.0,
                ),
                HardwareEvent(
                    id = 5,
                    token = token,
                    eventType = EventType.MOCK_LOCATION,
                    event = "Mock location signal detected: flagged, not discarded",
                    time = base + 8 * min,
                    audience = EventAudience.DEBUG,
                    lat = 18.5280,
                    lng = 73.8600,
                ),
                HardwareEvent(
                    id = 6,
                    token = token,
                    eventType = EventType.GPS_LOST,
                    event = "GPS lost: tunnelling under Baner flyover",
                    time = base + 12 * min,
                    audience = EventAudience.USER,
                    lat = 18.5420,
                    lng = 73.8012,
                ),
                HardwareEvent(id = 7, token = token, eventType = EventType.GPS_REGAINED, event = "GPS regained: accuracy 9 m", time = base + 14 * min, audience = EventAudience.USER, lat = 18.5501, lng = 73.8150, speed = 28f),
                HardwareEvent(
                    id = 8,
                    token = token,
                    eventType = EventType.CHECK_IN,
                    event = "Geo check-in confirmed: Hinjewadi IT Park",
                    time = base + 28 * min,
                    audience = EventAudience.USER,
                    lat = 18.5904,
                    lng = 73.7394,
                ),
                HardwareEvent(
                    id = 9,
                    token = token,
                    eventType = EventType.ODOMETER_END_CAPTURED,
                    event = "Odometer end captured: 045794 km",
                    time = base + 44 * min,
                    audience = EventAudience.USER,
                    battery = 78.0,
                ),
                HardwareEvent(id = 10, token = token, eventType = EventType.TRACKING_STOPPED, event = "Tracking stopped: Viman Nagar, Pune", time = base + 45 * min, audience = EventAudience.USER, lat = 18.5679, lng = 73.9143, speed = 0f, battery = 77.5),
                HardwareEvent(
                    id = 11,
                    token = token,
                    eventType = EventType.JOURNEY_SUMMARY,
                    event = "Journey summary: 12.4 km · 45 min · quality score 90",
                    time = base + 45 * min + 2000,
                    audience = EventAudience.SUMMARY,
                    activity = "IN_VEHICLE",
                ),
            )
        }
    }

    override fun onAction(action: HardwareEventsAction) {
        when (action) {
            is HardwareEventsAction.LoadByToken -> loadEventsByToken(action.token)
            is HardwareEventsAction.SetSearchQuery ->
                setState { copy(searchQuery = action.query).withRecomputed() }
            is HardwareEventsAction.ToggleAudienceFilter -> {
                val updated =
                    currentState.selectedAudiences.toMutableSet().apply {
                        if (contains(action.audience)) remove(action.audience) else add(action.audience)
                    }
                setState { copy(selectedAudiences = updated).withRecomputed() }
            }
            HardwareEventsAction.ClearFilters ->
                setState {
                    copy(searchQuery = "", selectedAudiences = setOf(EventAudience.USER)).withRecomputed()
                }
        }
    }

    private fun loadEventsByToken(token: String) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            eventRepo.getEventsForRoute(token).fold(
                onSuccess = { loaded ->
                    setState {
                        copy(allEvents = loaded.ifEmpty { demoEvents(token) }, isLoading = false).withRecomputed()
                    }
                },
                onFailure = {
                    Napier.e(tag = TAG, message = "Error loading events: ${it.message}")
                    setState { copy(allEvents = demoEvents(token), isLoading = false).withRecomputed() }
                },
            )
        }
    }

    fun prepareExportPayload(format: ExportFormat): Triple<String, String, String>? {
        val list = currentState.filteredEvents
        if (list.isEmpty()) return null
        val (data, filename) = buildPayload(list, format)
        val mime = if (format == ExportFormat.CSV) "text/csv" else "application/json"
        return Triple(data, filename, mime)
    }

    private fun buildPayload(
        events: List<HardwareEvent>,
        format: ExportFormat,
    ): Pair<String, String> {
        return when (format) {
            ExportFormat.CSV -> {
                val sb = StringBuilder("id,token,eventType,event,time,audience,battery,activity\n")
                events.forEach { e ->
                    sb.append("${e.id},${e.token},${e.eventType},\"${e.event}\",${e.time},${e.audience},${e.battery ?: ""},${e.activity ?: ""}\n")
                }
                Pair(sb.toString(), "hardware_events.csv")
            }
            // ponytail: hardware-events export only offers CSV/JSON in its dialog; every other
            // ExportFormat (JSON, and any future one like EXCEL) falls through to the JSON payload,
            // matching prepareExportPayload's own "CSV else JSON" mime assumption above.
            else -> {
                val sb = StringBuilder("[")
                events.forEachIndexed { i, e ->
                    if (i > 0) sb.append(",")
                    val escaped = e.event.replace("\"", "\\\"")
                    sb.append(
                        "{\"id\":${e.id},\"token\":\"${e.token}\",\"eventType\":\"${e.eventType}\",\"event\":\"$escaped\",\"time\":${e.time},\"audience\":\"${e.audience}\"}",
                    )
                }
                sb.append("]")
                Pair(sb.toString(), "hardware_events.json")
            }
        }
    }
}
