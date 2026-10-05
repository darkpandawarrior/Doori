package com.mileway.feature.travel.request

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mileway.core.data.domain.claim.ApprovalAction
import com.mileway.core.data.domain.claim.ReportLifecycleEvent
import com.mileway.core.data.domain.claim.ReportLifecycleState
import com.mileway.core.data.domain.claim.formatMinorCurrency
import com.mileway.core.data.domain.travel.TravelEstimate
import com.mileway.core.ui.previews.PreviewLightDark
import com.mileway.core.ui.theme.MilewayTheme
import org.koin.compose.viewmodel.koinViewModel

/** Pre-trip estimate and local review, reachable from the travel hub on every platform. */
@Composable
fun TravelRequestScreen(
    onBack: () -> Unit,
    viewModel: TravelRequestViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TravelRequestContent(state, state.selected?.let(viewModel::nextRole), onBack, viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TravelRequestContent(
    state: TravelRequestUiState,
    nextRole: String?,
    onBack: () -> Unit,
    onAction: (TravelRequestAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pre-trip authorization") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } })
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Travel requests are kept for this session only", style = MaterialTheme.typography.bodyMedium)
            Text("Authorization estimates do not create claims or payouts.", style = MaterialTheme.typography.bodySmall)

            state.message?.let { message -> Text(message, color = MaterialTheme.colorScheme.error) }
            val selected = state.selected
            if (selected != null) {
                Text(selected.purpose, style = MaterialTheme.typography.titleLarge)
                Text("Travel date: ${selected.travelDate}")
                Text("Status: ${selected.state}")
                EstimateCard(selected.estimate)

                selected.approvalChain.steps.forEach { step ->
                    Text("${step.role}: ${step.action} by ${step.actedBy}. ${step.comment.orEmpty()}")
                }
                if (selected.state == ReportLifecycleState.SUBMITTED) {
                    Text("Local review simulation", style = MaterialTheme.typography.titleMedium)
                    Text("Reviewer identities are entered here. This is not server authorization.")
                    Text("Next review: $nextRole")
                    OutlinedTextField(state.reviewer, {
                        onAction(TravelRequestAction.Reviewer(it))
                    }, label = { Text("Reviewer account") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(state.comment, {
                        onAction(TravelRequestAction.Comment(it))
                    }, label = { Text("Review comment") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth())

                    ApprovalAction.entries.forEach { action ->

                        Button(
                            onClick = { onAction(TravelRequestAction.Review(action)) },
                            enabled = !state.busy && state.reviewer.isNotBlank() && state.comment.isNotBlank(),
                        ) {
                            Text(
                                when (action) {
                                    ApprovalAction.APPROVE -> "Approve $nextRole step"
                                    ApprovalAction.SEND_BACK -> "Send request back"
                                    ApprovalAction.REJECT -> "Reject request"
                                },
                            )
                        }
                    }
                    if (selected.approvalChain.steps.isEmpty()) {
                        TextButton(
                            onClick = { onAction(TravelRequestAction.Transition(ReportLifecycleEvent.RECALL)) },
                            enabled = !state.busy,
                        ) { Text("Recall request") }
                    }
                }
                if (selected.state == ReportLifecycleState.SENT_BACK || selected.state == ReportLifecycleState.RECALLED) {
                    Button(onClick = {
                        onAction(
                            TravelRequestAction.Transition(
                                if (selected.state ==
                                    ReportLifecycleState.SENT_BACK
                                ) {
                                    ReportLifecycleEvent.RESUBMIT
                                } else {
                                    ReportLifecycleEvent.SUBMIT
                                },
                            ),
                        )
                    }, enabled = !state.busy) { Text("Resubmit unchanged estimate") }
                }
                TextButton(onClick = { onAction(TravelRequestAction.New) }, enabled = !state.busy) { Text("New request") }
            } else {
                state.requests.forEach { request ->
                    TextButton(
                        onClick = { onAction(TravelRequestAction.Open(request)) },
                        enabled = !state.busy,
                    ) { Text("Open ${request.purpose}: ${request.state}") }
                }
                state.estimate?.let { estimate -> EstimateCard(estimate) }

                Button(onClick = { onAction(TravelRequestAction.Submit) }, enabled = state.canSubmit) { Text("Submit for authorization") }
                if (state.owner == null) Text("Choose an active account to submit a request")

                RequestField("Business purpose", TravelRequestField.PURPOSE, state, onAction)
                RequestField("Travel date YYYY-MM-DD", TravelRequestField.DATE, state, onAction)

                Toggle("Use published HMRC mirror (GBP); otherwise IRS (USD)", state.hmrc, !state.busy) { onAction(TravelRequestAction.Hmrc(it)) }
                Text("Published mirror only. Employer rate edits are not connected.", style = MaterialTheme.typography.bodySmall)

                RequestField("Annual business distance before trip (miles)", TravelRequestField.ANNUAL_DISTANCE, state, onAction)
                RequestField("Origin latitude", TravelRequestField.ORIGIN_LAT, state, onAction)
                RequestField("Origin longitude", TravelRequestField.ORIGIN_LON, state, onAction)
                Toggle("Origin is Home (protected)", state.originHome, !state.busy) { onAction(TravelRequestAction.Home(true, it)) }
                RequestField("Destination latitude", TravelRequestField.DESTINATION_LAT, state, onAction)
                RequestField("Destination longitude", TravelRequestField.DESTINATION_LON, state, onAction)
                Toggle("Destination is Home (protected)", state.destinationHome, !state.busy) { onAction(TravelRequestAction.Home(false, it)) }
                Toggle("Round trip", state.roundTrip, !state.busy) { onAction(TravelRequestAction.RoundTrip(it)) }
                RequestField("Self-hosted OSRM server (optional)", TravelRequestField.SERVER, state, onAction)
                Button(onClick = { onAction(TravelRequestAction.Estimate) }, enabled = !state.busy) { Text("Estimate route") }
                state.manualReason?.let { reason ->

                    Text(reason)
                    Text("Enter total distance including the return leg for a round trip.")
                    RequestField("Total distance km (approximate)", TravelRequestField.MANUAL_DISTANCE, state, onAction)
                    Button(onClick = { onAction(TravelRequestAction.Manual) }, enabled = !state.busy) { Text("Estimate manual distance") }
                }
            }
            Text(if (state.busy) "Working..." else "", modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun EstimateCard(estimate: TravelEstimate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Estimated authorization: ${formatMinorCurrency(estimate.amountMinor, estimate.currency)}", style = MaterialTheme.typography.titleMedium)
            Text("${estimate.distanceKm} km (${if (estimate.approximate) "approximate" else "OSRM route-ahead"})")
            Text("Rate effective: ${estimate.rateEffectiveFrom}")
            Text("Annual distance before trip: ${estimate.annualDistanceBeforeUnits} ${estimate.distanceUnit}")
            Text(estimate.authority, style = MaterialTheme.typography.bodySmall)
            Text(estimate.sourceUrl, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RequestField(
    label: String,
    key: TravelRequestField,
    state: TravelRequestUiState,
    onAction: (TravelRequestAction) -> Unit,
) {
    OutlinedTextField(state.field(key), {
        onAction(TravelRequestAction.Edit(key, it))
    }, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !state.busy, singleLine = true)
}

@Composable
private fun Toggle(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked, onChange, enabled = enabled)
    }
}

@PreviewLightDark
@Composable
private fun TravelRequestPreview() {
    MilewayTheme { TravelRequestContent(TravelRequestUiState(), null, {}, {}) }
}
