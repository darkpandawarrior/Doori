package com.mileway.feature.events.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mileway.core.ui.components.SectionCard
import com.mileway.core.ui.components.scaffold.FormSubmissionScaffold
import com.mileway.core.ui.resources.Res
import com.mileway.core.ui.resources.events_create_title
import com.mileway.core.ui.resources.events_field_capacity
import com.mileway.core.ui.resources.events_field_category
import com.mileway.core.ui.resources.events_field_date
import com.mileway.core.ui.resources.events_field_title
import com.mileway.core.ui.resources.events_field_venue
import com.mileway.core.ui.resources.events_publish_event
import com.mileway.core.ui.resources.events_section_event
import com.mileway.core.ui.resources.events_section_schedule_capacity
import com.mileway.core.ui.resources.events_toast_policy_violations
import com.mileway.core.ui.resources.events_toast_published
import com.mileway.core.ui.resources.events_toast_sent_approval
import com.mileway.core.ui.toast.ToastType
import com.mileway.core.ui.toast.Toasts
import com.mileway.feature.events.viewmodel.CreateEventAction
import com.mileway.feature.events.viewmodel.CreateEventEffect
import com.mileway.feature.events.viewmodel.CreateEventViewModel
import org.jetbrains.compose.resources.stringResource
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
    val toastPublished = stringResource(Res.string.events_toast_published)
    val toastSentApproval = stringResource(Res.string.events_toast_sent_approval)
    val toastPolicyViolations = stringResource(Res.string.events_toast_policy_violations)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreateEventEffect.Success -> {
                    Toasts.show(toastPublished, "Reference ${effect.id}", ToastType.Success)
                    onSubmitted(effect.id)
                }
                is CreateEventEffect.NeedsApproval -> {
                    Toasts.show(toastSentApproval, "${effect.id} is awaiting sign-off", ToastType.Info)
                    onSubmitted(effect.id)
                }
                is CreateEventEffect.Violation ->
                    Toasts.show(toastPolicyViolations, effect.messages.joinToString(" · "), ToastType.Warning)
            }
        }
    }

    FormSubmissionScaffold(
        title = stringResource(Res.string.events_create_title),
        onBack = onBack,
        onSubmit = { viewModel.onAction(CreateEventAction.Submit) },
        modifier = modifier,
        canSubmit = ui.canSubmit,
        isSubmitting = ui.isSubmitting,
        submitLabel = stringResource(Res.string.events_publish_event),
    ) { contentPadding ->
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(16.dp)) {
            SectionCard(title = stringResource(Res.string.events_section_event), leadingIcon = Icons.Default.Event) {
                Field(stringResource(Res.string.events_field_title), ui.title) { viewModel.onAction(CreateEventAction.SetTitle(it)) }
                Field(stringResource(Res.string.events_field_venue), ui.venue) { viewModel.onAction(CreateEventAction.SetVenue(it)) }
                Field(stringResource(Res.string.events_field_category), ui.category) { viewModel.onAction(CreateEventAction.SetCategory(it)) }
            }
            SectionCard(title = stringResource(Res.string.events_section_schedule_capacity), leadingIcon = Icons.Default.Schedule) {
                Field(stringResource(Res.string.events_field_date), ui.date) { viewModel.onAction(CreateEventAction.SetDate(it)) }
                Field(
                    stringResource(Res.string.events_field_capacity),
                    ui.capacityText,
                    KeyboardType.Number,
                ) { viewModel.onAction(CreateEventAction.SetCapacity(it)) }
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
