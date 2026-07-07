package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
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
import com.mileway.core.ui.resources.travel_create_visa_subtitle
import com.mileway.feature.travel.viewmodel.CreateVisaAction
import com.mileway.feature.travel.viewmodel.CreateVisaViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val VISA_TYPES = listOf("Business", "Tourist", "Transit")

/** TR.7: Visa request, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateVisaScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateVisaViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = "Visa request", onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = "Visa Request",
        subtitle = stringResource(Res.string.travel_create_visa_subtitle),
        titleIcon = Icons.Filled.Description,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateVisaAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Submit visa request",
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Destination", leadingIcon = null) {
                TravelField("Country *", ui.country) { viewModel.onAction(CreateVisaAction.SetCountry(it)) }
                TravelField("Travel date *", ui.travelDate) { viewModel.onAction(CreateVisaAction.SetTravelDate(it)) }
            }
            SectionCard(title = "Traveller", leadingIcon = null) {
                TravelField("Passport number *", ui.passportNumber) { viewModel.onAction(CreateVisaAction.SetPassportNumber(it)) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VISA_TYPES.forEach { type ->
                        FilterChip(
                            selected = ui.visaType == type,
                            onClick = { viewModel.onAction(CreateVisaAction.SetVisaType(type)) },
                            label = { Text(type) },
                        )
                    }
                }
            }
        }
    }
}
