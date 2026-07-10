package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_create_trip_subtitle
import com.mileway.core.ui.resources.travel_field_end_date
import com.mileway.core.ui.resources.travel_field_from_city
import com.mileway.core.ui.resources.travel_field_purpose
import com.mileway.core.ui.resources.travel_field_start_date
import com.mileway.core.ui.resources.travel_field_to_city
import com.mileway.core.ui.resources.travel_noun_trip_request
import com.mileway.core.ui.resources.travel_request_advance
import com.mileway.core.ui.resources.travel_section_advance
import com.mileway.core.ui.resources.travel_section_dates
import com.mileway.core.ui.resources.travel_section_trip
import com.mileway.core.ui.resources.travel_submit_trip
import com.mileway.core.ui.resources.travel_title_create_trip
import com.mileway.feature.travel.viewmodel.CreateTripAction
import com.mileway.feature.travel.viewmodel.CreateTripViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** TR.2: Create Trip request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateTripScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateTripViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = stringResource(Res.string.travel_noun_trip_request), onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = stringResource(Res.string.travel_title_create_trip),
        subtitle = stringResource(Res.string.travel_create_trip_subtitle),
        titleIcon = Icons.Filled.CardTravel,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateTripAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = stringResource(Res.string.travel_submit_trip),
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = stringResource(Res.string.travel_section_trip), leadingIcon = null) {
                TravelField(stringResource(Res.string.travel_field_purpose), ui.purpose) { viewModel.onAction(CreateTripAction.SetPurpose(it)) }
                TravelField(stringResource(Res.string.travel_field_from_city), ui.fromCity) { viewModel.onAction(CreateTripAction.SetFromCity(it)) }
                TravelField(stringResource(Res.string.travel_field_to_city), ui.toCity) { viewModel.onAction(CreateTripAction.SetToCity(it)) }
            }
            SectionCard(title = stringResource(Res.string.travel_section_dates), leadingIcon = null) {
                TravelField(stringResource(Res.string.travel_field_start_date), ui.startDate) { viewModel.onAction(CreateTripAction.SetStartDate(it)) }
                TravelField(stringResource(Res.string.travel_field_end_date), ui.endDate) { viewModel.onAction(CreateTripAction.SetEndDate(it)) }
            }
            SectionCard(title = stringResource(Res.string.travel_section_advance), leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(Res.string.travel_request_advance), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = ui.advanceRequired,
                        onCheckedChange = { viewModel.onAction(CreateTripAction.SetAdvanceRequired(it)) },
                    )
                }
            }
        }
    }
}
