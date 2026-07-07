package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_create_flight_subtitle
import com.mileway.feature.travel.viewmodel.CreateFlightAction
import com.mileway.feature.travel.viewmodel.CreateFlightViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CABIN_CLASSES = listOf("Economy", "Premium", "Business")

/** TR.3: Add Flight booking request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateFlightScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateFlightViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = "Flight booking", onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = "Add Flight",
        subtitle = stringResource(Res.string.travel_create_flight_subtitle),
        titleIcon = Icons.Filled.Flight,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateFlightAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Request flight",
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Route", leadingIcon = null) {
                TravelField("From city *", ui.fromCity) { viewModel.onAction(CreateFlightAction.SetFromCity(it)) }
                TravelField("To city *", ui.toCity) { viewModel.onAction(CreateFlightAction.SetToCity(it)) }
                TravelField("Travel date *", ui.travelDate) { viewModel.onAction(CreateFlightAction.SetTravelDate(it)) }
            }
            SectionCard(title = "Preferences", leadingIcon = null) {
                TravelField("Preferred airline", ui.preferredAirline) { viewModel.onAction(CreateFlightAction.SetPreferredAirline(it)) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CABIN_CLASSES.forEach { cabin ->
                        FilterChip(
                            selected = ui.cabinClass == cabin,
                            onClick = { viewModel.onAction(CreateFlightAction.SetCabinClass(cabin)) },
                            label = { Text(cabin) },
                        )
                    }
                }
            }
        }
    }
}
