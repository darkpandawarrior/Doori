@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.mileway.feature.tracking.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.data.domain.policy.TripClassification
import com.mileway.core.data.model.db.SavedPlaceEntity
import com.mileway.core.network.routing.RouteEstimate
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.theme.MilewayTheme
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Reachable from Saved Tracks on every platform; routing needs an explicit user action. */
@Composable
fun PointToPointScreen(
    onBack: () -> Unit,
    viewModel: PointToPointViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val places by viewModel.places.savedPlaces.collectAsState(emptyList())
    val favourites by viewModel.places.favouriteRoutes.collectAsState(emptyList())
    var origin by remember { mutableStateOf<SavedPlaceEntity?>(null) }
    var destination by remember { mutableStateOf<SavedPlaceEntity?>(null) }
    var roundTrip by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf("") }
    var vehicle by remember { mutableStateOf("CAR") }
    var vehicleRule by remember { mutableStateOf<TripClassification?>(null) }
    var hours by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf("") }
    var override by remember { mutableStateOf<TripClassification?>(null) }
    var favourite by remember { mutableStateOf(false) }
    val input = origin?.let { start -> destination?.let { end -> RouteInput(start, end, roundTrip, vehicle, server, vehicleRule, hours) } }
    FormSubmissionScaffold(
        title = "Point-to-point route",
        onBack = onBack,
        subtitle = "Route estimates are saved as drafts for review",
        onSubmit = { input?.let { viewModel.save(it, manual.toDoubleOrNull(), override, favourite) } },
        submitLabel = "Save route draft",
        canSubmit = input != null && state.estimate != null && state.savedRouteId == null,
        isSubmitting = state.busy,
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard(title = "Endpoints") {
                EndpointEntry("Origin", places, origin) {
                    origin = it
                    viewModel.invalidate()
                }
                EndpointEntry("Destination", places, destination) {
                    destination = it
                    viewModel.invalidate()
                }
                Text("Home is protected. Its coordinates stay on this device.")
                RouteCheck("Round trip", roundTrip, !state.busy) {
                    roundTrip = it
                    viewModel.invalidate()
                }
                if (state.returnAlreadyRecorded) Text("Return leg already recorded; no extra return distance will be added")
            }
            SectionCard(title = "Distance") {
                OutlinedTextField(server, {
                    server = it
                    viewModel.invalidate()
                }, label = { Text("Self-hosted OSRM server (optional)") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { input?.let(viewModel::estimate) }, enabled = input != null && !state.busy) { Text("Estimate route") }
                when (val estimate = state.estimate) {
                    is RouteEstimate.Routed -> Text("OSRM routed distance: ${estimate.distanceKm} km")
                    is RouteEstimate.ManualRequired -> {
                        Text(estimate.reason)
                        OutlinedTextField(manual, { manual = it }, label = { Text("Total distance km (approximate)") }, modifier = Modifier.fillMaxWidth())
                    }
                    null -> Text("Choose endpoints and estimate, or use manual entry when routing is unavailable")
                }
            }
            SectionCard(title = "Classification") {
                OutlinedTextField(vehicle, {
                    vehicle = it
                    viewModel.invalidate()
                }, label = { Text("Vehicle key") })
                Text("Vehicle rule")
                ClassificationChoices(vehicleRule) {
                    vehicleRule = it
                    viewModel.invalidate()
                }
                RouteCheck("Working hours rule (09:00-18:00)", hours, !state.busy) {
                    hours = it
                    viewModel.invalidate()
                }
                Text("Automatic order: vehicle, place, hours, last trip")
                state.decision?.let { Text("Suggested: ${it.classification.name.lowercase()} (${it.source.name.lowercase()})") }
                Text("Your override")
                ClassificationChoices(override) { override = it }
                RouteCheck("Save as favourite route", favourite, !state.busy) { favourite = it }
            }
            SectionCard(title = "Saved places") {
                SavedPlaceEditor(places, viewModel::savePlace)
            }
            if (favourites.isNotEmpty()) {
                SectionCard(title = "Favourite routes") {
                    favourites.forEach { route ->
                        TextButton(onClick = {
                            manual = route.distanceKm.toString()
                            override = TripClassification.entries.firstOrNull { it.name.equals(route.purpose, ignoreCase = true) }
                        }) { Text("Use favourite: ${route.name} (${route.distanceKm} km)") }
                    }
                    Text("Favourites keep distance and purpose; Home endpoints are never shared")
                }
            }
            state.message?.let { Text(it) }
        }
    }
}

@Composable
private fun EndpointEntry(
    label: String,
    places: List<SavedPlaceEntity>,
    selected: SavedPlaceEntity?,
    onSelect: (SavedPlaceEntity?) -> Unit,
) {
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    Text(label)
    places.forEach { place ->
        FilterChip(selected = selected?.id == place.id, onClick = { onSelect(place) }, label = {
            Text(
                if (place.type ==
                    "HOME"
                ) {
                    "Home (protected)"
                } else {
                    place.label
                },
            )
        })
    }

    fun update() {
        val latitude = lat.toDoubleOrNull()
        val longitude = lng.toDoubleOrNull()
        onSelect(
            if (latitude != null &&
                latitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude != null &&
                longitude.isFinite() &&
                longitude in -180.0..180.0
            ) {
                SavedPlaceEntity(label, "OTHER", label, "", latitude, longitude, 0L)
            } else {
                null
            },
        )
    }
    OutlinedTextField(lat, {
        lat = it
        update()
    }, label = { Text("$label latitude") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(lng, {
        lng = it
        update()
    }, label = { Text("$label longitude") }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun ClassificationChoices(
    selected: TripClassification?,
    onSelect: (TripClassification?) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == null, { onSelect(null) }, label = { Text("Automatic") })
        TripClassification.entries.forEach { value ->
            FilterChip(selected == value, { onSelect(value) }, label = { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }) })
        }
    }
}

@Composable
private fun RouteCheck(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row {
        Checkbox(checked, onChange, enabled = enabled)
        Text(label, Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun SavedPlaceEditor(
    places: List<SavedPlaceEntity>,
    onSave: (SavedPlaceEntity) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("OTHER") }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("HOME", "WORK", "OTHER").forEach { value -> FilterChip(type == value, { type = value }, label = { Text(value.lowercase()) }) }
    }
    OutlinedTextField(label, { label = it }, label = { Text("Place label") })
    OutlinedTextField(address, { address = it }, label = { Text("Address (device only for Home)") })
    OutlinedTextField(lat, { lat = it }, label = { Text("Place latitude (optional)") })
    OutlinedTextField(lng, { lng = it }, label = { Text("Place longitude (optional)") })
    val validCoordinates =
        (lat.isBlank() && lng.isBlank()) ||
            (
                lat.toDoubleOrNull()?.let { it.isFinite() && it in -90.0..90.0 } == true &&
                    lng.toDoubleOrNull()?.let { it.isFinite() && it in -180.0..180.0 } == true
            )
    TextButton(onClick = {
        val id = if (type == "HOME") places.firstOrNull { it.type == "HOME" }?.id ?: Uuid.random().toString() else Uuid.random().toString()
        onSave(SavedPlaceEntity(id, type, label, address, lat.toDoubleOrNull(), lng.toDoubleOrNull(), Clock.System.now().toEpochMilliseconds()))
    }, enabled = label.isNotBlank() && validCoordinates) { Text("Save place") }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun RouteClassificationPreview() {
    MilewayTheme { SectionCard(title = "Classification") { ClassificationChoices(null) {} } }
}
