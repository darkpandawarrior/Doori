package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.mileway.feature.travel.viewmodel.CreateTripAction
import com.mileway.feature.travel.viewmodel.CreateTripViewModel
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

    HandleTravelCreateEffects(viewModel.effect, noun = "Trip request", onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = "Create Trip",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateTripAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Submit trip",
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Trip", leadingIcon = null) {
                TravelField("Purpose *", ui.purpose) { viewModel.onAction(CreateTripAction.SetPurpose(it)) }
                TravelField("From city *", ui.fromCity) { viewModel.onAction(CreateTripAction.SetFromCity(it)) }
                TravelField("To city *", ui.toCity) { viewModel.onAction(CreateTripAction.SetToCity(it)) }
            }
            SectionCard(title = "Dates", leadingIcon = null) {
                TravelField("Start date", ui.startDate) { viewModel.onAction(CreateTripAction.SetStartDate(it)) }
                TravelField("End date", ui.endDate) { viewModel.onAction(CreateTripAction.SetEndDate(it)) }
            }
            SectionCard(title = "Advance", leadingIcon = null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Request travel advance", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = ui.advanceRequired,
                        onCheckedChange = { viewModel.onAction(CreateTripAction.SetAdvanceRequired(it)) },
                    )
                }
            }
        }
    }
}
