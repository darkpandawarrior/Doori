package com.mileway.feature.travel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.travel_add_leg
import com.mileway.core.ui.resources.travel_create_mjp_subtitle
import com.mileway.core.ui.resources.travel_remove_leg
import com.mileway.core.ui.resources.travel_route
import com.mileway.core.ui.theme.DesignTokens
import com.mileway.feature.travel.viewmodel.CreateMjpAction
import com.mileway.feature.travel.viewmodel.CreateMjpViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** TR.6: Multi-city Journey Plan (MJP) create flow with add/remove legs, on the shared FormSubmissionScaffold. */
@Composable
fun CreateMjpScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateMjpViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    HandleTravelCreateEffects(viewModel.effect, noun = "Journey plan", onSubmitted = onSubmitted)

    FormSubmissionScaffold(
        title = "Journey Plan",
        subtitle = stringResource(Res.string.travel_create_mjp_subtitle),
        titleIcon = Icons.Filled.Map,
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateMjpAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Submit plan",
        submitIcon = Icons.Filled.Check,
    ) { contentPadding ->
        TravelFormBody(contentPadding) {
            SectionCard(title = "Plan", leadingIcon = null) {
                TravelField("Purpose *", ui.purpose) { viewModel.onAction(CreateMjpAction.SetPurpose(it)) }
            }
            ui.legs.forEachIndexed { index, leg ->
                SectionCard(title = "Leg ${index + 1}", leadingIcon = null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(Res.string.travel_route), modifier = Modifier.padding(top = 12.dp))
                        if (ui.legs.size > 1) {
                            IconButton(onClick = { viewModel.onAction(CreateMjpAction.RemoveLeg(index)) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.travel_remove_leg))
                            }
                        }
                    }
                    TravelField("From city *", leg.fromCity) { viewModel.onAction(CreateMjpAction.SetLegFrom(index, it)) }
                    TravelField("To city *", leg.toCity) { viewModel.onAction(CreateMjpAction.SetLegTo(index, it)) }
                    TravelField("Travel date *", leg.travelDate) { viewModel.onAction(CreateMjpAction.SetLegDate(index, it)) }
                }
            }
            OutlinedButton(
                onClick = { viewModel.onAction(CreateMjpAction.AddLeg) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = DesignTokens.Shape.button,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(Res.string.travel_add_leg))
            }
        }
    }
}
