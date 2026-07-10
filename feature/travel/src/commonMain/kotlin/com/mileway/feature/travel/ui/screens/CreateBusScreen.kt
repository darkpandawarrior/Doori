package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBus
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
import com.mileway.core.ui.resources.travel_create_bus_subtitle
import com.mileway.core.ui.resources.travel_field_from_city
import com.mileway.core.ui.resources.travel_field_operator
import com.mileway.core.ui.resources.travel_field_to_city
import com.mileway.core.ui.resources.travel_field_travel_date
import com.mileway.core.ui.resources.travel_noun_bus_booking
import com.mileway.core.ui.resources.travel_route
import com.mileway.core.ui.resources.travel_section_preferences
import com.mileway.core.ui.resources.travel_submit_bus
import com.mileway.core.ui.resources.travel_title_add_bus
import com.mileway.feature.travel.viewmodel.CreateBusAction
import com.mileway.feature.travel.viewmodel.CreateBusViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SEAT_PREFERENCES = listOf("Seater", "Sleeper", "Semi-sleeper")

/** TR.4: Add Bus booking request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateBusScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateBusViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = stringResource(Res.string.travel_noun_bus_booking), onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = stringResource(Res.string.travel_title_add_bus),
        subtitle = stringResource(Res.string.travel_create_bus_subtitle),
        titleIcon = Icons.Filled.DirectionsBus,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateBusAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = stringResource(Res.string.travel_submit_bus),
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = stringResource(Res.string.travel_route), leadingIcon = null) {
                TravelField(stringResource(Res.string.travel_field_from_city), ui.fromCity) { viewModel.onAction(CreateBusAction.SetFromCity(it)) }
                TravelField(stringResource(Res.string.travel_field_to_city), ui.toCity) { viewModel.onAction(CreateBusAction.SetToCity(it)) }
                TravelField(stringResource(Res.string.travel_field_travel_date), ui.travelDate) { viewModel.onAction(CreateBusAction.SetTravelDate(it)) }
            }
            SectionCard(title = stringResource(Res.string.travel_section_preferences), leadingIcon = null) {
                TravelField(stringResource(Res.string.travel_field_operator), ui.operator) { viewModel.onAction(CreateBusAction.SetOperator(it)) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SEAT_PREFERENCES.forEach { seat ->
                        FilterChip(
                            selected = ui.seatPreference == seat,
                            onClick = { viewModel.onAction(CreateBusAction.SetSeatPreference(seat)) },
                            label = { Text(travelChipLabel(seat)) },
                        )
                    }
                }
            }
        }
    }
}
