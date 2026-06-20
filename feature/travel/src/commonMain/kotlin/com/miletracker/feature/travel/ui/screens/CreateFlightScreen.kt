package com.miletracker.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold
import com.miletracker.feature.travel.viewmodel.CreateFlightAction
import com.miletracker.feature.travel.viewmodel.CreateFlightViewModel
import org.koin.compose.viewmodel.koinViewModel

private val CABIN_CLASSES = listOf("Economy", "Premium", "Business")

/** TR.3 — Add Flight booking request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
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
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateFlightAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Request flight",
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
