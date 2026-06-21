package com.miletracker.feature.events.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.miletracker.core.ui.components.SectionCard
import com.miletracker.core.ui.components.scaffold.FormSubmissionScaffold
import com.miletracker.core.ui.toast.ToastType
import com.miletracker.core.ui.toast.Toasts
import com.miletracker.feature.events.viewmodel.CreateEventAction
import com.miletracker.feature.events.viewmodel.CreateEventEffect
import com.miletracker.feature.events.viewmodel.CreateEventViewModel
import org.koin.compose.viewmodel.koinViewModel

/** EV: Create Event, built on the shared F0.1 FormSubmissionScaffold + SectionCards. */
@Composable
fun CreateEventScreen(
    onBack: () -> Unit,
    onSubmitted: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateEventViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateEventEffect.Success -> {
                    Toasts.show("Event published", "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateEventEffect.NeedsApproval -> {
                    Toasts.show("Sent for approval", "${effect.id} is awaiting sign-off", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateEventEffect.Violation ->
                    Toasts.show("Policy violations", effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = "Create Event",
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateEventAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = "Publish event",
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = "Event", leadingIcon = null) {
                Field("Title *", ui.title) { viewModel.onAction(CreateEventAction.SetTitle(it)) }
                Field("Venue *", ui.venue) { viewModel.onAction(CreateEventAction.SetVenue(it)) }
                Field("Category", ui.category) { viewModel.onAction(CreateEventAction.SetCategory(it)) }
            }
            SectionCard(title = "Schedule & capacity", leadingIcon = null) {
                Field("Date", ui.date) { viewModel.onAction(CreateEventAction.SetDate(it)) }
                Field("Capacity *", ui.capacityText, KeyboardType.Number) { viewModel.onAction(CreateEventAction.SetCapacity(it)) }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}
